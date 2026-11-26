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
		if (!on) current = null;
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
		return out(operation, 8); // IAsyncOperation<T>.GetResults
	}
}
