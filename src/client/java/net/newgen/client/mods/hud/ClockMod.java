package net.newgen.client.mods.hud;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ClockMod extends HudMod {
	private final Setting.Choice format = choice("format", "Format", "24 hour", "24 hour", "12 hour");
	private final Setting.Bool seconds = bool("seconds", "Show seconds", false);
	private final Setting.Bool date = bool("date", "Show date", false);
	private final Setting.Choice dateFormat = choice("date_format", "Date format", "dd/MM/yyyy",
			"dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "d MMM yyyy", "EEE d MMM");
	private final Setting.Bool sameLine = bool("date_same_line", "Date on the same line", false);

	public ClockMod() {
		super("clock", "Clock", "Real-world time", 1f, 0f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		String s = seconds.value ? ":ss" : "";
		String pattern = format.value.equals("12 hour") ? "h:mm" + s + " a" : "HH:mm" + s;
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
		if (!date.value) return List.of(time);
		String day = formatDate(LocalDate.now(), dateFormat.value);
		if (sameLine.value) return List.of(day + "  " + time);
		List<String> out = new ArrayList<>();
		out.add(time);
		out.add(day);
		return out;
	}

	static String formatDate(LocalDate date, String pattern) {
		return date.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
	}
}
