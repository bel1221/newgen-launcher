package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Block light and sky light at your feet and at the block you look at; coloured when mobs can spawn (block light 0). */
public class LightLevelMod extends HudMod {
	private final Setting.Bool target = bool("targeted_block", "Targeted block", true);
	private final Setting.Bool sky = bool("sky_light", "Show sky light", true);
	private final Setting.Color spawnColor = color("spawn_color", "Mob spawn colour", 0xFFFF5555);

	private final List<String> text = new ArrayList<>();
	private final List<Boolean> danger = new ArrayList<>();

	public LightLevelMod() {
		super("light_level", "Light Level", "Block and sky light where you stand and where you look", 0f, 0.5f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	private void line(Minecraft mc, String key, BlockPos pos) {
		int block = mc.level.getBrightness(LightLayer.BLOCK, pos);
		text.add(sky.value ? I18n.get(key + "_sky", block, mc.level.getBrightness(LightLayer.SKY, pos)) : I18n.get(key, block));
		danger.add(block == 0);
	}

	@Override
	protected int[] size(Minecraft mc) {
		text.clear();
		danger.clear();
		if (mc.player == null || mc.level == null) return editing ? new int[]{mc.font.width(name), 9} : null;
		line(mc, "newgen_client.hud.light_level.feet", mc.player.blockPosition());
		if (target.value && mc.hitResult instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK)
			line(mc, "newgen_client.hud.light_level.target", b.getBlockPos().relative(b.getDirection()));
		int w = 0;
		for (String s : text) w = Math.max(w, mc.font.width(s));
		return new int[]{w, text.size() * 10 - 1};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		if (text.isEmpty()) {
			g.drawString(mc.font, name, 0, 0, textColor.current(), textShadow.value);
			return;
		}
		for (int i = 0; i < text.size(); i++)
			g.drawString(mc.font, text.get(i), 0, i * 10, danger.get(i) ? spawnColor.current() : textColor.current(), textShadow.value);
	}
}
