package net.newgen.client.mods.controls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToggleStateTest {
	@Test
	void pressTogglesOnceUntilReleased() {
		ToggleState t = new ToggleState();
		assertFalse(t.update(false));
		assertTrue(t.update(true));
		assertTrue(t.update(true)); // still held: no second toggle
		assertTrue(t.update(false));
		assertFalse(t.update(true));
		assertFalse(t.update(false));
	}

	@Test
	void setOverridesState() {
		ToggleState t = new ToggleState();
		t.update(true);
		t.set(false);
		assertFalse(t.on());
		assertFalse(t.update(true)); // key still held from before: no new press
		assertFalse(t.update(false));
		assertTrue(t.update(true));
	}
}
