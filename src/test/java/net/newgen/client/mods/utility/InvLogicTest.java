package net.newgen.client.mods.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

// Wave 3 utility 1
/** The pure rules of the inventory mods (no game needed). */
class InvLogicTest {
	@Test
	void lockedSlotsRoundTrip() {
		Set<Integer> s = InvLogic.parseInts(" 5,3;40 x 41 -1,3", 40);
		assertEquals(Set.of(3, 5, 40), s);
		assertEquals("3,5,40", InvLogic.formatInts(s));
		assertTrue(InvLogic.parseInts("", 40).isEmpty());
	}

	@Test
	void pairsParseAndBind() {
		Map<Integer, Integer> p = InvLogic.parsePairs("0:12, 3:30, 9:20, 2:5, bad, 4:12");
		assertEquals(Map.of(3, 30, 4, 12), p);
		Map<Integer, Integer> q = new LinkedHashMap<>(p);
		assertTrue(InvLogic.bind(q, 30, 1)); // inventory first, hotbar second; 30 leaves hotbar 3
		assertEquals(Map.of(1, 30, 4, 12), q);
		assertFalse(InvLogic.bind(q, 1, 2)); // two hotbar slots
		assertFalse(InvLogic.bind(q, 12, 13)); // two inventory slots
		assertEquals(30, InvLogic.partner(q, 1));
		assertEquals(4, InvLogic.partner(q, 12));
		assertEquals(-1, InvLogic.partner(q, 7));
		assertEquals(q, InvLogic.parsePairs(InvLogic.formatPairs(q)));
	}

	@Test
	void itemIds() {
		assertEquals(List.of("minecraft:diamond_sword", "mymod:thing"), List.copyOf(InvLogic.parseIds("Diamond_Sword,  mymod:thing;")));
	}

	@Test
	void hotbarScroll() {
		// vanilla: wheel up (positive) goes one slot left, wrapping
		assertEquals(8, InvLogic.nextHotbar(0, 1, 9, false, true));
		assertEquals(0, InvLogic.nextHotbar(8, -1, 9, false, true));
		assertEquals(1, InvLogic.nextHotbar(0, 1, 9, true, true));
		assertEquals(0, InvLogic.nextHotbar(0, 1, 9, false, false));
		assertEquals(8, InvLogic.nextHotbar(8, -2.5, 9, false, false));
		assertEquals(5, InvLogic.nextHotbar(4, -1, 9, false, false));
	}

	@Test
	void enchantLabels() {
		assertEquals("S5", InvLogic.enchantLabel("sharpness", 5, true));
		assertEquals("M", InvLogic.enchantLabel("mending", 1, true));
		assertEquals("P", InvLogic.enchantLabel("protection", 4, false));
		assertEquals("NE2", InvLogic.enchantLabel("new_enchant", 2, true));
	}

	@Test
	void secondPressNeedsReleaseAndTime() {
		assertTrue(InvLogic.secondPress(1000, 1300, 400, true));
		assertFalse(InvLogic.secondPress(1000, 1300, 400, false)); // key held: key repeat
		assertFalse(InvLogic.secondPress(1000, 1500, 400, true)); // too late
		assertFalse(InvLogic.secondPress(0, 300, 400, true)); // nothing blocked yet
	}
}
