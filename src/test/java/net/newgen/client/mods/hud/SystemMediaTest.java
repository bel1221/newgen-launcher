package net.newgen.client.mods.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** The Media Player's Windows reader, against the real Windows media controls of the machine running the tests. */
class SystemMediaTest {
	@Test
	void guidLayout() {
		Memory m = SystemMedia.guid("00000036-0000-0000-c000-000000000046");
		assertEquals(0x36, m.getInt(0));
		assertEquals((byte) 0xC0, m.getByte(8));
		assertEquals((byte) 0x46, m.getByte(15));
	}

	/** Gets the session manager and reads the current session (if any) without crashing; prints what it found. */
	@Test
	void readsTheCurrentSession() {
		Assumptions.assumeTrue(SystemMedia.isWindows(), "Windows only");
		SystemMedia.Combase.INSTANCE.RoInitialize(1);
		Pointer manager = SystemMedia.requestManager();
		assertNotNull(manager, "session manager");
		for (int i = 0; i < 3; i++) {
			SystemMedia.Track track = SystemMedia.read(manager);
			System.out.println("Media Player test read: " + track);
		}
	}

	@Test
	void positionMovesOnlyWhilePlaying() {
		SystemMedia.Track playing = new SystemMedia.Track("t", "a", true, 10_000, 60_000, 1_000);
		assertEquals(15_000, playing.position(6_000));
		assertEquals(60_000, playing.position(999_000), "capped at the end");
		SystemMedia.Track paused = new SystemMedia.Track("t", "a", false, 10_000, 60_000, 1_000);
		assertEquals(10_000, paused.position(6_000));
	}
}
