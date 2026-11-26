package net.newgen.client.mods.hud;

import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the computer is playing (Spotify, a browser tab, any player that shows in the Windows volume flyout), read from
 * Windows' media controls: Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, a WinRT class.
 * It is called through JNA (which Minecraft already ships) straight on the COM vtables, so no native library is added.
 * Written from the public Windows API; other systems simply show nothing.
 *
 * <p>One background thread reads the current session once a second while the HUD is on, and runs play/pause/skip
 * commands. The game thread only reads {@link #current()}.
 */
final class SystemMedia {
	private static final Logger LOG = LoggerFactory.getLogger("newgen_client");

	/** A snapshot of the playing track. Positions in ms; {@code readAt} is when {@code positionMs} was true. */
	record Track(String title, String artist, boolean playing, long positionMs, long durationMs, long readAt) {
		/** The position now: it keeps moving while playing, between the (rare) updates players send. */
		long position(long now) {
			long p = playing ? positionMs + Math.max(0, now - readAt) : positionMs;
			return durationMs > 0 ? Math.min(p, durationMs) : p;
		}
	}

	enum Command {
		TOGGLE(20), NEXT(16), PREVIOUS(17);

		/** Slot of TryTogglePlayPauseAsync / TrySkipNextAsync / TrySkipPreviousAsync on the session. */
		final int slot;

		Command(int slot) {
			this.slot = slot;
		}
	}

	private static volatile Track current;
	// Wave 3 vanilla HUD: album art of the current track, decoded on the reader thread.
	private static volatile Art art;
	private static String artKey = "";
	private static int artTries;
	private static volatile boolean wanted;
	private static Thread thread;
	private static final BlockingQueue<Command> COMMANDS = new LinkedBlockingQueue<>();

	private SystemMedia() {
	}

	static Track current() {
		return current;
	}

	static void command(Command command) {
		if (wanted) COMMANDS.offer(command);
	}

	/** Reads while the HUD is on, stops (and lets the thread end) when it is off. */
	static synchronized void setActive(boolean on) {
		wanted = on;
		if (!on) {
			current = null;
			art = null;
			artKey = "";
		}
		if (on && (thread == null || !thread.isAlive()) && isWindows()) {
			thread = new Thread(SystemMedia::run, "newgen-media");
			thread.setDaemon(true);
			thread.start();
		}
	}

	static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
	}

	// ---- the reader thread ----

	private static void run() {
		Pointer manager = null;
		try {
			Combase.INSTANCE.RoInitialize(1); // multithreaded apartment (S_FALSE when already initialised is fine)
			manager = requestManager();
			if (manager == null) {
				LOG.warn("Media Player: Windows media controls are not available");
				return;
			}
			while (wanted) {
				current = read(manager);
				Command command = COMMANDS.poll(1, TimeUnit.SECONDS);
				while (command != null) {
					run(manager, command);
					command = COMMANDS.poll();
				}
			}
		} catch (Throwable t) {
			LOG.warn("Media Player stopped reading", t);
		} finally {
			current = null;
			release(manager);
		}
	}

	/** GlobalSystemMediaTransportControlsSessionManager.RequestAsync(), awaited. */
	static Pointer requestManager() {
		PointerByReference name = new PointerByReference();
		String cls = "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager";
		if (Combase.INSTANCE.WindowsCreateString(new WString(cls), cls.length(), name) < 0) return null;
		try {
			PointerByReference factory = new PointerByReference();
			if (Combase.INSTANCE.RoGetActivationFactory(name.getValue(), MANAGER_STATICS, factory) < 0) return null;
			try {
				Pointer op = out(factory.getValue(), 6); // IGlobalSystemMediaTransportControlsSessionManagerStatics.RequestAsync
				try {
					return await(op, 5000);
				} finally {
					release(op);
				}
			} finally {
				release(factory.getValue());
			}
		} finally {
			Combase.INSTANCE.WindowsDeleteString(name.getValue());
		}
	}

	/** The current session's track, or null when nothing is playing. */
	static Track read(Pointer manager) {
		Pointer session = out(manager, 6); // GetCurrentSession
		if (session == null) return null;
		try {
			boolean playing = false;
			Pointer info = out(session, 9); // GetPlaybackInfo
			if (info != null) {
				playing = int32(info, 7) == 4; // PlaybackStatus: 4 = Playing
				release(info);
			}

			long position = 0, duration = 0, readAt = System.currentTimeMillis();
			Pointer timeline = out(session, 8); // GetTimelineProperties
			if (timeline != null) {
				long start = int64(timeline, 6), end = int64(timeline, 7), pos = int64(timeline, 10), updated = int64(timeline, 11);
				release(timeline);
				duration = Math.max(0, (end - start) / 10_000); // TimeSpan: 100 ns units
				position = Math.max(0, (pos - start) / 10_000);
				// DateTime: 100 ns units since 1601-01-01 UTC
				if (updated > 0) readAt = Math.min(readAt, (updated - 116_444_736_000_000_000L) / 10_000);
			}

			String title = "", artist = "";
			Pointer op = out(session, 7); // TryGetMediaPropertiesAsync
			if (op != null) {
				Pointer props = await(op, 2000);
				release(op);
				if (props != null) {
					title = hstring(props, 6); // Title
					artist = hstring(props, 9); // Artist
					try {
						readArt(props, title + "|" + artist);
					} catch (Throwable t) {
						art = null; // no art is fine; never stop the reader for it
					}
					release(props);
				}
			}
			if (title.isBlank()) return null;
			return new Track(title.strip(), artist.strip(), playing, position, duration, readAt);
		} finally {
			release(session);
		}
	}

	private static void run(Pointer manager, Command command) {
		Pointer session = out(manager, 6);
		if (session == null) return;
		try {
			release(out(session, command.slot)); // the IAsyncOperation<bool>; the result is not needed
		} finally {
			release(session);
		}
	}

	// ---- album art (Wave 3 vanilla HUD) ----

	/** Album art scaled to fit {@link #ART_SIDE}, as ARGB pixels row by row. A new object for each new picture. */
	record Art(int width, int height, int[] argb) {
	}

	static final int ART_SIDE = 64;
	private static final int MAX_ART_BYTES = 8 * 1024 * 1024;

	static Art art() {
		return art;
	}

	/** Reads the thumbnail once per track (a few tries: players often set it a moment after the title). */
	private static void readArt(Pointer props, String key) {
		if (!key.equals(artKey)) {
			artKey = key;
			artTries = 0;
			art = null;
		}
		if (art != null || artTries >= 3) return;
		artTries++;
		Pointer ref = out(props, 15); // IGlobalSystemMediaTransportControlsSessionMediaProperties.Thumbnail
		if (ref == null) return;
		try {
			byte[] bytes = readStream(ref);
			if (bytes != null) art = decode(bytes, ART_SIDE);
		} finally {
			release(ref);
		}
	}

	private static final Memory RANDOM_ACCESS_STREAM = guid("905a0fe1-bc53-11df-8c49-001e4fc686da");
	private static final Memory INPUT_STREAM = guid("905a0fe2-bc53-11df-8c49-001e4fc686da");
	private static final Memory BUFFER_BYTE_ACCESS = guid("905a0fef-bc53-11df-8c49-001e4fc686da");
	private static final Memory BUFFER_FACTORY = guid("71af914d-c10f-484b-bc50-14bc623b3a27");

	/** IRandomAccessStreamReference.OpenReadAsync, then the whole stream through IInputStream.ReadAsync into a Buffer. */
	private static byte[] readStream(Pointer ref) {
		Pointer op = out(ref, 6); // OpenReadAsync
		Pointer stream = await(op, 2000);
		release(op);
		if (stream == null) return null;
		Pointer random = null, input = null, buffer = null;
		try {
			random = query(stream, RANDOM_ACCESS_STREAM);
			input = query(stream, INPUT_STREAM);
			if (random == null || input == null) return null;
			long size = int64(random, 6); // get_Size
			if (size <= 0 || size > MAX_ART_BYTES) return null;
			buffer = newBuffer((int) size);
			if (buffer == null) return null;
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream((int) size);
			while (out.size() < size) {
				PointerByReference readOp = new PointerByReference();
				// ReadAsync(buffer, count, InputStreamOptions.None, out operation)
				if (call(input, 6, buffer, (int) size - out.size(), 0, readOp) < 0) return null;
				Pointer result = await(readOp.getValue(), 3000, 10); // IAsyncOperationWithProgress<IBuffer, UInt32>
				release(readOp.getValue());
				if (result == null) return null;
				try {
					byte[] chunk = bytes(result);
					if (chunk == null || chunk.length == 0) break;
					out.write(chunk, 0, chunk.length);
				} finally {
					release(result);
				}
			}
			return out.toByteArray();
		} finally {
			release(buffer);
			release(input);
			release(random);
			release(stream);
		}
	}

	/** A new Windows.Storage.Streams.Buffer of this capacity. */
	private static Pointer newBuffer(int capacity) {
		PointerByReference name = new PointerByReference();
		String cls = "Windows.Storage.Streams.Buffer";
		if (Combase.INSTANCE.WindowsCreateString(new WString(cls), cls.length(), name) < 0) return null;
		try {
			PointerByReference factory = new PointerByReference();
			if (Combase.INSTANCE.RoGetActivationFactory(name.getValue(), BUFFER_FACTORY, factory) < 0) return null;
			try {
				PointerByReference buffer = new PointerByReference();
				return call(factory.getValue(), 6, capacity, buffer) < 0 ? null : buffer.getValue(); // IBufferFactory.Create
			} finally {
				release(factory.getValue());
			}
		} finally {
			Combase.INSTANCE.WindowsDeleteString(name.getValue());
		}
	}

	/** The filled bytes of an IBuffer: its Length, copied out through IBufferByteAccess. */
	private static byte[] bytes(Pointer buffer) {
		int length = int32(buffer, 7); // IBuffer.get_Length
		if (length <= 0) return new byte[0];
		Pointer access = query(buffer, BUFFER_BYTE_ACCESS);
		if (access == null) return null;
		try {
			PointerByReference data = new PointerByReference();
			if (call(access, 3, data) < 0 || data.getValue() == null) return null; // IBufferByteAccess.Buffer
			return data.getValue().getByteArray(0, length);
		} finally {
			release(access);
		}
	}

	private static Pointer query(Pointer object, Memory iid) {
		PointerByReference result = new PointerByReference();
		return call(object, 0, iid, result) < 0 ? null : result.getValue();
	}

	/**
	 * Decodes PNG, JPEG, BMP or GIF bytes with the game's own stb_image (no AWT) and scales the picture down to fit
	 * side × side. Null when the bytes are not an image stb can read.
	 */
	static Art decode(byte[] bytes, int side) {
		java.nio.ByteBuffer data = org.lwjgl.system.MemoryUtil.memAlloc(bytes.length);
		try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
			data.put(bytes).flip();
			java.nio.IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), comp = stack.mallocInt(1);
			java.nio.ByteBuffer pixels = org.lwjgl.stb.STBImage.stbi_load_from_memory(data, w, h, comp, 4);
			if (pixels == null) return null;
			try {
				return scale(w.get(0), h.get(0), (x, y) -> {
					int i = (y * w.get(0) + x) * 4;
					int r = pixels.get(i) & 0xFF, g = pixels.get(i + 1) & 0xFF, b = pixels.get(i + 2) & 0xFF, a = pixels.get(i + 3) & 0xFF;
					return a << 24 | r << 16 | g << 8 | b;
				}, side);
			} finally {
				org.lwjgl.stb.STBImage.stbi_image_free(pixels);
			}
		} finally {
			org.lwjgl.system.MemoryUtil.memFree(data);
		}
	}

	interface Pixels {
		int argb(int x, int y);
	}

	/** Nearest-neighbour scale of a w × h picture to fit side × side (never larger), keeping its shape. */
	static Art scale(int w, int h, Pixels source, int side) {
		if (w <= 0 || h <= 0) return null;
		double s = Math.min(1.0, (double) side / Math.max(w, h));
		int ow = Math.max(1, (int) Math.round(w * s)), oh = Math.max(1, (int) Math.round(h * s));
		int[] out = new int[ow * oh];
		for (int y = 0; y < oh; y++)
			for (int x = 0; x < ow; x++) out[y * ow + x] = source.argb(Math.min(w - 1, x * w / ow), Math.min(h - 1, y * h / oh));
		return new Art(ow, oh, out);
	}

	// ---- COM / WinRT plumbing ----

	interface Combase extends Library {
		Combase INSTANCE = Native.load("combase", Combase.class);

		int RoInitialize(int type);

		int RoGetActivationFactory(Pointer activatableClassId, Pointer iid, PointerByReference factory);

		int WindowsCreateString(WString source, int length, PointerByReference string);

		int WindowsDeleteString(Pointer string);

		Pointer WindowsGetStringRawBuffer(Pointer string, IntByReference length);
	}

	private static final Memory MANAGER_STATICS = guid("2050c4ee-11a0-57de-aed7-c97c70338245");
	private static final Memory ASYNC_INFO = guid("00000036-0000-0000-c000-000000000046");

	/** A GUID in memory: Data1 (4 bytes), Data2 and Data3 (2 each) little-endian, then 8 bytes as written. */
	static Memory guid(String text) {
		String hex = text.replace("-", "");
		Memory m = new Memory(16);
		m.setInt(0, (int) Long.parseLong(hex.substring(0, 8), 16));
		m.setShort(4, (short) Integer.parseInt(hex.substring(8, 12), 16));
		m.setShort(6, (short) Integer.parseInt(hex.substring(12, 16), 16));
		for (int i = 0; i < 8; i++) m.setByte(8 + i, (byte) Integer.parseInt(hex.substring(16 + i * 2, 18 + i * 2), 16));
		return m;
	}

	/** Calls method number {@code slot} of a COM object (0-2 IUnknown, 3-5 IInspectable, then the interface's own). */
	private static int call(Pointer object, int slot, Object... args) {
		Pointer method = object.getPointer(0).getPointer((long) slot * Native.POINTER_SIZE);
		Object[] all = new Object[args.length + 1];
		all[0] = object;
		System.arraycopy(args, 0, all, 1, args.length);
		return Function.getFunction(method).invokeInt(all);
	}

	private static void release(Pointer object) {
		if (object != null) call(object, 2);
	}

	/** A method whose only parameter is an [out] interface pointer; null on failure or when it returns none. */
	private static Pointer out(Pointer object, int slot) {
		PointerByReference result = new PointerByReference();
		return call(object, slot, result) < 0 ? null : result.getValue();
	}

	private static int int32(Pointer object, int slot) {
		IntByReference result = new IntByReference();
		return call(object, slot, result) < 0 ? -1 : result.getValue();
	}

	private static long int64(Pointer object, int slot) {
		LongByReference result = new LongByReference();
		return call(object, slot, result) < 0 ? 0 : result.getValue();
	}

	/** A string property (HSTRING out parameter), freed after copying. */
	private static String hstring(Pointer object, int slot) {
		PointerByReference result = new PointerByReference();
		if (call(object, slot, result) < 0 || result.getValue() == null) return "";
		try {
			IntByReference length = new IntByReference();
			Pointer chars = Combase.INSTANCE.WindowsGetStringRawBuffer(result.getValue(), length);
			return chars == null ? "" : new String(chars.getCharArray(0, length.getValue()));
		} finally {
			Combase.INSTANCE.WindowsDeleteString(result.getValue());
		}
	}

	/** Waits for an IAsyncOperation to complete (via IAsyncInfo.Status) and returns GetResults(), or null. */
	private static Pointer await(Pointer operation, long timeoutMs) {
		return await(operation, timeoutMs, 8); // IAsyncOperation<T>.GetResults
	}

	/** As above, with the slot of GetResults (10 for IAsyncOperationWithProgress). */
	private static Pointer await(Pointer operation, long timeoutMs, int resultsSlot) {
		if (operation == null) return null;
		PointerByReference infoRef = new PointerByReference();
		if (call(operation, 0, ASYNC_INFO, infoRef) < 0) return null; // QueryInterface
		Pointer info = infoRef.getValue();
		try {
			long deadline = System.currentTimeMillis() + timeoutMs;
			while (true) {
				int status = int32(info, 7); // IAsyncInfo.Status: 0 started, 1 completed, 2 canceled, 3 error
				if (status == 1) break;
				if (status != 0 || System.currentTimeMillis() > deadline) return null;
				Thread.sleep(5);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} finally {
			release(info);
		}
		return out(operation, resultsSlot);
	}
}
