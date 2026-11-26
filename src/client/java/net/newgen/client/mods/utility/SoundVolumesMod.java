package net.newgen.client.mods.utility;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Per-sound volume: a typed list "sound.id=percent; ..." plus quick sliders for common noisy sounds. Hook: SoundEngineVolumeMixin. */
public class SoundVolumesMod extends Mod {
	public static SoundVolumesMod I;

	private final Setting.Text list = text("list", "Sound volumes (sound.id=percent; ...)", "");
	private final Setting.Num explosions = num("explosions", "Explosions (%)", 100, 0, 100, 5);
	private final Setting.Num eating = num("eating", "Eating and drinking (%)", 100, 0, 100, 5);
	private final Setting.Num anvils = num("anvils", "Anvils (%)", 100, 0, 100, 5);
	private final Setting.Num villagers = num("villagers", "Villagers (%)", 100, 0, 100, 5);

	private String parsedFrom;
	private Map<String, Float> rules = Map.of();

	public SoundVolumesMod() {
		super("sound_volumes", "Sound Volumes", "Turn single sounds up or down: explosions, eating, anvils, villagers or any sound id");
		I = this;
	}

	/** Volume multiplier (0..1) for a sound about to play or playing; 1 when the mod is off. */
	public static float factor(Identifier id) {
		SoundVolumesMod m = I;
		if (m == null || !m.enabled || id == null) return 1f;
		if (!m.list.value.equals(m.parsedFrom)) {
			m.rules = parse(m.list.value);
			m.parsedFrom = m.list.value;
		}
		return lookup(id.getNamespace(), id.getPath(), m.rules, m.explosions.get(), m.eating.get(), m.anvils.get(), m.villagers.get());
	}

	/**
	 * "entity.generic.explode=30; minecraft:block.anvil.land = 10; entity.villager.*=50" to rules (key lower-cased, value 0..1).
	 * A key ending in * matches every sound starting with the rest. Bad entries are skipped.
	 */
	static Map<String, Float> parse(String text) {
		Map<String, Float> out = new LinkedHashMap<>();
		if (text == null) return out;
		for (String part : text.split("[;,\\n]")) {
			int eq = part.indexOf('=');
			if (eq <= 0) continue;
			String key = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
			String val = part.substring(eq + 1).trim();
			if (val.endsWith("%")) val = val.substring(0, val.length() - 1).trim();
			if (key.startsWith("minecraft:")) key = key.substring("minecraft:".length());
			if (key.isEmpty()) continue;
			try {
				double pct = Double.parseDouble(val);
				out.put(key, (float) Math.max(0, Math.min(100, pct)) / 100f);
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	/** Typed rules win (exact id first, then the longest matching "prefix*"); else the quick sliders; else 1. Percent values 0..100. */
	static float lookup(String namespace, String path, Map<String, Float> rules, double explosions, double eating, double anvils, double villagers) {
		String full = namespace.equals("minecraft") ? path : namespace + ":" + path;
		Float exact = rules.get(full);
		if (exact != null) return exact;
		String best = null;
		for (String key : rules.keySet()) {
			if (key.endsWith("*") && full.startsWith(key.substring(0, key.length() - 1)) && (best == null || key.length() > best.length())) best = key;
		}
		if (best != null) return rules.get(best);
		if (!namespace.equals("minecraft")) return 1f;
		if (path.contains("explode") || path.equals("entity.firework_rocket.blast") || path.equals("entity.firework_rocket.large_blast")) return (float) explosions / 100f;
		if (path.endsWith(".eat") || path.endsWith(".drink") || path.equals("entity.player.burp")) return (float) eating / 100f;
		if (path.startsWith("block.anvil.")) return (float) anvils / 100f;
		if (path.startsWith("entity.villager.") || path.startsWith("entity.wandering_trader.")) return (float) villagers / 100f;
		return 1f;
	}
}
