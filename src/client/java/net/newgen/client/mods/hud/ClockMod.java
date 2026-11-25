package net.newgen.client.mods.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ClockMod extends HudMod {
	private final Setting.Choice format = choice("format", "Format", "24 hour", "24 hour", "12 hour");
	private final Setting.Bool seconds = bool("seconds", "Show seconds", false);

	public ClockMod() {
		super("clock", "Clock", "Real-world time", 1f, 0f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		String s = seconds.value ? ":ss" : "";
		String pattern = format.value.equals("12 hour") ? "h:mm" + s + " a" : "HH:mm" + s;
		return List.of(LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)));
	}
}
