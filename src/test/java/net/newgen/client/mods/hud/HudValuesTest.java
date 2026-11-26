package net.newgen.client.mods.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudValuesTest {
	@Test
	void romanNumerals() {
		assertEquals("I", HudValues.roman(1));
		assertEquals("IV", HudValues.roman(4));
		assertEquals("IX", HudValues.roman(9));
		assertEquals("XIV", HudValues.roman(14));
		assertEquals("CCLV", HudValues.roman(255));
		assertEquals("0", HudValues.roman(0));
	}

	@Test
	void gradeLowerIsBetter() {
		// Ping: good at or below 80, bad at or above 150.
		assertEquals(0, HudValues.grade(40, 80, 150));
		assertEquals(0, HudValues.grade(80, 80, 150));
		assertEquals(1, HudValues.grade(100, 80, 150));
		assertEquals(2, HudValues.grade(150, 80, 150));
	}

	@Test
	void gradeHigherIsBetter() {
		// FPS: good at or above 60, bad at or below 30.
		assertEquals(0, HudValues.grade(144, 60, 30));
		assertEquals(1, HudValues.grade(45, 60, 30));
		assertEquals(2, HudValues.grade(20, 60, 30));
	}

	@Test
	void blendAndFade() {
		assertEquals(0xFF00FF00, HudValues.blend(0xFF00FF00, 0xFFFF0000, 0));
		assertEquals(0xFFFF0000, HudValues.blend(0xFF00FF00, 0xFFFF0000, 1));
		assertEquals(0xFF808080, HudValues.blend(0xFF000000, 0xFFFFFFFF, 0.5));
		assertEquals(0x80FFFFFF, HudValues.fade(0xFFFFFFFF, 128 / 255.0));
	}

	@Test
	void spikesKeepTheWindow() {
		HudValues.Spikes s = new HudValues.Spikes();
		s.add(0, 50, 1000);
		s.add(500, 10, 1000);
		s.add(900, 30, 1000);
		assertEquals(10, s.min());
		assertEquals(50, s.max());
		s.add(1600, 20, 1000); // drops the first two
		assertEquals(20, s.min());
		assertEquals(30, s.max());
		assertEquals(25, s.average());
	}

	@Test
	void marqueePausesThenRuns() {
		assertEquals(0, HudValues.marquee(500, 100, 50, 1000));
		assertEquals(25, HudValues.marquee(1500, 100, 50, 1000));
		int last = HudValues.marquee(2999, 100, 50, 1000);
		assertTrue(last <= 100 && last >= 99);
		assertEquals(0, HudValues.marquee(3000, 100, 50, 1000));
	}

	@Test
	void pingBars() {
		assertEquals("|||||", PingMod.bars(40));
		assertEquals("|||..", PingMod.bars(450));
		assertEquals("|....", PingMod.bars(2000));
	}

	@Test
	void clockDateFormats() {
		java.time.LocalDate d = java.time.LocalDate.of(2026, 3, 7);
		assertEquals("07/03/2026", ClockMod.formatDate(d, "dd/MM/yyyy"));
		assertEquals("2026-03-07", ClockMod.formatDate(d, "yyyy-MM-dd"));
		assertEquals("Sat 7 Mar", ClockMod.formatDate(d, "EEE d MMM"));
	}

	/** The lang files must stay valid JSON after keys are appended. */
	@Test
	void langFilesParse() throws Exception {
		for (String lang : new String[] {"en_us", "vi_vn"}) {
			String text = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/assets/newgen_client/lang/" + lang + ".json"));
			assertTrue(com.google.gson.JsonParser.parseString(text).isJsonObject(), lang);
		}
	}
}
