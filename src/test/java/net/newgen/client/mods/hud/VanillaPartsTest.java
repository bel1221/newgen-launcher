package net.newgen.client.mods.hud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Wave 3 vanilla HUD
class VanillaPartsTest {
	@Test
	void smoothHealthEasesThenSnaps() {
		HealthPartMod.Smoother s = new HealthPartMod.Smoother();
		assertEquals(20, s.update(20, 0, 6)); // first frame: no animation
		int mid = s.update(10, 100, 6); // 0.1 s after taking 5 hearts of damage
		assertTrue(mid > 10 && mid < 20, "eases, got " + mid);
		int later = s.update(10, 400, 6);
		assertTrue(later <= mid && later >= 10);
		assertEquals(10, s.update(10, 2000, 6)); // settles on the real value
		assertEquals(14, s.update(14, 4000, 6)); // a long pause (dt capped at 1 s) still ends close enough to snap
	}

	@Test
	void smoothHealthResetJumps() {
		HealthPartMod.Smoother s = new HealthPartMod.Smoother();
		s.update(20, 0, 6);
		s.reset();
		assertEquals(4, s.update(4, 10, 6));
	}

	@Test
	void yawToMatchesMinecraftYaw() {
		assertEquals(0f, DirectionHudMod.yawTo(0, 10), 0.001f); // south
		assertEquals(90f, DirectionHudMod.yawTo(-10, 0), 0.001f); // west
		assertEquals(-90f, DirectionHudMod.yawTo(10, 0), 0.001f); // east
		assertEquals(180f, Math.abs(DirectionHudMod.yawTo(0, -10)), 0.001f); // north
	}

	@Test
	void artScalesDownKeepingShape() {
		SystemMedia.Art art = SystemMedia.scale(128, 64, (x, y) -> x < 64 ? 0xFF000000 : 0xFFFFFFFF, 64);
		assertEquals(64, art.width());
		assertEquals(32, art.height());
		assertEquals(0xFF000000, art.argb()[0]);
		assertEquals(0xFFFFFFFF, art.argb()[63]);
		SystemMedia.Art small = SystemMedia.scale(2, 1, (x, y) -> x, 64); // never enlarged
		assertArrayEquals(new int[] {0, 1}, small.argb());
	}
}
