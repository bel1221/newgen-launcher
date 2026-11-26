package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/**
 * A HUD that shows one measured number (ping, FPS, memory): update rate, spikes (lowest/highest over the last
 * seconds) and colour by value (good / ok / bad thresholds). Subclasses measure and format the number.
 */
public abstract class ValueHud extends HudMod {
	protected final Setting.Num updateRate;
	protected final Setting.Bool spikes;
	protected final Setting.Num spikeSeconds;
	protected final Setting.Bool colorByValue;
	protected final Setting.Num goodAt, badAt;
	protected final Setting.Color goodColor, okColor, badColor;

	private final HudValues.Spikes window = new HudValues.Spikes();
	private long lastUpdate;
	private String shown;
	private double value = Double.NaN;

	/** good/bad: thresholds in the unit of sample(); good &lt; bad means lower is better. */
	protected ValueHud(String id, String name, String description, float x, float y, double good, double bad, double max) {
		super(id, name, description, x, y);
		updateRate = num("update_rate", "Update every (ms)", 250, 0, 2000, 50);
		spikes = bool("spikes", "Show spikes (lowest-highest)", false);
		spikeSeconds = num("spike_seconds", "Spike window (seconds)", 10, 1, 60, 1);
		colorByValue = bool("color_by_value", "Colour by value", false);
		goodAt = num("good_at", "Good at", good, 0, max, 1);
		badAt = num("bad_at", "Bad at", bad, 0, max, 1);
		goodColor = color("good_color", "Good colour", 0xFF55FF55);
		okColor = color("ok_color", "OK colour", 0xFFFFFF55);
		badColor = color("bad_color", "Bad colour", 0xFFFF5555);
	}

	/** The measured value now; NaN = nothing to show. */
	protected abstract double sample(Minecraft mc);

	/** The text for the value, in the chosen display mode. */
	protected abstract String text(double value, Minecraft mc);

	/** A number in the spikes part. */
	protected String number(double v) {
		return String.valueOf(Math.round(v));
	}

	private void update(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (shown != null && now - lastUpdate < updateRate.get()) return;
		lastUpdate = now;
		double v = sample(mc);
		if (Double.isNaN(v)) {
			shown = null;
			window.clear();
			return;
		}
		value = v;
		window.add(now, v, (long) (spikeSeconds.get() * 1000));
		String text = text(v, mc);
		if (spikes.value && !window.isEmpty()) text += " (" + number(window.min()) + "-" + number(window.max()) + ")";
		shown = text;
	}

	/** Recent values (for averages); same window as spikes. */
	protected double average() {
		return window.average();
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		update(mc);
		return shown == null ? List.of() : List.of(shown);
	}

	private int valueColor() {
		return switch (HudValues.grade(value, goodAt.get(), badAt.get())) {
			case 0 -> goodColor.current();
			case 1 -> okColor.current();
			default -> badColor.current();
		};
	}

	private List<Component> coloured(Minecraft mc) {
		update(mc);
		if (shown == null) return List.of();
		return List.of(HudText.line(this, List.of(new HudText.Part(shown, valueColor()))));
	}

	@Override
	protected int[] size(Minecraft mc) {
		if (!colorByValue.value) return null;
		List<Component> lines = coloured(mc);
		return lines.isEmpty() ? null : HudText.size(mc, lines);
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		HudText.draw(g, mc, this, coloured(mc));
	}
}
