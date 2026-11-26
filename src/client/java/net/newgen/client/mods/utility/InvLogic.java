package net.newgen.client.mods.utility;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// Wave 3 utility 1
/** Pure rules behind the inventory mods (no game classes, unit-tested): saved lists, hotbar scroll, enchant labels. */
public final class InvLogic {
	private InvLogic() {
	}

	/** "3, 5,40" -> {3, 5, 40}; bad entries and numbers outside 0..max are skipped. */
	public static Set<Integer> parseInts(String text, int max) {
		Set<Integer> out = new TreeSet<>();
		if (text == null) return out;
		for (String part : text.split("[,;\\s]+")) {
			try {
				int v = Integer.parseInt(part.trim());
				if (v >= 0 && v <= max) out.add(v);
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	public static String formatInts(Collection<Integer> values) {
		StringBuilder b = new StringBuilder();
		for (int v : new TreeSet<>(values)) {
			if (b.length() > 0) b.append(',');
			b.append(v);
		}
		return b.toString();
	}

	/** "0:12,3:30" -> {0=12, 3=30}: hotbar slot (0..8) to main inventory slot (9..35). Later duplicates win. */
	public static Map<Integer, Integer> parsePairs(String text) {
		Map<Integer, Integer> out = new LinkedHashMap<>();
		if (text == null) return out;
		for (String part : text.split("[,;\\s]+")) {
			String[] ab = part.split(":");
			if (ab.length != 2) continue;
			try {
				int h = Integer.parseInt(ab[0].trim()), inv = Integer.parseInt(ab[1].trim());
				if (h < 0 || h > 8 || inv < 9 || inv > 35) continue;
				out.values().remove(inv);
				out.put(h, inv);
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	public static String formatPairs(Map<Integer, Integer> pairs) {
		StringBuilder b = new StringBuilder();
		for (Map.Entry<Integer, Integer> e : pairs.entrySet()) {
			if (b.length() > 0) b.append(',');
			b.append(e.getKey()).append(':').append(e.getValue());
		}
		return b.toString();
	}

	/** Pairs a with b (one hotbar slot, one main inventory slot, either order), dropping older pairs of either.
	 * Returns false (and changes nothing) when the two slots can't be paired. */
	public static boolean bind(Map<Integer, Integer> pairs, int a, int b) {
		int h = a <= 8 ? a : b, inv = a <= 8 ? b : a;
		if (h < 0 || h > 8 || inv < 9 || inv > 35) return false;
		pairs.remove(h);
		pairs.values().remove(inv);
		pairs.put(h, inv);
		return true;
	}

	/** The partner of an inventory index, or -1. */
	public static int partner(Map<Integer, Integer> pairs, int index) {
		if (pairs.containsKey(index)) return pairs.get(index);
		for (Map.Entry<Integer, Integer> e : pairs.entrySet()) if (e.getValue() == index) return e.getKey();
		return -1;
	}

	/** "diamond_sword, minecraft:totem_of_undying" -> full lower-case ids; no namespace means "minecraft:". */
	public static Set<String> parseIds(String text) {
		Set<String> out = new LinkedHashSet<>();
		if (text == null) return out;
		for (String part : text.split("[,;\\s]+")) {
			String id = part.trim().toLowerCase(Locale.ROOT);
			if (id.isEmpty()) continue;
			out.add(id.contains(":") ? id : "minecraft:" + id);
		}
		return out;
	}

	/**
	 * The hotbar slot after one wheel step. scroll > 0 is wheel up (vanilla: one slot left). invert flips that;
	 * without wrap the selection stops at the first and last slot.
	 */
	public static int nextHotbar(int current, double scroll, int size, boolean invert, boolean wrap) {
		int step = (int) Math.signum(scroll);
		if (invert) step = -step;
		int next = current - step;
		if (wrap) return Math.floorMod(next, size);
		return Math.max(0, Math.min(size - 1, next));
	}

	private static final Map<String, String> SHORT = Map.ofEntries(
			Map.entry("sharpness", "S"), Map.entry("smite", "Sm"), Map.entry("bane_of_arthropods", "BA"),
			Map.entry("protection", "P"), Map.entry("fire_protection", "FP"), Map.entry("blast_protection", "BP"),
			Map.entry("projectile_protection", "PP"), Map.entry("feather_falling", "FF"), Map.entry("unbreaking", "U"),
			Map.entry("efficiency", "E"), Map.entry("fortune", "F"), Map.entry("looting", "L"), Map.entry("silk_touch", "ST"),
			Map.entry("mending", "M"), Map.entry("power", "Pw"), Map.entry("punch", "Pu"), Map.entry("flame", "Fl"),
			Map.entry("infinity", "I"), Map.entry("knockback", "K"), Map.entry("fire_aspect", "FA"), Map.entry("sweeping_edge", "SE"),
			Map.entry("thorns", "T"), Map.entry("respiration", "R"), Map.entry("aqua_affinity", "AA"), Map.entry("depth_strider", "DS"),
			Map.entry("frost_walker", "FW"), Map.entry("soul_speed", "SS"), Map.entry("swift_sneak", "SwS"),
			Map.entry("luck_of_the_sea", "LS"), Map.entry("lure", "Lu"), Map.entry("loyalty", "Ly"), Map.entry("riptide", "Rt"),
			Map.entry("channeling", "Ch"), Map.entry("impaling", "Im"), Map.entry("multishot", "MS"), Map.entry("piercing", "Pi"),
			Map.entry("quick_charge", "QC"), Map.entry("density", "D"), Map.entry("breach", "B"), Map.entry("wind_burst", "WB"),
			Map.entry("lunge", "Lg"), Map.entry("binding_curse", "CB"), Map.entry("vanishing_curse", "CV"));

	/** Short label of an enchantment by id path: "sharpness", 5 -> "S5"; unknown ones use their word initials. */
	public static String enchantLabel(String path, int level, boolean showLevel) {
		String s = SHORT.get(path);
		if (s == null) {
			StringBuilder b = new StringBuilder();
			for (String w : path.split("_")) if (!w.isEmpty() && b.length() < 3) b.append(Character.toUpperCase(w.charAt(0)));
			s = b.length() == 0 ? "?" : b.toString();
		}
		return showLevel && level > 1 ? s + level : s;
	}

	/**
	 * Double-press gate of Drop Protection: a protected drop goes through only when an earlier one was blocked
	 * within windowMs and the drop key/mouse was released in between (so holding Q never gets through).
	 */
	public static boolean secondPress(long lastBlocked, long now, long windowMs, boolean releasedSince) {
		return lastBlocked != 0 && releasedSince && now - lastBlocked <= windowMs;
	}
}
