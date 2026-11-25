package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class WeatherChangerMod extends Mod {
	public static final WeatherChangerMod I = new WeatherChangerMod();
	public final Setting.Choice weather = choice("weather", "Weather", "Clear", "Clear", "Rain", "Thunder");

	private WeatherChangerMod() {
		super("weather_changer", "Weather Changer", "Shows a fixed weather on your screen only");
	}

	public float rain() {
		return weather.value.equals("Clear") ? 0 : 1;
	}

	public float thunder() {
		return weather.value.equals("Thunder") ? 1 : 0;
	}
}
