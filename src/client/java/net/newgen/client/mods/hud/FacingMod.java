package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Facing: the direction you look as text (N, NE, a bearing, an axis or a word). */
public class FacingMod extends HudMod {
	private static final String[] EIGHT = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
	private static final String[] WORDS = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
	private static final String[] AXES = {"-Z", "+X", "+Z", "-X"};

	private final Setting.Choice style = choice("facing_style", "Style", "Eight-way", "Eight-way", "Four-way", "Word", "Bearing", "Axis", "All");
	private final Setting.Num decimals = num("decimals", "Decimals", 0, 0, 2, 1);

	public FacingMod() {
		super("facing", "Facing", "The direction you are facing, as text", 0f, 0.34f);
	}

	/** Compass bearing in degrees from Minecraft's yaw: north = 0, east = 90 (yaw 0 is south). */
	static double bearing(double yaw) {
		return ((yaw + 180) % 360 + 360) % 360;
	}

	/** Index into an n-way compass (0 = north, going clockwise). */
	static int sector(double yaw, int n) {
		return (int) Math.floorMod(Math.round(bearing(yaw) / (360.0 / n)), n);
	}

	static String eightWay(double yaw) {
		return EIGHT[sector(yaw, 8)];
	}

	static String fourWay(double yaw) {
		return EIGHT[sector(yaw, 4) * 2];
	}

	static String axis(double yaw) {
		return AXES[sector(yaw, 4)];
	}

	static String degrees(double yaw, int decimals) {
		return String.format(java.util.Locale.ROOT, "%." + decimals + "f°", bearing(yaw));
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		double yaw = mc.player.getYRot();
		int d = (int) decimals.get();
		return List.of(switch (style.value) {
			case "Four-way" -> fourWay(yaw);
			case "Word" -> I18n.get("newgen_client.facing." + WORDS[sector(yaw, 8)]);
			case "Bearing" -> degrees(yaw, d);
			case "Axis" -> axis(yaw);
			case "All" -> eightWay(yaw) + " " + degrees(yaw, d) + " (" + axis(yaw) + ")";
			default -> eightWay(yaw);
		});
	}
}
