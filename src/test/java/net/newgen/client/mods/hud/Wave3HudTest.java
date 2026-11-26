package net.newgen.client.mods.hud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class Wave3HudTest {
	@Test
	void speedFormat() {
		assertEquals("4.32 b/s", SpeedMod.format(4.317, "Blocks/s", 2, true));
		assertEquals("15.5 km/h", SpeedMod.format(4.317, "km/h", 1, true));
		assertEquals("4", SpeedMod.format(4.317, "m/s", 0, false));
		assertEquals("0.00 m/s", SpeedMod.format(-0.0001, "m/s", 2, true));
		assertEquals(5.0, SpeedMod.smooth(0, 10, 0.5), 1e-9);
		assertEquals(10.0, SpeedMod.smooth(0, 10, 0), 1e-9);
	}

	@Test
	void tpsEstimate() {
		assertEquals(20.0, TpsMod.estimate(new long[] {1000, 1000}, 200), 1e-9);
		assertEquals(10.0, TpsMod.estimate(new long[] {2000, 2000}, 500), 1e-9);
		assertEquals(20.0, TpsMod.estimate(new long[] {900}, 100), 1e-9); // faster than 20 is capped
		assertEquals(5.0, TpsMod.estimate(new long[] {1000}, 4000), 1e-9); // stalled now: the wait counts
		assertTrue(Double.isNaN(TpsMod.estimate(new long[0], 0)));
	}

	@Test
	void batteryParse() {
		assertNull(BatteryMod.parse(1, 128, 255, -1)); // desktop: no system battery
		assertNull(BatteryMod.parse(255, 255, 255, -1)); // unknown
		BatteryMod.Reading r = BatteryMod.parse(0, 1, 85, 7500);
		assertEquals(85, r.percent());
		assertFalse(r.charging());
		assertEquals("85% (2:05)", BatteryMod.text(r, true, true));
		BatteryMod.Reading c = BatteryMod.parse(1, 8, 40, -1);
		assertTrue(c.charging());
		assertEquals("+40%", BatteryMod.text(c, true, true));
	}

	@Test
	void facing() {
		assertEquals("S", FacingMod.eightWay(0));
		assertEquals("W", FacingMod.eightWay(90));
		assertEquals("N", FacingMod.eightWay(180));
		assertEquals("N", FacingMod.eightWay(-180));
		assertEquals("E", FacingMod.eightWay(-90));
		assertEquals("NE", FacingMod.eightWay(-135));
		assertEquals("+Z", FacingMod.axis(10));
		assertEquals("-X", FacingMod.axis(95));
		assertEquals("E", FacingMod.fourWay(-80));
		assertEquals("90°", FacingMod.degrees(-90, 0));
		assertEquals(0.0, FacingMod.bearing(180), 1e-9);
	}

	@Test
	void clockHands() {
		assertArrayEquals(new double[] {90, 0, 0}, AnalogClockMod.handAngles(3 * 3600), 1e-9);
		assertArrayEquals(new double[] {15, 180, 0}, AnalogClockMod.handAngles(12 * 3600 + 30 * 60), 1e-9);
		assertEquals(6 * 3600, AnalogClockMod.gameSeconds(0), 1e-9);
		assertEquals(18 * 3600, AnalogClockMod.gameSeconds(12000 + 24000 * 3), 1e-9);
	}

	@Test
	void customText() {
		assertEquals(List.of("a", "b", "c"), CustomTextMod.split("a\\nb\nc"));
		Map<String, String> v = Map.of("fps", "144", "x", "10");
		assertEquals("144 fps at 10 {nope}", CustomTextMod.fill("{fps} fps at {X} {nope}", v));
		assertEquals("open { only", CustomTextMod.fill("open { only", v));
	}

	@Test
	void customImage() {
		assertTrue(CustomImageMod.isPng(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0}));
		assertFalse(CustomImageMod.isPng(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
		assertArrayEquals(new int[] {64, 32}, CustomImageMod.fit(200, 100, 64));
		assertArrayEquals(new int[] {16, 64}, CustomImageMod.fit(50, 200, 64));
	}

	@Test
	void shapes() {
		assertEquals(10, HudShapes.halfWidth(10, 10, 0), 1e-9);
		assertEquals(0, HudShapes.halfWidth(10, 10, 10), 1e-9);
		assertEquals(8, HudShapes.halfWidth(10, 10, 6), 1e-9);
	}

	@Test
	void biome() {
		assertEquals(BiomeMod.Category.SNOWY, BiomeMod.category("frozen_ocean", false, false, true));
		assertEquals(BiomeMod.Category.WATER, BiomeMod.category("warm_ocean", false, false, true));
		assertEquals(BiomeMod.Category.FOREST, BiomeMod.category("dark_forest", false, false, false));
		assertEquals(BiomeMod.Category.DRY, BiomeMod.category("eroded_badlands", false, false, false));
		assertEquals(BiomeMod.Category.NETHER, BiomeMod.category("crimson_forest", true, false, false));
		assertEquals(BiomeMod.Category.OTHER, BiomeMod.category("plains", false, false, false));
		assertEquals("Dark Forest", BiomeMod.pretty("dark_forest"));
	}

	@Test
	void blockAbove() {
		assertEquals(1.2, BlockAboveMod.gapTo(64.8, 66, 0, 3), 1e-9);
		assertTrue(Double.isNaN(BlockAboveMod.gapTo(64.8, 70, 0, 3)));
		assertEquals(0.0, BlockAboveMod.gapTo(64.8, 64, 0.8, 3), 1e-9);
		assertTrue(Double.isNaN(BlockAboveMod.gapTo(64.8, 64, 0, 3))); // block at head height, not above
	}

	@Test
	void previewSpin() {
		assertEquals(25f, PlayerPreviewMod.bodyTurn("Fixed", 25, 45, 1000), 1e-6);
		assertEquals(90f, PlayerPreviewMod.bodyTurn("Spin", 0, 45, 2000), 1e-6);
	}
}
