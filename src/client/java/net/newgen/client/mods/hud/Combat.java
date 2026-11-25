package net.newgen.client.mods.hud;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Hit tracking shared by the Combo, PvP Info and Reach Display mods. */
final class Combat {
	static int combo, bestCombo, hitsDealt, hitsTaken;
	/** Distance of the last attack, in blocks; -1 = none yet. */
	static double reach = -1;
	/** A combo ends when no hit lands for this long (ms). Set by ComboMod. */
	static long comboTimeout = 3000;

	private static int pendingTarget = -1;
	private static long attackTime, lastHitTime;
	private static int lastHurtTime;
	private static Object lastLevel;

	private Combat() {
	}

	static void init() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			Minecraft mc = Minecraft.getInstance();
			if (level.isClientSide() && player == mc.player) {
				pendingTarget = entity.getId();
				attackTime = System.currentTimeMillis();
				reach = mc.hitResult instanceof EntityHitResult e
						? player.getEyePosition().distanceTo(e.getLocation())
						: player.distanceTo(entity);
			}
			return InteractionResult.PASS;
		});
	}

	/** Call every frame from a mod that shows these numbers. */
	static void update(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		long now = System.currentTimeMillis();
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			combo = bestCombo = hitsDealt = hitsTaken = 0;
			reach = -1;
		}
		if (pendingTarget != -1) {
			// shortcut: a hit counts when the target starts its hurt animation soon after our attack,
			// so a hit by someone else in the same moment is counted too. Upgrade to damage packets if needed.
			Entity target = mc.level.getEntity(pendingTarget);
			if (target instanceof LivingEntity living && living.hurtTime >= 8) {
				combo++;
				hitsDealt++;
				bestCombo = Math.max(bestCombo, combo);
				lastHitTime = now;
				pendingTarget = -1;
			} else if (now - attackTime > 500) {
				pendingTarget = -1;
			}
		}
		int hurt = mc.player.hurtTime;
		if (hurt > lastHurtTime) {
			hitsTaken++;
			combo = 0;
		}
		lastHurtTime = hurt;
		if (now - lastHitTime > comboTimeout) combo = 0;
	}
}
