package net.newgen.client.core;

import java.util.List;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** A mod that draws lines of text on the game screen. Subclasses only supply the text. */
public abstract class HudMod extends Mod {
	private static final int PADDING = 3;

	/** Position, 0..1: 0 = left/top edge, 1 = right/bottom edge. Keeps the element on screen at any window size. */
	public float x, y;
	/** Where the element sits before the player moves it; "reset position" in the HUD editor goes back here. */
	public final float defaultX, defaultY;

	/** True while the HUD editor is open: an element with nothing to show draws its name, so it can still be placed. */
	public static boolean editing;

	public final Setting.Num scale;
	public final Setting.Color textColor;
	public final Setting.Bool textShadow;
	public final Setting.Bool background;
	public final Setting.Color bgColor;
	public final Setting.Bool border;
	public final Setting.Color borderColor;
	private final String idPrefix;

	/** Where the element was last drawn, in screen pixels. The HUD editor uses this for dragging. */
	public int left, top, width, height;

	protected HudMod(String id, String name, String description, float x, float y) {
		this(id, name, description, x, y, "", "");
	}

	/** The prefixes keep the settings apart when the element belongs to another mod (see Mod.indicator). */
	HudMod(String id, String name, String description, float x, float y, String idPrefix, String namePrefix) {
		super(id, name, description);
		this.x = defaultX = x;
		this.y = defaultY = y;
		scale = num(idPrefix + "scale", label(namePrefix, "Scale"), 1.0, 0.5, 3.0, 0.1);
		textColor = color(idPrefix + "text_color", label(namePrefix, "Text colour"), 0xFFFFFFFF);
		textShadow = bool(idPrefix + "text_shadow", label(namePrefix, "Text shadow"), true);
		background = bool(idPrefix + "background", label(namePrefix, "Background"), true);
		bgColor = color(idPrefix + "background_color", label(namePrefix, "Background colour"), 0x80000000);
		border = bool(idPrefix + "border", label(namePrefix, "Border"), false);
		borderColor = color(idPrefix + "border_color", label(namePrefix, "Border colour"), 0xFFF6B041);
		this.idPrefix = idPrefix;
	}

	private static String label(String prefix, String name) {
		return prefix.isEmpty() ? name : prefix + name.toLowerCase();
	}

	/** The colour of the panel behind the element. */
	public int backgroundColor() {
		return bgColor.value;
	}

	/** Configs written before the colour setting existed stored a black panel with an opacity. */
	@Override
	protected void migrate(JsonObject saved) {
		super.migrate(saved);
		String old = idPrefix + "background_opacity";
		if (saved.has(old) && !saved.has(bgColor.id)) bgColor.value = (int) Math.round(saved.get(old).getAsDouble() * 255) << 24;
	}

	@Override
	public void reset() {
		super.reset();
		x = defaultX;
		y = defaultY;
	}

	protected abstract List<String> lines(Minecraft mc);

	/** Optional item icon in front of each line (same order as lines()); rows become 18 px tall. Empty = text only.
	 * Always called right after lines(), so a mod can fill both lists in lines(). */
	protected List<ItemStack> icons(Minecraft mc) {
		return List.of();
	}

	/** For HUDs that draw themselves: return {width, height} of the content and override draw(). null = text lines. */
	protected int[] size(Minecraft mc) {
		return null;
	}

	/** Draws a self-drawn HUD; (0,0) is the top-left corner of the content area given by size(). */
	protected void draw(GuiGraphics g, Minecraft mc) {
	}

	public void render(GuiGraphics g, Minecraft mc) {
		int[] custom = size(mc);
		List<String> lines = custom == null ? lines(mc) : List.of();
		List<ItemStack> icons = custom == null ? icons(mc) : List.of();
		if (custom == null && lines.isEmpty()) {
			if (!editing) return;
			lines = List.of(name);
			icons = List.of();
		}
		int row = icons.isEmpty() ? 10 : 18;
		int iconWidth = icons.isEmpty() ? 0 : 20;

		int textWidth = 0;
		for (String line : lines) textWidth = Math.max(textWidth, mc.font.width(line));
		int w = (custom != null ? custom[0] : iconWidth + textWidth) + PADDING * 2;
		int h = (custom != null ? custom[1] : lines.size() * row - (icons.isEmpty() ? 1 : 2)) + PADDING * 2;

		float s = (float) scale.get();
		width = Math.round(w * s);
		height = Math.round(h * s);
		left = Math.round(x * Math.max(0, g.guiWidth() - width));
		top = Math.round(y * Math.max(0, g.guiHeight() - height));

		g.pose().pushMatrix();
		g.pose().translate(left, top);
		g.pose().scale(s, s);
		if (background.value) g.fill(0, 0, w, h, backgroundColor());
		if (border.value) g.renderOutline(0, 0, w, h, borderColor.value);
		if (custom != null) {
			g.pose().translate(PADDING, PADDING);
			draw(g, mc);
		}
		for (int i = 0; i < lines.size(); i++) {
			if (i < icons.size() && !icons.get(i).isEmpty()) g.renderItem(icons.get(i), PADDING, PADDING + i * row);
			g.drawString(mc.font, lines.get(i), PADDING + iconWidth,PADDING + i * row + (icons.isEmpty() ? 0 : 4), textColor.value, textShadow.value);
		}
		g.pose().popMatrix();
	}
}
