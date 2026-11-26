package net.newgen.client.mods.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Custom Text: lines the player types (\n starts a new line), with {fps} {ping} {x} {y} {z} {time} {name} filled in. */
public class CustomTextMod extends HudMod {
	private final Setting.Text content = text("custom_text", "Text", "Newgen Client");
	private final Setting.Bool placeholders = bool("placeholders", "Fill in placeholders", true);

	public CustomTextMod() {
		super("custom_text", "Custom Text", "Your own text on the screen, with optional live values", 0.5f, 0.1f);
	}

	/** Splits on a typed "\n" or a real line break. */
	static List<String> split(String text) {
		List<String> out = new ArrayList<>();
		for (String line : text.replace("\\n", "\n").split("\n", -1)) out.add(line);
		return out;
	}

	/** Replaces each {key} found in values; unknown {words} stay as they are. */
	static String fill(String line, Map<String, String> values) {
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < line.length()) {
			int open = line.indexOf('{', i);
			int close = open < 0 ? -1 : line.indexOf('}', open);
			if (open < 0 || close < 0) {
				out.append(line, i, line.length());
				break;
			}
			String value = values.get(line.substring(open + 1, close).toLowerCase());
			out.append(line, i, open).append(value != null ? value : line.substring(open, close + 1));
			i = close + 1;
		}
		return out.toString();
	}

	private static Map<String, String> values(Minecraft mc) {
		Map<String, String> v = new HashMap<>();
		v.put("fps", String.valueOf(mc.getFps()));
		v.put("time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		if (mc.player != null) {
			v.put("x", String.valueOf(mc.player.getBlockX()));
			v.put("y", String.valueOf(mc.player.getBlockY()));
			v.put("z", String.valueOf(mc.player.getBlockZ()));
			v.put("name", mc.player.getGameProfile().name());
			PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
			v.put("ping", info == null ? "0" : String.valueOf(info.getLatency()));
		}
		return v;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (content.value.isBlank()) return List.of();
		List<String> lines = split(content.value);
		if (!placeholders.value) return lines;
		Map<String, String> v = values(mc);
		List<String> out = new ArrayList<>();
		for (String line : lines) out.add(fill(line, v));
		return out;
	}
}
