package net.newgen.client.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

/** The Wave 1A setting types save and load, and configs written before them still load. */
class SettingJsonTest {
	@Test
	void plainColourKeepsTheOldFormat() {
		Setting.Color c = new Setting.Color("c", "C", 0xFF112233);
		assertEquals("#FF112233", c.toJson().getAsString());
		c.fromJson(new JsonPrimitive("#80ABCDEF"));
		assertEquals(0x80ABCDEF, c.value);
		assertFalse(c.chroma);
	}

	@Test
	void chromaColourRoundTrips() {
		Setting.Color c = new Setting.Color("c", "C", 0xFF112233);
		c.chroma = true;
		c.chromaSpeed = 30;
		Setting.Color d = new Setting.Color("c", "C", 0);
		d.fromJson(c.toJson());
		assertTrue(d.chroma);
		assertEquals(30, d.chromaSpeed);
		assertEquals(0xFF112233, d.value);
	}

	@Test
	void orderKeepsSavedOrderAndAddsNewItems() {
		Setting.Order o = new Setting.Order("o", "O", "a", "b", "c");
		Setting.Order saved = new Setting.Order("o", "O", "a", "b");
		saved.move(1, 0); // b, a
		o.fromJson(saved.toJson());
		assertEquals(List.of("b", "a", "c"), o.items);
	}

	@Test
	void rangeAndMultiLoad() {
		Setting.Range r = new Setting.Range("r", "R", 2, 8, 0, 10, 1);
		r.set(9, 3);
		assertEquals(3, r.lo);
		assertEquals(9, r.hi);
		Setting.Multi m = new Setting.Multi("m", "M", new String[]{"Bold", "Italic"});
		m.fromJson(new Setting.Multi("m", "M", new String[]{"Bold", "Italic", "Gone"}, "Italic", "Gone").toJson());
		assertEquals(List.of("Italic"), List.copyOf(m.selected));
	}
}
