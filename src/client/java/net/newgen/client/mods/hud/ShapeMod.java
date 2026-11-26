package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Shape: a decorative rectangle, circle, ring or line to frame or split up the HUD. */
public class ShapeMod extends HudMod {
	private final Setting.Choice shape = radio("shape", "Shape", "Rectangle", "Rectangle", "Circle", "Ring", "Line");
	private final Setting.Num shapeWidth = num("shape_width", "Width", 40, 1, 400, 1);
	private final Setting.Num shapeHeight = num("shape_height", "Height", 20, 1, 400, 1);
	private final Setting.Color fill = color("fill_color", "Fill colour", 0x80F6B041);
	private final Setting.Num thickness = num("thickness", "Thickness (ring, line)", 2, 1, 20, 1);
	private final Setting.Choice direction = choice("line_direction", "Line direction", "Horizontal", "Horizontal", "Vertical", "Diagonal down", "Diagonal up");
	private final Setting.Bool outline = bool("outline", "Outline", false);
	private final Setting.Color outlineColor = color("outline_color", "Outline colour", 0xFFFFFFFF);
	private final Setting.Num outlineWidth = num("outline_width", "Outline thickness", 1, 1, 10, 1);

	public ShapeMod() {
		super("shape", "Shape", "A decorative rectangle, circle, ring or line", 0.5f, 0.5f);
		background.value = false;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		return new int[] {(int) shapeWidth.get(), (int) shapeHeight.get()};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		int w = (int) shapeWidth.get(), h = (int) shapeHeight.get(), t = (int) thickness.get(), o = (int) outlineWidth.get();
		int color = fill.current();
		switch (shape.value) {
			case "Circle" -> {
				HudShapes.disc(g, 0, 0, w, h, color);
				if (outline.value) HudShapes.ring(g, 0, 0, w, h, o, outlineColor.current());
			}
			case "Ring" -> {
				HudShapes.ring(g, 0, 0, w, h, t, color);
				if (outline.value) {
					HudShapes.ring(g, 0, 0, w, h, o, outlineColor.current());
					if (w > 2 * t && h > 2 * t) HudShapes.ring(g, t, t, w - 2 * t, h - 2 * t, o, outlineColor.current());
				}
			}
			case "Line" -> {
				switch (direction.value) {
					case "Vertical" -> g.fill((w - t) / 2, 0, (w - t) / 2 + t, h, color);
					case "Diagonal down" -> HudShapes.line(g, t / 2.0, t / 2.0, w - t / 2.0, h - t / 2.0, t, color);
					case "Diagonal up" -> HudShapes.line(g, t / 2.0, h - t / 2.0, w - t / 2.0, t / 2.0, t, color);
					default -> g.fill(0, (h - t) / 2, w, (h - t) / 2 + t, color);
				}
			}
			default -> {
				g.fill(0, 0, w, h, color);
				if (outline.value) for (int k = 0; k < o && 2 * k < Math.min(w, h); k++) g.renderOutline(k, k, w - 2 * k, h - 2 * k, outlineColor.current());
			}
		}
	}
}
