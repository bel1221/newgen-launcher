package net.newgen.client.mods.visual;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.joml.Vector4f;

public class FogMod extends Mod {
	public static final FogMod I = new FogMod();

	public final Setting.Bool distance = bool("distance", "Remove render distance fog", true);
	public final Setting.Bool environment = bool("environment", "Remove water, lava and effect fog", false);
	public final Setting.Num waterDistance = num("water_distance", "Water fog distance %", 100, 25, 1000, 25);
	public final Setting.Bool customDistance = bool("custom_distance", "Custom fog distance", false);
	public final Setting.Num start = num("start", "Fog start (blocks)", 64, 0, 1024, 8);
	public final Setting.Num end = num("end", "Fog end (blocks)", 192, 8, 1024, 8);
	public final Setting.Bool customColor = bool("custom_color", "Custom fog colour", false);
	public final Setting.Color overworld = color("overworld_color", "Overworld fog colour", 0xFFB0C8FF);
	public final Setting.Color nether = color("nether_color", "Nether fog colour", 0xFF501010);
	public final Setting.Color end_ = color("end_color", "End fog colour", 0xFF180C20);
	public final Setting.Color water = color("water_color", "Water fog colour", 0xFF103080);

	private FogMod() {
		super("fog", "Fog", "Removes fog");
	}

	/** Changes vanilla's fog for this frame; the colour's alpha is how strongly it replaces vanilla's colour. */
	public void apply(FogData fog, Vector4f color, Camera camera, ClientLevel level) {
		boolean inWater = camera.getFluidInCamera() == FogType.WATER;
		if (inWater) {
			float k = (float) waterDistance.get() / 100f;
			fog.environmentalStart *= k;
			fog.environmentalEnd *= k;
		}
		if (customDistance.value) {
			float s = (float) Math.min(start.get(), end.get() - 1);
			fog.renderDistanceStart = s;
			fog.renderDistanceEnd = (float) end.get();
		}
		if (distance.value) {
			fog.renderDistanceStart = 1.0E7f;
			fog.renderDistanceEnd = 2.0E7f;
		}
		if (environment.value) {
			fog.environmentalStart = 1.0E7f;
			fog.environmentalEnd = 2.0E7f;
		}
		if (customColor.value) {
			Setting.Color c = inWater ? water : level.dimension() == Level.NETHER ? nether : level.dimension() == Level.END ? end_ : overworld;
			int argb = c.current();
			float a = (argb >>> 24) / 255f;
			color.x += ((argb >> 16 & 0xFF) / 255f - color.x) * a;
			color.y += ((argb >> 8 & 0xFF) / 255f - color.y) * a;
			color.z += ((argb & 0xFF) / 255f - color.z) * a;
		}
	}
}
