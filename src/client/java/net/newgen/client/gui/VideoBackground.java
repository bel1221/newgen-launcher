package net.newgen.client.gui;

import java.nio.ByteBuffer;

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
 * 1280x720, 24 fps, H.264 baseline, CRF 19) played one after another with a 1 s crossfade, cover-fitted to the screen.
 * <p>
 * Each clip is decoded by JCodec on its own low-priority daemon thread (13-17 ms a frame at 720p on an i5-12400F, 32-40 % of one core, measured), paced to
 * 24 fps; the render thread only copies the newest frame into a texture. A decoder stops by itself when no screen
 * has drawn the background for 500 ms, so nothing runs in a world or on screens that do not show it.
 * When a clip cannot be read or decoded it is skipped; when none can, render() returns false for good and the
 * caller draws the vanilla panorama.
 */
public final class VideoBackground {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("newgen_client");
	private static final String[] CLIPS = {"clip1.mp4", "clip2.mp4", "clip3.mp4", "clip4.mp4"};
	private static final int W = 1280, H = 720, FPS = 24, FADE_FRAMES = FPS; // 1 s crossfade
	/** A light shade over the footage, so the glass buttons and white text keep their contrast. */
	private static final int SHADE = 0x38000000;
	private static final long IDLE_MS = 500;

	private static Player current, next;
	private static int clip;            // index of current's clip
	private static int failures;        // clips in a row that could not play
	private static boolean disabled;    // no clip can play: always fall back
	private static volatile long lastDrawn;

	private VideoBackground() {
	}

	/** Draws the background over the whole screen; false when there is nothing to show (yet), so the caller draws its fallback. */
	public static boolean render(GuiGraphics g, int width, int height) {
		if (disabled) return false;
		try {
			lastDrawn = System.currentTimeMillis();
			if (current == null || current.stopped) {
				if (current != null) current.close();
				current = start(clip);
				if (current == null) return false;
			}
			current.upload();
			if (next != null) next.upload();

			if (current.failed) {
				current.close();
				current = null;
				if (++failures >= CLIPS.length) disabled = true;
				clip = (clip + 1) % CLIPS.length;
				return false;
			}
			if (current.shown < 0) return false;
			failures = 0;
			cover(g, current, width, height, 0xFFFFFFFF);

			// Start the next clip a second before this one ends, and fade it in over that second.
			if (next == null && current.total > 0 && current.shown >= current.total - FADE_FRAMES) next = start((clip + 1) % CLIPS.length);
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
		current = next = null;
	}

	private static Player start(int index) {
		Identifier file = Identifier.fromNamespaceAndPath(NewgenClient.ID, "backgrounds/" + CLIPS[index]);
		byte[] bytes;
		try (var in = Minecraft.getInstance().getResourceManager().open(file)) {
			bytes = in.readAllBytes();
		} catch (Exception e) {
			LOGGER.warn("Background clip {} missing", file);
			Player p = new Player(index, null);
			p.failed = true;
			return p;
		}
		return new Player(index, bytes);
	}

	/** Draws a player's texture over the screen, scaled to cover it (cropped at the sides or top and bottom). */
	private static void cover(GuiGraphics g, Player p, int width, int height, int color) {
		float scale = Math.max(width / (float) W, height / (float) H);
		int w = Math.round(W * scale), h = Math.round(H * scale);
		g.blit(RenderPipelines.GUI_TEXTURED, p.id, (width - w) / 2, (height - h) / 2, 0, 0, w, h, W, H, W, H, color);
	}

	/** One clip: a decoder thread and the texture its frames go into. */
	private static final class Player {
		final Identifier id;
		final DynamicTexture texture;
		/** Three frame buffers in the texture's memory order (ABGR ints = RGBA bytes); the thread fills them in turn. */
		private final int[][] frames = new int[3][W * H];
		private volatile int ready = -1;      // number of the newest finished frame
		private volatile int readyBuffer;
		volatile int total;
		volatile boolean failed, stopped;
		int shown = -1;                       // number of the frame in the texture

		Player(int index, byte[] bytes) {
			id = Identifier.fromNamespaceAndPath(NewgenClient.ID, "background_video_" + index + "_" + System.nanoTime());
			texture = new LinearTexture();
			Minecraft.getInstance().getTextureManager().register(id, texture);
			if (bytes == null) return;
			Thread thread = new Thread(() -> decode(bytes), "Newgen background video");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY + 1);
			thread.start();
		}

		private void decode(byte[] bytes) {
			try {
				FrameGrab grab = FrameGrab.createFrameGrab(ByteBufferSeekableByteChannel.readFromByteBuffer(ByteBuffer.wrap(bytes)));
				total = grab.getVideoTrack().getMeta().getTotalFrames();
				long start = System.nanoTime();
				for (int n = 0; !stopped; n++) {
					Picture picture = grab.getNativeFrame();
					if (picture == null) break;
					int buffer = n % 3;
					convert(picture, frames[buffer]);
					long wait = start + n * 1_000_000_000L / FPS - System.nanoTime();
					if (wait > 0) Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
					readyBuffer = buffer;
					ready = n;
					if (System.currentTimeMillis() - lastDrawn > IDLE_MS) break; // no screen shows it any more
				}
			} catch (Throwable e) {
				LOGGER.warn("Background clip could not be decoded", e);
				failed = true;
			}
			stopped = true;
		}

		/** YUV 4:2:0 (BT.709, limited range; JCodec stores samples minus 128) to opaque RGBA. */
		private static void convert(Picture p, int[] out) {
			byte[] y = p.getPlaneData(0), u = p.getPlaneData(1), v = p.getPlaneData(2);
			int yw = p.getPlaneWidth(0), cw = p.getPlaneWidth(1);
			for (int row = 0; row < H; row++) {
				int yo = row * yw, co = (row >> 1) * cw, o = row * W;
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

		/** Copies the newest decoded frame into the texture, if there is a new one. Render thread only. */
		void upload() {
			int n = ready;
			if (n < 0 || n == shown) return;
			MemoryUtil.memIntBuffer(texture.getPixels().getPointer(), W * H).put(frames[readyBuffer]);
			texture.upload();
			shown = n;
		}

		void close() {
			stopped = true;
			Minecraft.getInstance().getTextureManager().release(id);
		}
	}

	/** A dynamic texture sampled smoothly, so frames drawn larger or smaller than 1280x720 stay smooth. */
	private static final class LinearTexture extends DynamicTexture {
		LinearTexture() {
			super(() -> "newgen background video", W, H, false);
			sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}
}
