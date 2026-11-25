package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.effect.MobEffectInstance;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class PotionEffectsMod extends HudMod {
	/** Read by GuiMixin to switch off the vanilla effect icons. */
	public final Setting.Bool hideVanilla = bool("hide_vanilla", "Hide vanilla effect icons", true);

	private final Setting.Color timeColor = color("time_color", "Time colour", 0xFFAAAAAA);

	private final List<MobEffectInstance> effects = new ArrayList<>();

	public PotionEffectsMod() {
		super("potion_effects", "Potion Effects", "Your active effects with name and time left", 1f, 0.2f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		effects.clear();
		if (mc.player == null) return null;
		effects.addAll(mc.player.getActiveEffects());
		if (effects.isEmpty()) return null;
		int textWidth = 0;
		for (MobEffectInstance e : effects) {
			textWidth = Math.max(textWidth, Math.max(mc.font.width(name(e)), mc.font.width(time(e))));
		}
		return new int[] {20 + textWidth, effects.size() * 20 - 2};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		for (int i = 0; i < effects.size(); i++) {
			MobEffectInstance e = effects.get(i);
			int y = i * 20;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(e.getEffect()), 0, y, 18, 18);
			g.drawString(mc.font, name(e), 20, y, textColor.value, textShadow.value);
			g.drawString(mc.font, time(e), 20, y + 9, timeColor.value, textShadow.value);
		}
	}

	private static String name(MobEffectInstance e) {
		String name = e.getEffect().value().getDisplayName().getString();
		return e.getAmplifier() > 0 ? name + " " + (e.getAmplifier() + 1) : name;
	}

	private static String time(MobEffectInstance e) {
		if (e.isInfiniteDuration()) return "Infinite";
		int seconds = e.getDuration() / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
