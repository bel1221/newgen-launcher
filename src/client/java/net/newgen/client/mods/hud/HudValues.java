package net.newgen.client.mods.hud;

import java.util.ArrayDeque;

/** Small pure helpers shared by the HUD mods (no Minecraft classes, so they can be unit tested). */
public final class HudValues {
	private HudValues() {
	}

	private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
	private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

	/** 1 -> "I", 4 -> "IV", 14 -> "XIV". Outside 1..3999 the plain number. */
	public static String roman(int n) {
		if (n < 1 || n > 3999) return String.valueOf(n);
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < ROMAN_VALUES.length; i++) {
			while (n >= ROMAN_VALUES[i]) {
				out.append(ROMAN_SYMBOLS[i]);
				n -= ROMAN_VALUES[i];
			}
		}
		return out.toString();
	}

	/** Mixes two ARGB colours channel by channel; t = 0 gives from, t = 1 gives to. */
	public static int blend(int from, int to, double t) {
		t = Math.max(0, Math.min(1, t));
		int out = 0;
		for (int shift = 0; shift < 32; shift += 8) {
			int a = from >>> shift & 0xFF, b = to >>> shift & 0xFF;
			out |= (int) Math.round(a + (b - a) * t) << shift;
		}
		return out;
	}

	/** The colour with its alpha multiplied by f (0..1). */
	public static int fade(int color, double f) {
		f = Math.max(0, Math.min(1, f));
		return (int) Math.round((color >>> 24) * f) << 24 | color & 0xFFFFFF;
	}

	/**
	 * Grades a value as good (0), ok (1) or bad (2). When good &lt; bad lower values are better (ping);
	 * when good &gt; bad higher values are better (FPS). Values at a threshold count as that grade.
	 */
	public static int grade(double value, double good, double bad) {
		if (good <= bad) return value <= good ? 0 : value >= bad ? 2 : 1;
		return value >= good ? 0 : value <= bad ? 2 : 1;
	}

	/** The lowest and highest value seen in the last windowMs milliseconds. */
	public static final class Spikes {
		private final ArrayDeque<double[]> samples = new ArrayDeque<>();

		public void add(long timeMs, double value, long windowMs) {
			samples.add(new double[] {timeMs, value});
			while (!samples.isEmpty() && timeMs - samples.peek()[0] > windowMs) samples.poll();
		}

		public boolean isEmpty() {
			return samples.isEmpty();
		}

		public double min() {
			double m = Double.POSITIVE_INFINITY;
			for (double[] s : samples) m = Math.min(m, s[1]);
			return m;
		}

		public double max() {
			double m = Double.NEGATIVE_INFINITY;
			for (double[] s : samples) m = Math.max(m, s[1]);
			return m;
		}

		public double average() {
			double sum = 0;
			for (double[] s : samples) sum += s[1];
			return samples.isEmpty() ? 0 : sum / samples.size();
		}

		public void clear() {
			samples.clear();
		}
	}

	/** Pixel offset of a scrolling (marquee) text: waits pauseMs at the start, then runs at speed px/s; loops over span px. */
	public static int marquee(long timeMs, int span, double speed, long pauseMs) {
		if (span <= 0) return 0;
		long runMs = (long) (span * 1000 / speed);
		long t = Math.floorMod(timeMs, runMs + pauseMs);
		return t < pauseMs ? 0 : (int) ((t - pauseMs) * speed / 1000);
	}
}
