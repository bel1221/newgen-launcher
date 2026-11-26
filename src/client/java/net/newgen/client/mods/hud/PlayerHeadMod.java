package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Player Head: your skin's face on the HUD. */
public class PlayerHeadMod extends HudMod {
	private final Setting.Num headSize = num("head_size", "Head size", 16, 8, 64, 1);
	private final Setting.Bool hat = bool("hat_layer", "Hat layer", true);

	public PlayerHeadMod() {
		super("player_head", "Player Head", "Your skin's face on the screen", 0.02f, 0.5f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		if (mc.player == null) return null;
		int s = (int) headSize.get();
		return new int[] {s, s};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		if (mc.player == null) return;
		PlayerFaceRenderer.draw(g, mc.player.getSkin().body().texturePath(), 0, 0, (int) headSize.get(), hat.value, false, -1);
	}
}
