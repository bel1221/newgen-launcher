package net.newgen.client.gui;

import java.nio.ByteBuffer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.newgen.client.NewgenClient;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.lwjgl.system.MemoryUtil;

/**
 * The ambient background of the menus outside a world: NewGen gameplay clips (assets/newgen_client/backgrounds,
 * 1920x1080, 24 fps, H.264 baseline, CRF 18, each as a top and a bottom half) played one after another with a 1 s
 * crossfade, cover-fitted to the screen.
 * <p>
 * Each half is decoded by JCodec on its own low-priority daemon thread, so a 1080p frame takes two threads about
 * 25 ms each (about 1 core in all on an i5-12400F, 2 during the 1 s crossfade; measured, no late frames once warm).
 * The render thread only copies the current frame into a texture. A decoder stops by itself when no screen
 * has drawn the background for 500 ms, so nothing runs in a world or on screens that do not show it.
 * When a clip cannot be read or decoded it is skipped; when none can, render() returns false for good and the
 * caller draws the vanilla panorama.
 */
public final class VideoBackground {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("newgen_client");
	/** Each clip is two files, its top and bottom halves (clip1a.mp4, clip1b.mp4), decoded side by side. */
	private static final String[] CLIPS = {"clip1", "clip2", "clip3", "clip4"};
	private static final int W = 1920, H = 1080, PARTS = 2, BAND = H / PARTS, FPS = 24, FADE_FRAMES = FPS; // 1 s crossfade
	/** Decoding may run this many frames ahead of the clock; SLOTS frame buffers make that safe (40 MB a clip). */
	private static final int LEAD = 3, SLOTS = LEAD + 2;
	/** A light shade over the footage, so the glass buttons and white text keep their contrast. */
	private static final int SHADE = 0x38000000;
	private static final long IDLE_MS = 500;

	private static Player current, next;
	/** The clip that stopped while no screen showed it: its last picture covers the restart. */
	private static Player stale;
	private static int clip;            // index of current's clip
	private static int failures;        // clips in a row that could not play
	private static boolean disabled;    // no clip can play: always fall back
	private static volatile long lastDrawn;
	/** Players whose textures are freed at the start of the next render(): the GUI draws later in the frame than render()
	 * records it, so freeing a texture in the same render() that drew it made the GPU sample a deleted texture. */
	private static final java.util.List<Player> retired = new java.util.ArrayList<>();
	/** Clip 1's first frame, bundled as a still: shown until the first decoded frame, then faded out over FADE_MS, so the
	 * vanilla panorama never flashes at boot. */
	private static final Identifier POSTER = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/poster.png");
	/** Frame buffer sets (SLOTS x 1920x1080 ints, ~40 MB each) and textures are pooled and reused by later clips: a restart
	 * (after a resource reload or a world) allocates nothing big, so it causes no GC pause. At most two of each exist. */
	private static final java.util.ArrayDeque<int[][]> FRAME_POOL = new java.util.ArrayDeque<>();
	private static final java.util.ArrayDeque<LinearTexture> TEXTURE_POOL = new java.util.ArrayDeque<>();
	private static int textures;
	/** The poster, read off the render thread once and kept as our own texture across resource reloads. */
	private static final Identifier POSTER_DYN = Identifier.fromNamespaceAndPath(NewgenClient.ID, "background_poster");
	private static volatile com.mojang.blaze3d.platform.NativeImage posterImage;
	private static boolean posterLoading, posterReady;
	private static long skipUploadUntil;
	private static final int POSTER_W = 960, POSTER_H = 540, FADE_MS = 300;
	private static long firstFrameAt = -1;

	private VideoBackground() {
	}

