package net.newgen.client.mods.hud;

import java.time.LocalTime;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Analog Clock: a round clock face with hands, showing real time or the game's time of day. */
public class AnalogClockMod extends HudMod {
	private final Setting.Choice source = radio("clock_source", "Time", "Real time", "Real time", "Game time");
	private final Setting.Num diameter = num("clock_size", "Size", 48, 20, 160, 2);
	private final Setting.Bool secondsHand = bool("seconds_hand", "Seconds hand", true);
	private final Setting.Bool marks = bool("hour_marks", "Hour marks", true);
	private final Setting.Color faceColor = color("face_color", "Face colour", 0xC0101010);
	private final Setting.Color rimColor = color("rim_color", "Rim colour", 0xFFFFFFFF);
	private final Setting.Color hourColor = color("hour_hand_color", "Hour hand colour", 0xFFFFFFFF);
	private final Setting.Color minuteColor = color("minute_hand_color", "Minute hand colour", 0xFFDDDDDD);
	private final Setting.Color secondColor = color("second_hand_color", "Seconds hand colour", 0xFFFF5555);

	public AnalogClockMod() {
		super("analog_clock", "Analog Clock", "A round clock face with hands, real or game time", 1f, 0.12f);
		background.value = false;
	}

	/** Hand angles in degrees clockwise from 12 for a time of day given in seconds: {hour, minute, second}. */
	static double[] handAngles(double secondsOfDay) {
		double s = ((secondsOfDay % 43200) + 43200) % 43200;
		return new double[] {s / 43200 * 360, s % 3600 / 3600 * 360, s % 60 / 60 * 360};
	}

	/** Game ticks of the day (0 = 6:00) as seconds of a 24 h day. */
	static double gameSeconds(long dayTime) {
		double hours = (Math.floorMod(dayTime, 24000L) / 1000.0 + 6) % 24;
		return hours * 3600;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		int d = (int) diameter.get();
		return new int[] {d, d};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		int d = (int) diameter.get();
		int t = Math.max(1, d / 32);
		HudShapes.disc(g, 0, 0, d, d, faceColor.current());
		HudShapes.ring(g, 0, 0, d, d, t, rimColor.current());
		double c = d / 2.0, r = c - t;
		if (marks.value) {
			for (int h = 0; h < 12; h++) {
				double a = Math.toRadians(h * 30);
				double inner = h % 3 == 0 ? r * 0.75 : r * 0.85;
				HudShapes.line(g, c + Math.sin(a) * inner, c - Math.cos(a) * inner, c + Math.sin(a) * (r - 1), c - Math.cos(a) * (r - 1), t, rimColor.current());
			}
		}
		double seconds;
		if (source.value.equals("Game time")) {
			if (mc.level == null) return;
			seconds = gameSeconds(mc.level.getLevelData().getDayTime());
		} else {
			seconds = LocalTime.now().toNanoOfDay() / 1e9;
		}
		double[] a = handAngles(seconds);
		hand(g, c, a[0], r * 0.5, t + 1, hourColor.current());
		hand(g, c, a[1], r * 0.8, t, minuteColor.current());
		if (secondsHand.value && source.value.equals("Real time")) hand(g, c, a[2], r * 0.9, Math.max(1, t / 2), secondColor.current());
		int dot = t + 2;
		g.fill((int) Math.round(c - dot / 2.0), (int) Math.round(c - dot / 2.0), (int) Math.round(c - dot / 2.0) + dot, (int) Math.round(c - dot / 2.0) + dot,
				hourColor.current());
	}

	private static void hand(GuiGraphics g, double c, double degrees, double length, int thickness, int color) {
		double a = Math.toRadians(degrees);
		HudShapes.line(g, c, c, c + Math.sin(a) * length, c - Math.cos(a) * length, thickness, color);
	}
}
