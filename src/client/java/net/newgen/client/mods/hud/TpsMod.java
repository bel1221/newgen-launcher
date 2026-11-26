package net.newgen.client.mods.hud;

import java.util.ArrayDeque;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.newgen.client.core.Setting;

/**
 * TPS: the server's ticks per second, estimated from the world time updates. The server sends one every 20 of its
 * ticks, so at a healthy 20 TPS they arrive a second apart; slower arrivals mean a slower server.
 * ClientLevelTimeMixin calls onTimePacket for each one.
 */
public class TpsMod extends ValueHud {
	private static final ArrayDeque<Long> INTERVALS = new ArrayDeque<>();
	private static long lastPacket;
	private static ClientLevel lastLevel;

	private final Setting.Choice format = choice("tps_format", "Format", "Number and unit", "Number and unit", "Number only", "Percent");
	private final Setting.Num samples = num("tps_samples", "Average over (updates)", 5, 1, 20, 1);

	public TpsMod() {
		super("tps", "TPS", "Estimated server ticks per second, from how often the world time arrives", 0f, 0.22f, 19, 15, 20);
		colorByValue.value = true;
	}

	/** One world time update arrived (main thread). */
	public static void onTimePacket(ClientLevel level) {
		long now = System.nanoTime() / 1_000_000;
		if (level != lastLevel) {
			INTERVALS.clear();
			lastLevel = level;
		} else if (lastPacket != 0) {
			INTERVALS.add(now - lastPacket);
			while (INTERVALS.size() > 20) INTERVALS.poll();
		}
		lastPacket = now;
	}

	/**
	 * TPS from the last intervals (ms between updates) and the time since the last update. If the wait for the next
	 * update is already well past the average interval, the server is slower than that now, so the wait counts.
	 */
	static double estimate(long[] intervals, long sinceLast) {
		if (intervals.length == 0) return Double.NaN;
		double sum = 0;
		for (long i : intervals) sum += i;
		double avg = sum / intervals.length;
		if (sinceLast > avg * 1.5) avg = sinceLast; // half a wait late: not network jitter any more
		if (avg <= 0) return 20;
		return Math.max(0, Math.min(20, 20 * 1000.0 / avg));
	}

	@Override
	protected double sample(Minecraft mc) {
		if (mc.level == null || mc.level != lastLevel) return Double.NaN;
		int n = Math.min(INTERVALS.size(), (int) samples.get());
		long[] last = new long[n];
		var it = INTERVALS.descendingIterator();
		for (int i = 0; i < n; i++) last[i] = it.next();
		return estimate(last, System.nanoTime() / 1_000_000 - lastPacket);
	}

	@Override
	protected String text(double value, Minecraft mc) {
		return switch (format.value) {
			case "Number only" -> number(value);
			case "Percent" -> Math.round(value * 5) + "%";
			default -> number(value) + " TPS";
		};
	}

	@Override
	protected String number(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}
}
