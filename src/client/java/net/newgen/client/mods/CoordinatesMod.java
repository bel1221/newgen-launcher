package net.newgen.client.mods;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;
import net.newgen.client.mods.hud.HudText;
import net.newgen.client.mods.hud.HudText.Part;

public class CoordinatesMod extends HudMod {
	private final Setting.Bool showX = bool("show_x", "X coordinate", true);
	private final Setting.Bool showY = bool("show_y", "Y coordinate", true);
	private final Setting.Bool showZ = bool("show_z", "Z coordinate", true);
	private final Setting.Bool direction = bool("direction", "Direction", true);
	private final Setting.Bool biome = bool("biome", "Biome", false);
	private final Setting.Choice listMode = choice("list_mode", "List mode", "Vertical", "Vertical", "Horizontal");
	private final Setting.Num decimals = num("decimals", "Decimals", 0, 0, 3, 1);
	private final Setting.Bool facingAxis = bool("facing_axis", "Facing as +X/-Z", false);
	private final Setting.Bool dimension = bool("dimension", "Dimension", false);
	private final Setting.Bool compact = bool("compact", "Compact one line", false);
	private final Setting.Bool axisColors = bool("axis_colors", "Colour each axis", false);
	private final Setting.Color xColor = color("x_color", "X colour", 0xFFFF6B6B);
	private final Setting.Color yColor = color("y_color", "Y colour", 0xFF6BFF8A);
	private final Setting.Color zColor = color("z_color", "Z colour", 0xFF6BA8FF);

	public CoordinatesMod() {
		super("coordinates", "Coordinates", "X, Y, Z position, facing direction and biome", 0f, 0.45f);
	}

	/** "Single line" was a switch before the list mode existed. */
	@Override
	protected void migrate(JsonObject saved) {
		super.migrate(saved);
		if (!saved.has(listMode.id) && saved.has("single_line") && saved.get("single_line").getAsBoolean()) listMode.value = "Horizontal";
	}

	/** The rows as coloured parts. */
	private List<List<Part>> rows(Minecraft mc) {
		List<List<Part>> rows = new ArrayList<>();
		if (mc.player == null || mc.level == null) return rows;
		boolean vertical = listMode.value.equals("Vertical") && !compact.value;
		int d = (int) decimals.get();
		int ink = textColor.current();
		boolean colors = axisColors.value;
		List<Part> axes = new ArrayList<>();
		double[] values = {mc.player.getX(), mc.player.getY(), mc.player.getZ()};
		boolean[] shown = {showX.value, showY.value, showZ.value};
		String[] names = {"X", "Y", "Z"};
		Setting.Color[] axisColor = {xColor, yColor, zColor};
		for (int i = 0; i < 3; i++) {
			if (!shown[i]) continue;
			int c = colors ? axisColor[i].current() : ink;
			String text = (vertical ? names[i] + ": " : "") + number(values[i], d);
			if (vertical) rows.add(new ArrayList<>(List.of(new Part(text, c))));
			else {
				if (!axes.isEmpty()) axes.add(new Part(compact.value ? " " : ", ", ink));
				axes.add(new Part(text, c));
			}
		}
		if (!axes.isEmpty()) rows.add(axes);
		List<String> extra = new ArrayList<>();
		if (direction.value) {
			Direction dir = mc.player.getDirection();
			String name = compact.value ? dir.getName().substring(0, 1).toUpperCase() : pretty(dir.getName());
			extra.add(facingAxis.value ? name + " (" + axis(dir) + ")" : name);
		}
		if (biome.value) {
			extra.add(mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(key -> pretty(key.identifier().getPath())).orElse("Unknown"));
		}
		if (dimension.value) extra.add(pretty(mc.level.dimension().identifier().getPath()));
		for (String e : extra) rows.add(new ArrayList<>(List.of(new Part(e, ink))));
		if (vertical || rows.size() <= 1) return rows;
		// Horizontal / compact: everything on one line.
		List<Part> one = new ArrayList<>();
		for (List<Part> r : rows) {
			if (!one.isEmpty()) one.add(new Part(compact.value ? " " : "  ", ink));
			one.addAll(r);
		}
		return List.of(one);
	}

	/** North -> "-Z", east -> "+X". */
	static String axis(Direction dir) {
		return (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? "+" : "-") + dir.getAxis().getName().toUpperCase();
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		List<String> out = new ArrayList<>();
		for (List<Part> r : rows(mc)) {
			StringBuilder b = new StringBuilder();
			for (Part p : r) b.append(p.text());
			out.add(b.toString());
		}
		return out;
	}

	private List<Component> coloured(Minecraft mc) {
		List<Component> out = new ArrayList<>();
		for (List<Part> r : rows(mc)) out.add(HudText.line(this, r));
		return out;
	}

	/** Plain text lines unless the axes are coloured; then the HUD draws its coloured lines itself. */
	@Override
	protected int[] size(Minecraft mc) {
		if (!axisColors.value) return null;
		List<Component> lines = coloured(mc);
		return lines.isEmpty() ? null : HudText.size(mc, lines);
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		HudText.draw(g, mc, this, coloured(mc));
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
