package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.newgen.client.core.HudMod;

/**
 * Text lines made of coloured parts, for HUDs that colour parts of a line (value colours, per-axis colours).
 * Keeps the element's label, brackets, suffix and font style, like the plain text lines.
 */
public final class HudText {
	private HudText() {
	}

	/** A piece of text in a colour (ARGB). */
	public record Part(String text, int color) {
	}

	public static Component line(HudMod hud, List<Part> parts) {
		Style st = Style.EMPTY.withBold(hud.font.has("Bold")).withItalic(hud.font.has("Italic")).withUnderlined(hud.font.has("Underline"));
		int ink = hud.textColor.current() & 0xFFFFFF;
		MutableComponent out = Component.empty();
		if (hud.label.value && !hud.labelText.value.isEmpty())
			out.append(Component.literal(hud.labelText.value + ": ").withStyle(st.withColor(hud.labelColor.current() & 0xFFFFFF)));
		if (hud.brackets.value) out.append(Component.literal("[").withStyle(st.withColor(hud.bracketColor.current() & 0xFFFFFF)));
		for (Part p : parts) out.append(Component.literal(p.text()).withStyle(st.withColor(p.color() & 0xFFFFFF)));
		if (!hud.suffix.value.isEmpty()) out.append(Component.literal(" " + hud.suffix.value).withStyle(st.withColor(ink)));
		if (hud.brackets.value) out.append(Component.literal("]").withStyle(st.withColor(hud.bracketColor.current() & 0xFFFFFF)));
		return out;
	}

	/** {width, height} of the lines, 10 px a row. */
	public static int[] size(Minecraft mc, List<Component> lines) {
		int w = 0;
		for (Component c : lines) w = Math.max(w, mc.font.width(c));
		return new int[] {w, Math.max(0, lines.size() * 10 - 1)};
	}

	/** Draws the lines at (0,0) with the element's alignment and shadow. */
	public static void draw(GuiGraphics g, Minecraft mc, HudMod hud, List<Component> lines) {
		int area = size(mc, lines)[0];
		for (int i = 0; i < lines.size(); i++) {
			int lw = mc.font.width(lines.get(i));
			int x = switch (hud.align.value) {
				case "Centre" -> (area - lw) / 2;
				case "Right" -> area - lw;
				default -> 0;
			};
			g.drawString(mc.font, lines.get(i), x, i * 10, 0xFFFFFFFF, hud.textShadow.value);
		}
	}
}