	/** Draws the background over the whole screen; false when there is nothing to show (yet), so the caller draws its fallback. */
	public static boolean render(GuiGraphics g, int width, int height) {
		for (Player p : retired) p.release(); // first, so nothing leaks once the video is turned off
		retired.clear();
		if (disabled) return false;
		preparePoster();
		try {
			// During a resource reload (language change, F3+T, packs) the loading overlay is up: decode nothing, start
			// nothing, keep the last picture. lastDrawn is not refreshed, so the decoders idle out and wait for the end.
			if (Minecraft.getInstance().getOverlay() != null) {
				if (current != null && current.shown >= 0) cover(g, current, width, height, 0xFFFFFFFF);
				else poster(g, width, height, 0xFFFFFFFF);
				g.fill(0, 0, width, height, SHADE);
				return true;
			}
			lastDrawn = System.currentTimeMillis();
			// A clip that failed is skipped (and counted: when every clip has failed, the caller's fallback takes over).
			if (current != null && current.failed) {
				current.close();
				current = null;
				clip = (clip + 1) % CLIPS.length;
				if (++failures >= CLIPS.length) {
					disabled = true;
					stopAll();
					return false;
				}
			}
			// A clip that stopped before its end (idle) starts again, its last picture staying on screen until the new
			// one has a frame; one that decoded to the end keeps its last frame until the next clip has faded in.
			if (current == null || current.stopped && !current.complete()) {
				if (current != null && current.shown >= 0 && stale == null) {
					stale = current;
					stale.freeFrames(); // only its texture is still shown; its frame set goes to the next clip
				}
				else if (current != null) current.close();
				current = new Player(clip);
			}
			current.upload();
			if (next != null) next.upload();
			if (current.shown < 0) {
				if (stale != null) cover(g, stale, width, height, 0xFFFFFFFF);
				else poster(g, width, height, 0xFFFFFFFF);
				g.fill(0, 0, width, height, SHADE);
				return true;
			}
			if (firstFrameAt < 0) firstFrameAt = System.currentTimeMillis(); // once a session: the poster fades out at boot only
			if (stale != null) {
				stale.close();
				stale = null;
			}
			failures = 0;
			cover(g, current, width, height, 0xFFFFFFFF);

			// Start the next clip a second before this one ends, and fade it in over that second.
			if (next == null && current.total > 0 && current.shown >= current.total - FADE_FRAMES) next = new Player((clip + 1) % CLIPS.length);
			if (next != null) {
				if (next.failed) {
					next.close();
					next = null;
					clip = (clip + 1) % CLIPS.length; // skip it; current ends and the one after starts
				} else if (next.shown >= 0) {
					float t = Math.min(1f, (next.shown + 1) / (float) FADE_FRAMES);
					cover(g, next, width, height, (int) (t * 255) << 24 | 0xFFFFFF);
					if (t >= 1f) {
						current.close();
						current = next;
						next = null;
						clip = (clip + 1) % CLIPS.length;
					}
				}
			}
			long since = System.currentTimeMillis() - firstFrameAt;
			if (since < FADE_MS) poster(g, width, height, (int) (255 * (1 - since / (float) FADE_MS)) << 24 | 0xFFFFFF);
			g.fill(0, 0, width, height, SHADE);
			return true;
		} catch (Throwable e) {
			LOGGER.warn("Background video turned off", e);
			stopAll();
			disabled = true;
			return false;
		}
	}

	private static void stopAll() {
		if (current != null) current.close();
		if (next != null) next.close();
		if (stale != null) stale.close();
		current = next = stale = null;
	}

	/** Reads the poster on a background thread the first time, then registers it once (render thread, a quick upload). */
	private static void preparePoster() {
		if (posterReady) return;
		if (!posterLoading) {
			posterLoading = true;
			Thread t = new Thread(() -> {
				try (var in = Minecraft.getInstance().getResourceManager().open(POSTER)) {
					posterImage = com.mojang.blaze3d.platform.NativeImage.read(in);
				} catch (Exception e) {
					LOGGER.warn("Background poster could not be read", e);
				}
			}, "Newgen background poster");
			t.setDaemon(true);
			t.start();
		} else if (posterImage != null) {
			DynamicTexture tex = new DynamicTexture(() -> "newgen background poster", posterImage);
			Minecraft.getInstance().getTextureManager().register(POSTER_DYN, tex);
			posterReady = true;
		}
	}

	/** Draws the poster still over the screen, cover-fitted like the video. */
	private static void poster(GuiGraphics g, int width, int height, int color) {
		float scale = Math.max(width / (float) POSTER_W, height / (float) POSTER_H);
		int w = Math.round(POSTER_W * scale), h = Math.round(POSTER_H * scale);
		if (!posterReady) {
			g.fill(0, 0, width, height, 0xFF0D0F11); // a moment at boot until the poster is read
			return;
		}
		g.blit(RenderPipelines.GUI_TEXTURED, POSTER_DYN, (width - w) / 2, (height - h) / 2, 0, 0, w, h, POSTER_W, POSTER_H, POSTER_W, POSTER_H, color);
	}

