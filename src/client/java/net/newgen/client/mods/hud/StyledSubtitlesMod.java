package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/**
 * Restyles the vanilla subtitles: row background, text colour, scale and place (HUD editor). The SubtitleOverlay mixin
 * moves the vanilla drawing into this element's box and recolours it; this element only reserves the box.
 */
public class StyledSubtitlesMod extends HudMod {
	public static StyledSubtitlesMod INSTANCE;

	/** Vanilla anchors the subtitles at the bottom right: right edge at width - 1, bottom row ends at height - 30. */
	private static final int RIGHT = 1, BOTTOM = 30;

	private final Setting.Color rowColor = color("row_color", "Row background", 0xCC000000);
	private final Setting.Bool fade = bool("fade_text", "Fade old sounds", true);

	/** Size of the subtitles last drawn, and when; set by the mixin. */
	private int lastW = 120, lastH = 30;
	private long lastSeen;
	// The frame being drawn now.
	private int rows, maxW;
	private boolean pushed;

	public StyledSubtitlesMod() {
		super("styled_subtitles", "Styled Subtitles", "Restyle and move the subtitles (turn subtitles on in Accessibility)", 1f, 0.9f);
		background.value = false;
		padding.set(0);
		INSTANCE = this;
	}

	private boolean showing() {
		return System.currentTimeMillis() - lastSeen < 250;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		return showing() || editing ? new int[]{lastW, lastH} : null;
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		if (editing && !showing()) g.drawString(mc.font, name, 0, 0, textColor.current(), textShadow.value);
	}

	// ---- called by the SubtitleOverlay mixin

	public static boolean active() {
		return INSTANCE != null && INSTANCE.enabled;
	}

	/** Before vanilla draws: map its bottom-right anchor onto the bottom right of this element's box. */
	public void begin(GuiGraphics g) {
		rows = maxW = 0;
		float s = (float) scale.get();
		int pad = (int) padding.get();
		if (width <= 0 && height <= 0) {
			// Not drawn yet (first frame): vanilla place.
			pushed = false;
			return;
		}
		float bx = left + (pad + lastW) * s, by = top + (pad + lastH) * s;
		g.pose().pushMatrix();
		g.pose().translate(bx, by);
		g.pose().scale(s, s);
		g.pose().translate(-(g.guiWidth() - RIGHT), -(g.guiHeight() - BOTTOM));
		pushed = true;
	}

	public void end(GuiGraphics g) {
		if (pushed) g.pose().popMatrix();
		pushed = false;
		if (rows > 0) {
			lastW = maxW;
			lastH = rows * 10;
			lastSeen = System.currentTimeMillis();
		}
	}

	/** One row's background: x1..x2 is the row width. */
	public int row(int x1, int x2) {
		rows++;
		maxW = Math.max(maxW, x2 - x1);
		return rowColor.current();
	}

	/** Vanilla text colour is grey p,p,p fading from 255 to 75 as the sound gets older; keep that fade on our colour. */
	public int text(int vanilla) {
		int c = textColor.current();
		if (!fade.value) return c;
		float f = ARGB.red(vanilla) / 255f;
		return ARGB.color(ARGB.alpha(c), Math.round(ARGB.red(c) * f), Math.round(ARGB.green(c) * f), Math.round(ARGB.blue(c) * f));
	}
}
