package net.newgen.client.mods;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class CoordinatesMod extends HudMod {
	private final Setting.Bool showX = bool("show_x", "X coordinate", true);
	private final Setting.Bool showY = bool("show_y", "Y coordinate", true);
	private final Setting.Bool showZ = bool("show_z", "Z coordinate", true);
	private final Setting.Bool direction = bool("direction", "Direction", true);
	private final Setting.Bool biome = bool("biome", "Biome", false);
	private final Setting.Choice listMode = choice("list_mode", "List mode", "Vertical", "Vertical", "Horizontal");
	private final Setting.Num decimals = num("decimals", "Decimals", 0, 0, 3, 1);

	public CoordinatesMod() {
		super("coordinates", "Coordinates", "X, Y, Z position, facing direction and biome", 0f, 0.2f);
	}

	/** "Single line" was a switch before the list mode existed. */
	@Override
	protected void migrate(JsonObject saved) {
		super.migrate(saved);
		if (!saved.has(listMode.id) && saved.has("single_line") && saved.get("single_line").getAsBoolean()) listMode.value = "Horizontal";
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null || mc.level == null) return List.of();
		boolean vertical = listMode.value.equals("Vertical");
		int d = (int) decimals.get();
		List<String> lines = new ArrayList<>();
		if (showX.value) lines.add((vertical ? "X: " : "") + number(mc.player.getX(), d));
		if (showY.value) lines.add((vertical ? "Y: " : "") + number(mc.player.getY(), d));
		if (showZ.value) lines.add((vertical ? "Z: " : "") + number(mc.player.getZ(), d));
		if (!vertical && !lines.isEmpty()) {
			String joined = String.join(", ", lines);
			lines.clear();
			lines.add(joined);
		}
		if (direction.value) lines.add(pretty(mc.player.getDirection().getName()));
		if (biome.value) {
			lines.add(mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(key -> pretty(key.identifier().getPath())).orElse("Unknown"));
		}
		return vertical || lines.isEmpty() ? lines : List.of(String.join("  ", lines));
	}

	/** "dark_forest" -> "Dark forest". */
	private static String pretty(String id) {
		return id.isEmpty() ? id : Character.toUpperCase(id.charAt(0)) + id.substring(1).replace('_', ' ');
	}

	private static String number(double v, int decimals) {
		// With 0 decimals show the block position (floor), like F3 does, not a rounded value.
		return decimals == 0 ? String.valueOf((long) Math.floor(v)) : String.format("%." + decimals + "f", v);
	}
}