	/** Draws a player's texture over the screen, scaled to cover it (cropped at the sides or top and bottom); one linear scale. */
	private static void cover(GuiGraphics g, Player p, int width, int height, int color) {
		float scale = Math.max(width / (float) W, height / (float) H);
		int w = Math.round(W * scale), h = Math.round(H * scale);
		g.blit(RenderPipelines.GUI_TEXTURED, p.id, (width - w) / 2, (height - h) / 2, 0, 0, w, h, W, H, W, H, color);
	}

	/**
	 * One clip: a decoder thread per band (the top and bottom halves are separate videos) and the texture the frames
	 * go into. The bands meet at a barrier after every frame; the barrier publishes the frame. Decoding may run LEAD
	 * frames ahead of the clock, which absorbs key frames and GC pauses; the render thread shows the frame the clock
	 * is at (or the newest one if decoding is behind).
	 */
	private static final class Player {
		final Identifier id;
		final LinearTexture texture;
		private final java.util.concurrent.atomic.AtomicInteger bandsAlive = new java.util.concurrent.atomic.AtomicInteger();
		private boolean released;
		private boolean framesFree;
		private boolean closed;

		/** The frames are no longer needed (a stopped clip whose texture still covers a restart). */
		void freeFrames() {
			synchronized (FRAME_POOL) {
				framesFree = true;
			}
			giveBackFrames();
		}
		/** SLOTS frame buffers in the texture's memory order (ABGR ints = RGBA bytes); frame n goes to slot n % SLOTS. */
		private volatile int[][] frames; // allocated by the loader thread
		private final CyclicBarrier barrier;
		private int writing;                  // frame both bands are writing (only touched by the bands and the barrier action)
		private volatile int ready = -1;      // number of the newest finished frame
		private volatile long start;          // when frame 0 was ready; the clock of this clip
		volatile int total;
		volatile boolean failed, stopped;
		int shown = -1;                       // number of the frame in the texture

		/** Registers the texture (render thread); a loader thread reads the files and allocates the buffers, then starts the bands. */
		Player(int index) {
			LinearTexture pooled = TEXTURE_POOL.poll();
			if (pooled == null) {
				pooled = new LinearTexture(Identifier.fromNamespaceAndPath(NewgenClient.ID, "background_video_" + textures++));
				Minecraft.getInstance().getTextureManager().register(pooled.id, pooled);
			}
			texture = pooled;
			id = pooled.id;
			barrier = new CyclicBarrier(PARTS, this::frameDone);
			Thread loader = new Thread(() -> load(index), "Newgen background video loader");
			loader.setDaemon(true);
			loader.setPriority(Thread.MIN_PRIORITY + 1);
			loader.start();
		}

		private void load(int index) {
			byte[][] parts = new byte[PARTS][];
			try {
				for (int p = 0; p < PARTS; p++) {
					Identifier file = Identifier.fromNamespaceAndPath(NewgenClient.ID, "backgrounds/" + CLIPS[index] + (char) ('a' + p) + ".mp4");
					if (stopped) return; // closed while loading: read and allocate nothing more
					try (var in = Minecraft.getInstance().getResourceManager().open(file)) {
						parts[p] = in.readAllBytes();
					}
				}
				if (stopped) return;
				int[][] set;
				synchronized (FRAME_POOL) {
					set = FRAME_POOL.poll();
				}
				frames = set != null ? set : new int[SLOTS][W * H];
			} catch (Throwable e) {
				LOGGER.warn("Background clip {} could not be read", CLIPS[index], e);
				failed = true;
				stopped = true;
				return;
			}
			if (stopped) return;
			bandsAlive.set(PARTS);
			for (int p = 0; p < PARTS; p++) {
				final int band = p;
				Thread thread = new Thread(() -> decode(parts[band], band), "Newgen background video " + (char) ('a' + band));
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY + 1);
				thread.start();
			}
		}

		/** Runs once per frame when every band has written it: publish it, then wait until the next may be written. */
		private void frameDone() {
			int n = writing++;
			if (n == 0) start = System.nanoTime();
			ready = n;
			long wait = start + (n + 1 - LEAD) * 1_000_000_000L / FPS - System.nanoTime();
			try {
				if (wait > 0) Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
			} catch (InterruptedException e) {
				stopped = true;
			}
			if (System.currentTimeMillis() - lastDrawn > IDLE_MS) stopped = true; // no screen shows it any more
		}

