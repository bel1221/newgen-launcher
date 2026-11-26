package net.newgen.client.mods.utility;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Speed, jump height and health of the horse you ride or look at. */
public class HorseStatsMod extends HudMod {
	private final Setting.Bool speed = bool("speed", "Show speed", true);
	private final Setting.Bool jump = bool("jump", "Show jump height", true);
	private final Setting.Bool health = bool("health", "Show health", true);

	public HorseStatsMod() {
		super("horse_stats", "Horse Stats", "Stats of the horse you ride or look at", 0f, 0.4f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		AbstractHorse horse = mc.player.getVehicle() instanceof AbstractHorse ridden ? ridden
				: mc.crosshairPickEntity instanceof AbstractHorse seen ? seen : null;
		if (horse == null) return List.of();

		List<String> out = new ArrayList<>();
		// 42.16 = blocks per second for 1.0 of the movement speed attribute on a ridden horse.
		Double s = value(horse, Attributes.MOVEMENT_SPEED);
		if (speed.value && s != null) out.add(String.format("Speed: %.1f b/s", s * 42.16));
		// Community curve fit of jump strength to jump height in blocks.
		Double j = value(horse, Attributes.JUMP_STRENGTH);
		if (jump.value && j != null) {
			out.add(String.format("Jump: %.1f blocks", -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367));
		}
		if (health.value) out.add(net.minecraft.client.resources.language.I18n.get("newgen_client.hud.horse_stats.health", String.format(java.util.Locale.ROOT, "%.0f / %.0f", horse.getHealth(), horse.getMaxHealth())));
		return out;
	}

	/** Llamas and the like have no jump attribute; asking for a missing one would throw. */
	private static Double value(AbstractHorse horse, Holder<Attribute> attribute) {
		AttributeInstance a = horse.getAttribute(attribute);
		return a == null ? null : a.getValue();
	}
}
