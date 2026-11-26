package net.newgen.client.mods.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TabLogicTest {
	@Test
	void pingBands() {
		assertEquals(0xFFAAAAAA, TabLogic.pingColor(-1));
		assertEquals(0xFF55FF55, TabLogic.pingColor(0));
		assertEquals(0xFF55FF55, TabLogic.pingColor(74));
		assertEquals(0xFFC8FF55, TabLogic.pingColor(75));
		assertEquals(0xFFFFFF55, TabLogic.pingColor(150));
		assertEquals(0xFFFFAA00, TabLogic.pingColor(250));
		assertEquals(0xFFFF5555, TabLogic.pingColor(400));
		assertEquals(0xFFFF5555, TabLogic.pingColor(5000));
	}

	@Test
	void selfMovesToTopAndLimitApplies() {
		List<String> sorted = List.of("a", "b", "me", "c");
		assertEquals(List.of("me", "a", "b", "c"), TabLogic.arrange(sorted, "me"::equals, true, 80));
		assertEquals(List.of("a", "b", "me", "c"), TabLogic.arrange(sorted, "me"::equals, false, 80));
		assertEquals(List.of("me", "a"), TabLogic.arrange(sorted, "me"::equals, true, 2));
		assertEquals(List.of("a", "b"), TabLogic.arrange(sorted, "me"::equals, false, 2));
		assertEquals(List.of(), TabLogic.arrange(sorted, "me"::equals, false, 0));
	}
}