		private void decode(byte[] bytes, int band) {
			try {
				FrameGrab grab = FrameGrab.createFrameGrab(ByteBufferSeekableByteChannel.readFromByteBuffer(ByteBuffer.wrap(bytes)));
				if (band == 0) total = grab.getVideoTrack().getMeta().getTotalFrames();
				for (int n = 0; !stopped; n++) {
					Picture picture = grab.getNativeFrame();
					if (picture == null) break;
					convert(picture, frames[n % SLOTS], band * BAND);
					barrier.await();
				}
			} catch (BrokenBarrierException e) {
				// the other band stopped or failed
			} catch (Throwable e) {
				LOGGER.warn("Background clip could not be decoded", e);
				failed = true;
			}
			stopped = true;
			barrier.reset(); // wakes the other band
			if (bandsAlive.decrementAndGet() == 0) giveBackFrames();
		}

		/** The frame set goes back to the pool once no band writes to it and the player is released. */
		private void giveBackFrames() {
			synchronized (FRAME_POOL) {
				if (!(released || framesFree) || bandsAlive.get() > 0 || frames == null) return;
				if (FRAME_POOL.size() < 2) FRAME_POOL.push(frames);
				frames = null;
			}
		}

		/** One band, YUV 4:2:0 (BT.709, limited range; JCodec stores samples minus 128) to opaque RGBA, rows top..top+BAND. */
		private static void convert(Picture p, int[] out, int top) {
			byte[] y = p.getPlaneData(0), u = p.getPlaneData(1), v = p.getPlaneData(2);
			int yw = p.getPlaneWidth(0), cw = p.getPlaneWidth(1);
			for (int row = 0; row < BAND; row++) {
				int yo = row * yw, co = (row >> 1) * cw, o = (top + row) * W;
				for (int col = 0; col < W; col++) {
					int c = 298 * (y[yo + col] + 128 - 16);
					int d = u[co + (col >> 1)], e = v[co + (col >> 1)];
					int r = clamp((c + 459 * e + 128) >> 8);
					int gr = clamp((c - 55 * d - 136 * e + 128) >> 8);
					int b = clamp((c + 541 * d + 128) >> 8);
					out[o + col] = 0xFF000000 | b << 16 | gr << 8 | r;
				}
			}
		}

		private static int clamp(int x) {
			return x < 0 ? 0 : Math.min(255, x);
		}

		/** Copies the frame the clock is at (or the newest, if decoding is behind) into the texture. Render thread only. */
		void upload() {
			int newest = ready;
			if (newest < 0) return;
			int n = Math.min(newest, (int) ((System.nanoTime() - start) * FPS / 1_000_000_000L));
			if (n <= shown) return;
			int[][] set;
			synchronized (FRAME_POOL) { // the frames may be handed back by a band thread at the same time
				set = released || framesFree ? null : frames;
			}
			if (set == null || System.nanoTime() < skipUploadUntil) return;
			long t0 = System.nanoTime();
			MemoryUtil.memIntBuffer(texture.getPixels().getPointer(), W * H).put(set[n % SLOTS]);
			texture.upload();
			shown = n;
			// A copy and upload over 5 ms (a slow machine, a busy driver) skips the next frame's, so the game keeps its pace.
			long spent = System.nanoTime() - t0;
			if (spent > 5_000_000L) skipUploadUntil = System.nanoTime() + spent;
		}

		boolean complete() {
			return total > 0 && ready >= total - 1;
		}

		/** Stops decoding now; the texture goes at the start of the next render() (see retired). */
		void close() {
			if (closed) return; // once only: a second close must not put its texture in the pool twice
			closed = true;
			stopped = true;
			barrier.reset();
			retired.add(this);
		}

		/** Back to the pools: the texture now (render thread, the frame that drew it is done), the frames once the bands stop. */
		void release() {
			if (TEXTURE_POOL.size() < 2) TEXTURE_POOL.push(texture);
			else Minecraft.getInstance().getTextureManager().release(id);
			synchronized (FRAME_POOL) {
				released = true;
			}
			giveBackFrames();
		}
	}

	/** A dynamic texture sampled smoothly, so frames drawn larger or smaller than 1920x1080 stay smooth. */
	private static final class LinearTexture extends DynamicTexture {
		final Identifier id;

		LinearTexture(Identifier id) {
			super(() -> "newgen background video", W, H, false);
			sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
			this.id = id;
		}
	}
}
