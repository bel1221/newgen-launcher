package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class KeystrokesMod extends HudMod {
	private final Setting.Bool mouse = bool("mouse", "Show mouse buttons", true);
	private final Setting.Bool space = bool("space", "Show jump key", true);
	private final Setting.Color pressedColor = color("pressed_color", "Pressed colour", 0xB0FFFFFF);

	public KeystrokesMod() {
		super("keystrokes", "Keystrokes", "Shows the movement keys and mouse buttons you press", 0f, 0.75f);
		background.value = false; // every key has its own box
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		return new int[] {64, 42 + (mouse.value ? 22 : 0) + (space.value ? 14 : 0)};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		Options o = mc.options;
		box(g, mc, label(o.keyUp), o.keyUp.isDown(), 22, 0, 20, 20);
		box(g, mc, label(o.keyLeft), o.keyLeft.isDown(), 0, 22, 20, 20);
		box(g, mc, label(o.keyDown), o.keyDown.isDown(), 22, 22, 20, 20);
		box(g, mc, label(o.keyRight), o.keyRight.isDown(), 44, 22, 20, 20);
		int y = 44;
		if (mouse.value) {
			box(g, mc, "LMB", o.keyAttack.isDown(), 0, y, 31, 20);
			box(g, mc, "RMB", o.keyUse.isDown(), 33, y, 31, 20);
			y += 22;
		}
		if (space.value) box(g, mc, label(o.keyJump), o.keyJump.isDown(), 0, y, 64, 12);
	}

	private static String label(KeyMapping key) {
		return key.getTranslatedKeyMessage().getString().toUpperCase();
	}

	private void box(GuiGraphics g, Minecraft mc, String label, boolean down, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, down ? pressedColor.value : backgroundColor());
		g.drawString(mc.font, label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2,
				down ? 0xFF000000 : textColor.value, !down && textShadow.value);
	}
}
