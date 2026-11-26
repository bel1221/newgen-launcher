package net.newgen.client.mods.utility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The Chat mod's text rules (no game needed). */
class ChatModTest {
	@Test
	void timestampStyles() {
		assertEquals("[12:05] ", ChatMod.stampText("12:05", "[12:00]"));
		assertEquals("(12:05) ", ChatMod.stampText("12:05", "(12:00)"));
		assertEquals("12:05 | ", ChatMod.stampText("12:05", "12:00 |"));
		assertEquals("12:05 ", ChatMod.stampText("12:05", "12:00"));
	}

	@Test
	void shortcutsExpandOnlyAnExactName() {
		String table = "gg=Good game!; /h=/home ;bad";
		assertEquals("Good game!", ChatMod.expandShortcut("gg", table));
		assertEquals("Good game!", ChatMod.expandShortcut(" GG ", table));
		assertEquals("/home", ChatMod.expandShortcut("/h", table));
		assertEquals("gg wp", ChatMod.expandShortcut("gg wp", table));
		assertEquals("bad", ChatMod.expandShortcut("bad", table));
	}

	@Test
	void emojiShortcodes() {
		assertEquals("I ❤ this ★", ChatMod.replaceEmoji("I :heart: this :star:"));
		assertEquals("keep :unknown: as is", ChatMod.replaceEmoji("keep :unknown: as is"));
	}

	@Test
	void mentionsAreWholeWordsIgnoringCase() {
		List<String> terms = List.of("Bunn9", "boss");
		assertTrue(ChatMod.mentions("hey bunn9, come here", terms));
		assertTrue(ChatMod.mentions("where is the BOSS?", terms));
		assertFalse(ChatMod.mentions("bunn99 joined", terms));
		assertFalse(ChatMod.mentions("bossbar", terms));
	}

	@Test
	void searchMatchesIgnoringCase() {
		assertTrue(ChatMod.matches("Welcome to NewGen", "newgen"));
		assertTrue(ChatMod.matches("anything", null), "no search");
		assertTrue(ChatMod.matches("anything", ""), "empty search");
		assertFalse(ChatMod.matches("hello", "bye"));
	}

	@Test
	void chatTabs() {
		List<ChatTabs.Tab> tabs = ChatTabs.build("Trades=buy|sell; Broken=([; =x");
		assertEquals(6, tabs.size(), "4 built-in + Trades + Broken (an empty name is skipped)");
		assertTrue(tabs.get(5).invalid() && tabs.get(5).label().endsWith("(!)"), "a broken regex is a visible invalid tab");
		assertFalse(ChatTabs.belongs(tabs.get(5), "anything", true, false), "and matches nothing");
		ChatTabs.Tab server = tabs.get(1), whisper = tabs.get(2), mentions = tabs.get(3), trades = tabs.get(4);
		assertTrue(ChatTabs.belongs(server, "Server restarting soon", false, false));
		assertFalse(ChatTabs.belongs(server, "<Steve> hi", true, false));
		assertTrue(ChatTabs.belongs(whisper, "Steve whispers to you: hi", true, false));
		assertTrue(ChatTabs.belongs(whisper, "[Steve -> me] hi", true, false));
		assertTrue(ChatTabs.belongs(whisper, "From Steve: hi", true, false));
		assertFalse(ChatTabs.belongs(whisper, "<Steve> hi", true, false));
		assertTrue(ChatTabs.belongs(mentions, "anything", true, true));
		assertTrue(ChatTabs.belongs(trades, "<Alex> WTB diamonds, will BUY", true, false));
	}

	@Test
	void roundedBackgroundCoversTheBoxWithCutCorners() {
		int[][] rows = RoundedChatBackground.rows(0, 0, 100, 40, 3);
		assertEquals(7, rows.length);
		assertArrayEquals(new int[] {0, 3, 100, 37}, rows[0], "middle band");
		assertTrue(rows[1][0] > 0 && rows[1][1] == 0, "top row is inset");
		assertEquals(1, RoundedChatBackground.rows(0, 0, 100, 40, 0).length, "radius 0 = one rectangle");
	}

	@Test
	void chatScreenshotCropsTheRowsFromTheScreenshot() {
		// GUI scale 2: a 640-wide GUI on a 1280-wide frame
		assertArrayEquals(new int[] {0, 200, 600, 60}, ChatScreenshot.crop(new int[] {0, 100, 300, 130}, 1280, 720, 640));
		assertArrayEquals(new int[] {0, 700, 1280, 20}, ChatScreenshot.crop(new int[] {0, 350, 9999, 9999}, 1280, 720, 640), "clamped to the image");
	}

	@Test
	void evilRegexReturnsQuickly() {
		List<ChatTabs.Tab> tabs = ChatTabs.build("Evil=(a+)+$");
		String text = "a".repeat(40) + "b";
		long start = System.nanoTime();
		assertFalse(ChatTabs.belongs(tabs.get(4), text, true, false));
		assertTrue(System.nanoTime() - start < 200_000_000L, "gave up within the budget, not after billions of steps");
		assertTrue(ChatTabs.build("Long=" + "a".repeat(201)).get(4).invalid(), "patterns over 200 characters are refused");
	}

	@Test
	void imagePreviewRefusesLocalAddresses() {
		for (String host : new String[] {"127.0.0.1", "localhost", "10.0.0.5", "172.16.3.4", "192.168.1.10", "169.254.169.254", "[::1]", "[fe80::1]", "[fd00::1]", "0.0.0.0", "224.0.0.1", "100.64.0.1"})
			assertFalse(ImagePreview.publicHost(host), host);
		assertTrue(ImagePreview.publicHost("93.184.216.34"), "a public address literal");
		assertFalse(ImagePreview.publicHost(""), "no host");
	}

	@Test
	void newMessagesSlideInFor150Ms() {
		assertEquals(9, ChatMod.slide(0));
		assertTrue(ChatMod.slide(75) > 0 && ChatMod.slide(75) < 9);
		assertEquals(0, ChatMod.slide(150));
		assertEquals(0, ChatMod.slide(-5));
	}

	@Test
	void imagePreviewRules() throws Exception {
		assertTrue(ImagePreview.previewable("https://example.com/a.png"));
		assertTrue(ImagePreview.previewable("http://example.com/a"));
		assertFalse(ImagePreview.previewable("file:///C:/secret.png"));
		assertFalse(ImagePreview.previewable("javascript:alert(1)"));
		assertArrayEquals(new int[] {256, 128}, ImagePreview.fit(1024, 512, 256));
		assertArrayEquals(new int[] {100, 50}, ImagePreview.fit(100, 50, 256), "never enlarged");
		assertNull(ImagePreview.readCapped(new ByteArrayInputStream(new byte[2000]), 1000), "over the cap");
		assertEquals(500, ImagePreview.readCapped(new ByteArrayInputStream(new byte[500]), 1000).length);
	}
}
