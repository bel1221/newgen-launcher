package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class MobSizeMod extends Mod {
	public static final MobSizeMod I = new MobSizeMod();
	private final Setting.Num self = num("self", "Your size", 1, 0.3, 3, 0.1);
	private final Setting.Num players = num("players", "Size of other players", 1, 0.3, 3, 0.1);
	private final Setting.Num mobs = num("mobs", "Mob size", 1, 0.3, 3, 0.1);

	private MobSizeMod() {
		super("mob_size", "Mob Size", "Draws players and mobs bigger or smaller (looks only, hitboxes stay)");
	}

	public float factor(LivingEntity entity) {
		if (!enabled) return 1;
		if (entity == Minecraft.getInstance().player) return (float) self.get();
		return (float) (entity instanceof Player ? players : mobs).get();
	}
}
