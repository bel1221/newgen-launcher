package net.newgen.client.mods.hud;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
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
	private final Setting.Choice layout = choice("layout", "Layout", "WASD + mouse", "WASD + mouse", "WASD", "Mouse only");
	private final Setting.Bool cps = bool("cps", "Show CPS in mouse keys", true);
	private final Setting.Choice spaceStyle = choice("space_style", "Spacebar style", "Key name", "Key name", "Bar", "Blank");
	private final Setting.Num fade = num("fade", "Fade time (ms)", 80, 0, 500, 10);
	private final Setting.Color unpressedColor = color("unpressed_color", "Unpressed colour", 0x80000000);
	private final Setting.Color pressedTextColor = color("pressed_text_color", "Pressed text colour", 0xFF000000);
	private final Setting.Key custom1 = key("custom_key_1", "Extra key 1", -1);
	private final Setting.Key custom2 = key("custom_key_2", "Extra key 2", -1);
	private final Setting.Key custom3 = key("custom_key_3", "Extra key 3", -1);

	/** Press/release state per box: {down 0/1, time of the last change}. */
	private final Map<String, long[]> states = new HashMap<>();
	private final ArrayDeque<Long> leftClicks = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
	private boolean leftWas, rightWas;

	public KeystrokesMod() {
		super("keystrokes", "Keystrokes", "Shows the movement keys and mouse buttons you press", 0f, 0.75f);
		background.value = false; // every key has its own box
	}

	/** Before the unpressed colour existed the keys used the background colour. */
	@Override
	protected void migrate(com.google.gson.JsonObject saved) {
		super.migrate(saved);
		if (!saved.has(unpressedColor.id)) unpressedColor.value = bgColor.value;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	private boolean showKeys() {
		return !layout.value.equals("Mouse only");
	}

	private boolean showMouse() {
		return mouse.value && !layout.value.equals("WASD");
	}

	private int customCount() {
		int n = 0;
		for (Setting.Key k : new Setting.Key[] {custom1, custom2, custom3}) if (k.value != -1) n++;
		return n;
	}

	@Override
	protected int[] size(Minecraft mc) {
		int h = 0;
		if (showKeys()) h += 44;
		if (showMouse()) h += 22;
		if (showKeys() && space.value) h += 14;
		if (customCount() > 0) h += 22;
		return new int[] {64, Math.max(12, h - 2)};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		Options o = mc.options;
		countClicks(mc);
		int y = 0;
		if (showKeys()) {
			box(g, mc, "up", label(o.keyUp), o.keyUp.isDown(), 22, 0, 20, 20);
			box(g, mc, "left", label(o.keyLeft), o.keyLeft.isDown(), 0, 22, 20, 20);
			box(g, mc, "down", label(o.keyDown), o.keyDown.isDown(), 22, 22, 20, 20);
			box(g, mc, "right", label(o.keyRight), o.keyRight.isDown(), 44, 22, 20, 20);
			y = 44;
		}
		if (showMouse()) {
			box(g, mc, "lmb", cps.value ? net.minecraft.client.resources.language.I18n.get("newgen_client.hud.lmb") + "|" + leftClicks.size() : net.minecraft.client.resources.language.I18n.get("newgen_client.hud.lmb"), o.keyAttack.isDown(), 0, y, 31, 20);
			box(g, mc, "rmb", cps.value ? net.minecraft.client.resources.language.I18n.get("newgen_client.hud.rmb") + "|" + rightClicks.size() : net.minecraft.client.resources.language.I18n.get("newgen_client.hud.rmb"), o.keyUse.isDown(), 33, y, 31, 20);
			y += 22;
		}
		if (showKeys() && space.value) {
			String text = switch (spaceStyle.value) {
				case "Bar", "Blank" -> "";
				default -> label(o.keyJump);
			};
			box(g, mc, "jump", text, o.keyJump.isDown(), 0, y, 64, 12);
			if (spaceStyle.value.equals("Bar")) g.fill(20, y + 5, 44, y + 7, textColor.current());
			y += 14;
		}
		Setting.Key[] extra = {custom1, custom2, custom3};
		int n = customCount(), x = 0, w = n == 0 ? 0 : (64 - (n - 1) * 2) / n;
		for (int i = 0; i < extra.length; i++) {
			if (extra[i].value == -1) continue;
			box(g, mc, "custom" + i, keyName(extra[i]), extra[i].isDown(mc), x, y, w, 20);
			x += w + 2;
		}
	}

	/** Counts clicks from the button states, once per frame (CpsMod does the same). */
	private void countClicks(Minecraft mc) {
		long now = System.currentTimeMillis();
		boolean left = mc.mouseHandler.isLeftPressed(), right = mc.mouseHandler.isRightPressed();
		if (left && !leftWas) leftClicks.add(now);
		if (right && !rightWas) rightClicks.add(now);
		leftWas = left;
		rightWas = right;
		while (!leftClicks.isEmpty() && now - leftClicks.peek() > 1000) leftClicks.poll();
		while (!rightClicks.isEmpty() && now - rightClicks.peek() > 1000) rightClicks.poll();
	}

	private static String label(KeyMapping key) {
		return key.getTranslatedKeyMessage().getString().toUpperCase();
	}

	private static String keyName(Setting.Key key) {
		InputConstants.Key k = key.isMouse() ? InputConstants.Type.MOUSE.getOrCreate(key.value - Setting.Key.MOUSE)
				: InputConstants.Type.KEYSYM.getOrCreate(key.value);
		return k.getDisplayName().getString().toUpperCase();
	}

	/** 0 = released, 1 = pressed; in between while fading. */
	private double amount(String id, boolean down) {
		long now = System.currentTimeMillis();
		long[] s = states.computeIfAbsent(id, k -> new long[] {down ? 1 : 0, 0});
		if ((s[0] == 1) != down) {
			s[0] = down ? 1 : 0;
			s[1] = now;
		}
		double t = fade.get() <= 0 ? 1 : Math.min(1, (now - s[1]) / fade.get());
		return down ? t : 1 - t;
	}

	private void box(GuiGraphics g, Minecraft mc, String id, String label, boolean down, int x, int y, int w, int h) {
		double a = amount(id, down);
		g.fill(x, y, x + w, y + h, HudValues.blend(unpressedColor.current(), pressedColor.current(), a));
		int ink = HudValues.blend(textColor.current(), pressedTextColor.current(), a);
		String[] parts = label.split("\\|");
		if (parts.length == 2) {
			// Mouse key with CPS: name on top, clicks below in a smaller line.
			g.drawString(mc.font, parts[0], x + (w - mc.font.width(parts[0])) / 2, y + 2, ink, a < 0.5 && textShadow.value);
			String c = parts[1] + " CPS";
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f, y + 12);
			g.pose().scale(0.6f, 0.6f);
			g.drawString(mc.font, c, -mc.font.width(c) / 2, 0, ink, false);
			g.pose().popMatrix();
			return;
		}
		g.drawString(mc.font, label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2, ink, a < 0.5 && textShadow.value);
	}
}
