package net.newgen.client.mods.hud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudLogic2Test {
	@Test
	void entityStats() {
		assertArrayEquals(new int[]{12, 345}, HudLogic2.parseEntityStats("E: 12/345, SD: 12"));
		assertArrayEquals(new int[]{0, 7}, HudLogic2.parseEntityStats("E: 0/7"));
		assertNull(HudLogic2.parseEntityStats(null));
		assertNull(HudLogic2.parseEntityStats("garbage"));
	}

	@Test
	void shortKeys() {
		assertEquals("1", HudLogic2.shortKey("1"));
		assertEquals("LS", HudLogic2.shortKey("Left Shift"));
		assertEquals("MB4", HudLogic2.shortKey("Mouse Button 4"));
		assertEquals("K12", HudLogic2.shortKey("Keypad 12"));
		assertEquals("", HudLogic2.shortKey(null));
	}

	@Test
	void projectStraightAheadIsCentre() {
		// yaw 0 looks south (+Z)
		double[] p = HudLogic2.project(0, 0, 1, 0, 0, 70, 400, 200);
		assertEquals(200, p[0], 1e-6);
		assertEquals(100, p[1], 1e-6);
		assertNull(HudLogic2.project(0, 0, -1, 0, 0, 70, 400, 200));
	}

	@Test
	void projectSidesAndUp() {
		// Looking south, west (-X) is to the right; up is up on the screen.
		double[] right = HudLogic2.project(-0.2, 0, 1, 0, 0, 70, 400, 200);
		assertTrue(right[0] > 200);
		double[] up = HudLogic2.project(0, 0.2, 1, 0, 0, 70, 400, 200);
		assertTrue(up[1] < 100);
		// Looking west (yaw 90): moving west is centred.
		double[] west = HudLogic2.project(-1, 0, 0, 90, 0, 70, 400, 200);
		assertEquals(200, west[0], 1e-6);
		// Looking down (pitch 90): falling is centred.
		double[] down = HudLogic2.project(0, -1, 0, 0, 90, 70, 400, 200);
		assertEquals(200, down[0], 1e-6);
		assertEquals(100, down[1], 1e-6);
		// Edge of the vertical FOV lands on the top edge.
		double[] edge = HudLogic2.project(0, Math.tan(Math.toRadians(35)), 1, 0, 0, 70, 400, 200);
		assertEquals(0, edge[1], 1e-6);
	}

	@Test
	void placeCounter() {
		HudLogic2.PlaceCounter c = new HudLogic2.PlaceCounter();
		c.place(0);
		c.place(500);
		c.place(1500);
		assertEquals(1, c.within(1600, 1000));
		assertEquals(2, c.within(1400, 1000));
		assertEquals(3, c.within(1600, 60_000));
		assertEquals(1, c.within(61_000, 60_000));
		assertEquals(3, c.total());
		c.autoReset(50_000, 60_000);
		assertEquals(3, c.total());
		c.autoReset(60_000, 60_000);
		assertEquals(0, c.total());
		c.place(60_100);
		c.autoReset(200_000, 0);
		assertEquals(1, c.total());
	}

	@Test
	void mouseTrail() {
		HudLogic2.MouseTrail t = new HudLogic2.MouseTrail();
		t.update(100, -5, 1 / 60.0, 0, 20, 3);
		assertEquals(20, t.x, 1e-9);
		assertEquals(-5, t.y, 1e-9);
		t.update(0, 0, 1 / 60.0, 0.5, 20, 3);
		assertEquals(10, t.x, 1e-9);
		t.update(0, 0, 1 / 60.0, 0.5, 20, 3);
		t.update(0, 0, 1 / 60.0, 0.5, 20, 3);
		assertEquals(3, t.points.size());
	}
}
