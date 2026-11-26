package net.newgen.client.mods.hud;

import java.util.List;
import java.util.Locale;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Speed: how fast you move (horizontal, 3D or per axis), measured from your position each tick. */
public class SpeedMod extends HudMod {
	private final Setting.Choice measure = choice("speed_measure", "Measure", "Horizontal", "Horizontal", "3D", "Per axis");
	private final Setting.Choice unit = radio("speed_unit", "Unit", "Blocks/s", "Blocks/s", "m/s", "km/h");
	private final Setting.Num smoothing = num("smoothing", "Smoothing", 0.5, 0, 0.95, 0.05);
	private final Setting.Num decimals = num("decimals", "Decimals", 2, 0, 3, 1);
	private final Setting.Bool showUnit = bool("show_unit", "Show unit", true);

	private Vec3 last;
	private double vx, vy, vz;

	public SpeedMod() {
		super("speed", "Speed", "How fast you are moving: horizontal, 3D or per axis", 0f, 0.3f);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		if (!enabled || mc.player == null) {
			last = null;
			vx = vy = vz = 0;
			return;
		}
		Vec3 p = mc.player.position();
		if (last != null) {
			double dx = (p.x - last.x) * 20, dy = (p.y - last.y) * 20, dz = (p.z - last.z) * 20;
			if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 2000) dx = dy = dz = 0; // a teleport, not movement
			double a = smoothing.get();
			vx = smooth(vx, dx, a);
			vy = smooth(vy, dy, a);
			vz = smooth(vz, dz, a);
		}
		last = p;
	}

	/** Exponential smoothing: a = 0 takes the new value, a near 1 changes slowly. */
	static double smooth(double old, double now, double a) {
		return old * a + now * (1 - a);
	}

	/** Blocks (metres) per second in the unit, with the decimals; the unit text when showUnit. */
	static String format(double blocksPerSecond, String unit, int decimals, boolean showUnit) {
		double v = unit.equals("km/h") ? blocksPerSecond * 3.6 : blocksPerSecond;
		String n = String.format(Locale.ROOT, "%." + decimals + "f", v);
		if (n.startsWith("-") && Double.parseDouble(n) == 0) n = n.substring(1);
		if (!showUnit) return n;
		return n + " " + (unit.equals("Blocks/s") ? "b/s" : unit);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		int d = (int) decimals.get();
		return switch (measure.value) {
			case "3D" -> List.of(format(Math.sqrt(vx * vx + vy * vy + vz * vz), unit.value, d, showUnit.value));
			case "Per axis" -> List.of("X " + format(vx, unit.value, d, showUnit.value), "Y " + format(vy, unit.value, d, showUnit.value),
					"Z " + format(vz, unit.value, d, showUnit.value));
			default -> List.of(format(Math.hypot(vx, vz), unit.value, d, showUnit.value));
		};
	}
}
