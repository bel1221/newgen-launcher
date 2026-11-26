package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/**
 * The key bound to each hotbar slot, drawn over the vanilla hotbar. The element is exactly the size of the hotbar
 * (182 x 22) whatever the padding, so at its default place (bottom centre) it lines up with the slots.
 */
public class HotbarKeysMod extends HudMod {
	private static final int W = 182, H = 22;

	private final Setting.Choice corner = radio("corner", "Corner", "Top left", "Top left", "Top right", "Bottom left");
	private final Setting.Num textScale = num("key_text_scale", "Key text scale", 0.7, 0.5, 1.0, 0.05);
	private final Setting.Bool selectedOnly = bool("hide_selected", "Hide on selected slot", false);

	public HotbarKeysMod() {
		super("hotbar_keys", "Hotbar Key Overlay", "Shows the key of each hotbar slot over the hotbar", 0.5f, 1f);
		background.value = false;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		if (!editing && (mc.player == null || mc.player.isSpectator())) return null; // no hotbar
		int p = (int) padding.get();
		return new int[]{Math.max(0, W - 2 * p), Math.max(0, H - 2 * p)};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		int p = (int) padding.get();
		float ts = (float) textScale.get();
		int selected = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
		g.pose().pushMatrix();
		g.pose().translate(-p, -p);
		for (int i = 0; i < 9; i++) {
			if (selectedOnly.value && i == selected) continue;
			String key = HudLogic2.shortKey(mc.options.keyHotbarSlots[i].getTranslatedKeyMessage().getString());
			if (key.isEmpty()) continue;
			int w = Math.round(mc.font.width(key) * ts), h = Math.round(8 * ts);
			// Slot i: the item sits at x = 3 + 20 i, y = 3, 16 px square.
			float x = 3 + 20 * i, y = 3;
			if (corner.value.equals("Top right")) x += 16 - w;
			if (corner.value.equals("Bottom left")) y += 16 - h;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(ts, ts);
			g.drawString(mc.font, key, 0, 0, textColor.current(), textShadow.value);
			g.pose().popMatrix();
		}
		g.pose().popMatrix();
	}
}
