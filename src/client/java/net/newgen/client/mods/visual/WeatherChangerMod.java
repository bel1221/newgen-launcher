package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class WeatherChangerMod extends Mod {
	public static final WeatherChangerMod I = new WeatherChangerMod();

	public final Setting.Choice weather = choice("weather", "Weather", "Clear", "Clear", "Rain", "Thunder");
	public final Setting.Num rain = num("rain_strength", "Rain and snow intensity %", 100, 0, 100, 5);
	public final Setting.Num thunder = num("thunder_strength", "Thunder intensity %", 100, 0, 100, 5);

	private WeatherChangerMod() {
		super("weather_changer", "Weather Changer", "Shows a fixed weather on your screen only");
	}

	public float rain() {
		return weather.value.equals("Clear") ? 0 : (float) rain.get() / 100f;
	}

	public float thunder() {
		return weather.value.equals("Thunder") ? (float) thunder.get() / 100f : 0;
	}
}
