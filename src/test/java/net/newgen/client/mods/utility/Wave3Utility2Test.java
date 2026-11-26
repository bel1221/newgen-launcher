package net.newgen.client.mods.utility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Pure rules of the Wave 3 utility 2 mods (no game needed). */
class Wave3Utility2Test {
	@Test
	void soundListParses() {
		Map<String, Float> rules = SoundVolumesMod.parse("entity.generic.explode=30; minecraft:block.anvil.land = 10% ;bad; x=abc; mymod:boom=150; entity.villager.*=50");
		assertEquals(0.3f, rules.get("entity.generic.explode"), 1e-6);
		assertEquals(0.1f, rules.get("block.anvil.land"), 1e-6);
		assertEquals(1f, rules.get("mymod:boom"), 1e-6);
		assertEquals(0.5f, rules.get("entity.villager.*"), 1e-6);
		assertFalse(rules.containsKey("x"));
		assertEquals(4, rules.size());
	}

	@Test
	void soundLookupOrder() {
		Map<String, Float> rules = SoundVolumesMod.parse("entity.villager.*=50; entity.villager.ambient=20");
		assertEquals(0.2f, SoundVolumesMod.lookup("minecraft", "entity.villager.ambient", rules, 100, 100, 100, 10), 1e-6);
		assertEquals(0.5f, SoundVolumesMod.lookup("minecraft", "entity.villager.trade", rules, 100, 100, 100, 10), 1e-6);
		assertEquals(0.1f, SoundVolumesMod.lookup("minecraft", "entity.wandering_trader.ambient", rules, 100, 100, 100, 10), 1e-6);
		assertEquals(0.4f, SoundVolumesMod.lookup("minecraft", "entity.generic.explode", Map.of(), 40, 100, 100, 100), 1e-6);
		assertEquals(0.25f, SoundVolumesMod.lookup("minecraft", "entity.generic.eat", Map.of(), 100, 25, 100, 100), 1e-6);
		assertEquals(0.25f, SoundVolumesMod.lookup("minecraft", "entity.player.burp", Map.of(), 100, 25, 100, 100), 1e-6);
		assertEquals(0f, SoundVolumesMod.lookup("minecraft", "block.anvil.use", Map.of(), 100, 100, 0, 100), 1e-6);
		assertEquals(1f, SoundVolumesMod.lookup("minecraft", "block.stone.break", Map.of(), 0, 0, 0, 0), 1e-6);
		assertEquals(1f, SoundVolumesMod.lookup("othermod", "big.explode", Map.of(), 0, 0, 0, 0), 1e-6);
	}

	@Test
	void uhcItemList() {
		Set<String> ids = UhcOverlayMod.parse("gold_ingot, minecraft:golden_apple;PLAYER_HEAD  mymod:thing,,");
		assertEquals(Set.of("minecraft:gold_ingot", "minecraft:golden_apple", "minecraft:player_head", "mymod:thing"), ids);
	}

	@Test
	void pingText() {
		assertEquals("[42ms]", PingNametagsMod.text(42, "[42ms]"));
		assertEquals("42ms", PingNametagsMod.text(42, "42ms"));
		assertEquals("180", PingNametagsMod.text(180, "42"));
		assertEquals(0x55FF55, PingNametagsMod.colour(20));
		assertEquals(0xFFFF55, PingNametagsMod.colour(120));
		assertEquals(0xFF5555, PingNametagsMod.colour(400));
	}

	@Test
	void killWindow() {
		assertTrue(KillSoundsMod.recent(1000, 5000, 10000));
		assertTrue(KillSoundsMod.recent(1000, 11000, 10000));
		assertFalse(KillSoundsMod.recent(1000, 11001, 10000));
		assertFalse(KillSoundsMod.recent(5000, 1000, 10000));
	}

	@Test
	void packSearch() {
		assertTrue(PackOrganizerMod.matches("", "anything"));
		assertTrue(PackOrganizerMod.matches("faith 32", "file/Faithful 32x.zip", "Faithful", ""));
		assertTrue(PackOrganizerMod.matches("do hoa", "x", "Gói Đồ Họa", ""));
		assertFalse(PackOrganizerMod.matches("faith 64", "file/Faithful 32x.zip", "Faithful", ""));
		assertTrue(PackOrganizerMod.matches("PVP", "pvp_pack", null));
	}

	@Test
	void previewFit() {
		assertArrayEquals(new int[] {400, 225}, ScreenshotManagerMod.fit(1920, 1080, 400, 400));
		assertArrayEquals(new int[] {200, 400}, ScreenshotManagerMod.fit(500, 1000, 400, 400));
		assertArrayEquals(new int[] {0, 0}, ScreenshotManagerMod.fit(0, 10, 400, 400));
	}
}
