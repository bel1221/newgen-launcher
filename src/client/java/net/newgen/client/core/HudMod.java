package net.newgen.client.core;

import java.util.List;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** A mod that draws lines of text on the game screen. Subclasses only supply the text. */
public abstract class HudMod extends Mod {
	/** Default inner padding; the Padding option overrides it per element. */
	private static final int PADDING = 3;

	/** Position, 0..1: 0 = left/top edge, 1 = right/bottom edge. Keeps the element on screen at any window size. */
	public float x, y;
	/** Where the element sits before the player moves it; "reset position" in the HUD editor goes back here. */
	public final float defaultX, defaultY;

	/** HUD editor: a locked element cannot be moved; elements with the same group id are selected and moved together. */
	public boolean locked;
	public String group = "";

	/** True while the HUD editor is open: an element with nothing to show draws its name, so it can still be placed. */
	public static boolean editing;

	public final Setting.Num scale;
	public final Setting.Color textColor;
	public final Setting.Bool textShadow;
	public final Setting.Bool background;
	public final Setting.Color bgColor;
	public final Setting.Bool border;
	public final Setting.Color borderColor;
	// Global HUD style (Wave 1A): every HUD element has the same set, and "Apply to all" copies it across.
	public final Setting.Bool brackets, label;
	public final Setting.Color bracketColor, labelColor, shadowColor;
	public final Setting.Text labelText, suffix;
	public final Setting.Choice align;
	public final Setting.Num staticWidth, staticHeight, borderWidth, radius, padding, shadowOffset;
	public final Setting.Multi font;
	/** The style options, in order: what Apply to all and Copy from copy. */
	public final java.util.List<Setting> style = new java.util.ArrayList<>();
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
		int first = settings.size();
		brackets = bool(idPrefix + "brackets", label(namePrefix, "Brackets"), false);
		bracketColor = color(idPrefix + "bracket_color", label(namePrefix, "Bracket colour"), 0xFFAAAAAA);
		label = bool(idPrefix + "label", label(namePrefix, "Label"), false);
		labelText = text(idPrefix + "label_text", label(namePrefix, "Label text"), name);
		labelColor = color(idPrefix + "label_color", label(namePrefix, "Label colour"), 0xFFB0B5BD);
		suffix = text(idPrefix + "suffix", label(namePrefix, "Suffix"), "");
		align = radio(idPrefix + "align", label(namePrefix, "Alignment"), "Left", "Left", "Centre", "Right");
		staticWidth = num(idPrefix + "static_width", label(namePrefix, "Fixed width (0 = auto)"), 0, 0, 300, 1);
		staticHeight = num(idPrefix + "static_height", label(namePrefix, "Fixed height (0 = auto)"), 0, 0, 200, 1);
		borderWidth = num(idPrefix + "border_width", label(namePrefix, "Border thickness"), 1, 1, 4, 1);
		radius = num(idPrefix + "radius", label(namePrefix, "Corner radius"), 0, 0, 8, 1);
		padding = num(idPrefix + "padding", label(namePrefix, "Padding"), PADDING, 0, 10, 1);
		shadowColor = color(idPrefix + "shadow_color", label(namePrefix, "Shadow colour (clear = vanilla)"), 0x00000000);
		shadowOffset = num(idPrefix + "shadow_offset", label(namePrefix, "Shadow offset"), 1, 0.5, 3, 0.5);
		font = multi(idPrefix + "font", label(namePrefix, "Font"), new String[]{"Bold", "Italic", "Underline"});
		style.add(scale);
		style.add(textColor);
		style.add(textShadow);
		style.add(background);
		style.add(bgColor);
		style.add(border);
		style.add(borderColor);
		for (int i = first; i < settings.size(); i++) if (settings.get(i) != labelText) style.add(settings.get(i));
		action(idPrefix + "apply_all", label(namePrefix, "Style"), "newgen_client.hud.apply_all", () -> applyToAll());
		action(idPrefix + "copy_from", label(namePrefix, "Copy style from"), "newgen_client.hud.copy_from",
				() -> Minecraft.getInstance().setScreen(new net.newgen.client.gui.CopyStyleScreen(Minecraft.getInstance().screen, this)));
		this.idPrefix = idPrefix;
	}

	/** Copies this element's style onto every other HUD element (their positions and contents stay). */
	public void applyToAll() {
		for (HudMod other : Mods.allHuds()) if (other != this) other.copyStyleFrom(this);
		Mods.save();
	}

	public void copyStyleFrom(HudMod from) {
		for (int i = 0; i < style.size() && i < from.style.size(); i++) style.get(i).fromJson(from.style.get(i).toJson());
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

	/** One line as drawn: label, brackets, the text in the chosen font, suffix. */
	private net.minecraft.network.chat.Component styled(String text) {
		net.minecraft.network.chat.Style st = net.minecraft.network.chat.Style.EMPTY
				.withBold(font.has("Bold")).withItalic(font.has("Italic")).withUnderlined(font.has("Underline"));
		net.minecraft.network.chat.MutableComponent out = net.minecraft.network.chat.Component.empty();
		if (label.value && !labelText.value.isEmpty())
			out.append(net.minecraft.network.chat.Component.literal(labelText.value + ": ").withStyle(st.withColor(labelColor.current() & 0xFFFFFF)));
		if (brackets.value) out.append(net.minecraft.network.chat.Component.literal("[").withStyle(st.withColor(bracketColor.current() & 0xFFFFFF)));
		out.append(net.minecraft.network.chat.Component.literal(text).withStyle(st));
		if (!suffix.value.isEmpty()) out.append(net.minecraft.network.chat.Component.literal(" " + suffix.value).withStyle(st));
		if (brackets.value) out.append(net.minecraft.network.chat.Component.literal("]").withStyle(st.withColor(bracketColor.current() & 0xFFFFFF)));
		return out;
	}

	public void render(GuiGraphics g, Minecraft mc) {
		int[] custom = size(mc);
		List<String> raw = custom == null ? lines(mc) : List.of();
		List<ItemStack> icons = custom == null ? icons(mc) : List.of();
		if (custom == null && raw.isEmpty()) {
			if (!editing) return;
			raw = List.of(name);
			icons = List.of();
		}
		List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
		for (String line : raw) lines.add(styled(line));
		int pad = (int) padding.get();
		int row = icons.isEmpty() ? 10 : 18;
		int iconWidth = icons.isEmpty() ? 0 : 20;

		int textWidth = 0;
		for (var line : lines) textWidth = Math.max(textWidth, mc.font.width(line));
		int w = (custom != null ? custom[0] : iconWidth + textWidth) + pad * 2;
		int h = (custom != null ? custom[1] : lines.size() * row - (icons.isEmpty() ? 1 : 2)) + pad * 2;
		if (staticWidth.get() > 0) w = (int) staticWidth.get();
		if (staticHeight.get() > 0) h = (int) staticHeight.get();

		float s = (float) scale.get();
		width = Math.round(w * s);
		height = Math.round(h * s);
		left = Math.round(x * Math.max(0, g.guiWidth() - width));
		top = Math.round(y * Math.max(0, g.guiHeight() - height));

		g.pose().pushMatrix();
		g.pose().translate(left, top);
		g.pose().scale(s, s);
		int r = (int) radius.get(), bw = (int) borderWidth.get();
		if (background.value) {
			if (r > 0) net.newgen.client.gui.Ui.round(g, 0, 0, w, h, r, backgroundColor());
			else g.fill(0, 0, w, h, backgroundColor());
		}
		if (border.value) {
			if (r > 0) net.newgen.client.gui.Ui.ring(g, 0, 0, w, h, r, borderColor.current());
			else for (int k = 0; k < bw; k++) g.renderOutline(k, k, w - 2 * k, h - 2 * k, borderColor.current());
		}
		if (custom != null) {
			g.pose().translate(pad, pad);
			draw(g, mc);
		}
		int ink = textColor.current(), shadow = shadowColor.value;
		boolean customShadow = textShadow.value && (shadow >>> 24) != 0;
		for (int i = 0; i < lines.size(); i++) {
			if (i < icons.size() && !icons.get(i).isEmpty()) g.renderItem(icons.get(i), pad, pad + i * row);
			int lw = mc.font.width(lines.get(i)), area = w - pad * 2 - iconWidth;
			int lx = pad + iconWidth + switch (align.value) {
				case "Centre" -> (area - lw) / 2;
				case "Right" -> area - lw;
				default -> 0;
			};
			int ly = pad + i * row + (icons.isEmpty() ? 0 : 4);
			if (customShadow) {
				// A shadow of the chosen colour and offset, instead of the vanilla one.
				float o = (float) shadowOffset.get();
				g.pose().pushMatrix();
				g.pose().translate(o, o);
				g.drawString(mc.font, lines.get(i), lx, ly, shadow, false);
				g.pose().popMatrix();
			}
			g.drawString(mc.font, lines.get(i), lx, ly, ink, textShadow.value && !customShadow);
		}
		g.pose().popMatrix();
	}
}
