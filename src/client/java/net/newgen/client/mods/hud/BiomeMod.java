package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Biome: the name of the biome you stand in, optionally coloured by its kind (water, snowy, forest...). */
public class BiomeMod extends HudMod {
	private final Setting.Bool colorByCategory = bool("color_by_category", "Colour by biome type", false);
	private final Setting.Color water = color("water_color", "Water colour", 0xFF5599FF);
	private final Setting.Color snowy = color("snowy_color", "Snowy colour", 0xFFDDF4FF);
	private final Setting.Color forest = color("forest_color", "Forest colour", 0xFF55CC55);
	private final Setting.Color dry = color("dry_color", "Dry colour", 0xFFE8C16A);
	private final Setting.Color cave = color("cave_color", "Cave colour", 0xFFAAAAAA);
	private final Setting.Color nether = color("nether_color", "Nether colour", 0xFFFF6644);
	private final Setting.Color end = color("end_color", "End colour", 0xFFCC88FF);

	public BiomeMod() {
		super("biome", "Biome", "The name of the biome you are in", 0f, 0.38f);
	}

	enum Category { NETHER, END, WATER, SNOWY, FOREST, DRY, CAVE, OTHER }

	/** The kind of a biome by its tags (nether, end, ocean) or else its id path. */
	static Category category(String path, boolean isNether, boolean isEnd, boolean isOcean) {
		if (isNether) return Category.NETHER;
		if (isEnd) return Category.END;
		if (path.contains("frozen") || path.contains("snow") || path.contains("ice") || path.contains("grove") || path.contains("jagged")) return Category.SNOWY;
		if (isOcean || path.contains("river") || path.contains("beach") || path.contains("swamp")) return Category.WATER;
		if (path.contains("cave") || path.contains("deep_dark")) return Category.CAVE;
		if (path.contains("desert") || path.contains("badlands") || path.contains("savanna")) return Category.DRY;
		if (path.contains("forest") || path.contains("jungle") || path.contains("taiga") || path.contains("woodland")) return Category.FOREST;
		return Category.OTHER;
	}

	/** "dark_forest" -> "Dark Forest", for biomes from data packs without a translation. */
	static String pretty(String path) {
		StringBuilder out = new StringBuilder();
		for (String word : path.substring(path.lastIndexOf('/') + 1).split("_")) {
			if (word.isEmpty()) continue;
			if (!out.isEmpty()) out.append(' ');
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.toString();
	}

	private List<Component> shown(Minecraft mc) {
		if (mc.player == null || mc.level == null) return List.of();
		Holder<Biome> biome = mc.level.getBiome(mc.player.blockPosition());
		Identifier id = biome.unwrapKey().map(k -> k.identifier()).orElse(null);
		if (id == null) return List.of();
		String key = "biome." + id.getNamespace() + "." + id.getPath();
		String name = I18n.exists(key) ? I18n.get(key) : pretty(id.getPath());
		int color = textColor.current();
		if (colorByCategory.value) {
			color = switch (category(id.getPath(), biome.is(BiomeTags.IS_NETHER), biome.is(BiomeTags.IS_END), biome.is(BiomeTags.IS_OCEAN))) {
				case NETHER -> nether.current();
				case END -> end.current();
				case WATER -> water.current();
				case SNOWY -> snowy.current();
				case FOREST -> forest.current();
				case DRY -> dry.current();
				case CAVE -> cave.current();
				case OTHER -> textColor.current();
			};
		}
		return List.of(HudText.line(this, List.of(new HudText.Part(name, color))));
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		List<Component> lines = shown(mc);
		return lines.isEmpty() ? null : HudText.size(mc, lines);
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		HudText.draw(g, mc, this, shown(mc));
	}
}
