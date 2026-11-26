package net.newgen.client.mods.hud;

import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/**
 * Battery: the laptop's charge, read from Windows (GetSystemPowerStatus in kernel32, through the JNA that Minecraft
 * ships). Desktops, unknown batteries and other systems show nothing.
 */
public class BatteryMod extends HudMod {
	private final Setting.Bool hideCharging = bool("hide_charging", "Hide while charging", false);
	private final Setting.Bool showCharging = bool("show_charging", "Mark charging with +", true);
	private final Setting.Bool showTime = bool("show_time_left", "Show time left", false);
	private final Setting.Num lowAt = num("low_at", "Low at (%)", 20, 5, 50, 1);
	private final Setting.Color lowColor = color("low_color", "Low colour", 0xFFFF5555);

	private long lastRead;
	private Reading reading;

	public BatteryMod() {
		super("battery", "Battery", "Your laptop's battery charge (Windows laptops only)", 1f, 0.06f);
	}

	/** What the battery says: percent 0..100, seconds left or -1 when unknown. */
	record Reading(int percent, boolean charging, int secondsLeft) {
	}

	/** Reads the SYSTEM_POWER_STATUS fields; null when there is no battery or its charge is unknown. */
	static Reading parse(int acLineStatus, int batteryFlag, int percent, int lifeTime) {
		if ((batteryFlag & 0xFF) == 255 || (batteryFlag & 128) != 0) return null; // unknown / no system battery
		if (percent < 0 || percent > 100) return null;
		boolean charging = acLineStatus == 1 || (batteryFlag & 8) != 0;
		return new Reading(percent, charging, lifeTime < 0 ? -1 : lifeTime);
	}

	/** "85%", "+85%", "85% (2:05)". */
	static String text(Reading r, boolean plus, boolean time) {
		String s = (plus && r.charging() ? "+" : "") + r.percent() + "%";
		if (time && !r.charging() && r.secondsLeft() > 0) s += String.format(" (%d:%02d)", r.secondsLeft() / 3600, r.secondsLeft() / 60 % 60);
		return s;
	}

	private Reading current() {
		long now = System.currentTimeMillis();
		if (now - lastRead > 5000) {
			lastRead = now;
			reading = Windows.read();
		}
		return reading;
	}

	private List<Component> shown() {
		Reading r = current();
		if (r == null || hideCharging.value && r.charging()) return List.of();
		int color = r.percent() <= lowAt.get() && !r.charging() ? lowColor.current() : textColor.current();
		return List.of(HudText.line(this, List.of(new HudText.Part(text(r, showCharging.value, showTime.value), color))));
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		List<Component> lines = shown();
		return lines.isEmpty() ? null : HudText.size(mc, lines);
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		HudText.draw(g, mc, this, shown());
	}

	/** The Windows call, loaded on first use; any failure (not Windows, no JNA) means "no battery" for good. */
	static final class Windows {
		private static boolean broken = !System.getProperty("os.name", "").toLowerCase().contains("windows");
		private static Kernel32 kernel;

		@Structure.FieldOrder({"ACLineStatus", "BatteryFlag", "BatteryLifePercent", "SystemStatusFlag", "BatteryLifeTime", "BatteryFullLifeTime"})
		public static class PowerStatus extends Structure {
			public byte ACLineStatus;
			public byte BatteryFlag;
			public byte BatteryLifePercent;
			public byte SystemStatusFlag;
			public int BatteryLifeTime;
			public int BatteryFullLifeTime;
		}

		interface Kernel32 extends Library {
			boolean GetSystemPowerStatus(PowerStatus status);
		}

		static Reading read() {
			if (broken) return null;
			try {
				if (kernel == null) kernel = Native.load("kernel32", Kernel32.class);
				PowerStatus s = new PowerStatus();
				if (!kernel.GetSystemPowerStatus(s)) return null;
				return parse(s.ACLineStatus & 0xFF, s.BatteryFlag & 0xFF, s.BatteryLifePercent & 0xFF, s.BatteryLifeTime);
			} catch (Throwable t) {
				broken = true;
				return null;
			}
		}
	}
}
