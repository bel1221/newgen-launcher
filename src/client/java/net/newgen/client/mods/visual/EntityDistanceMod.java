package net.newgen.client.mods.visual;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/**
 * Entity render distance per kind: players, hostile mobs, other mobs, dropped items and orbs, everything else.
 * 0 = as far as the game draws them. Only ever draws less than vanilla, never more.
 */
public class EntityDistanceMod extends Mod {
	public static final EntityDistanceMod I = new EntityDistanceMod();
	private final Setting.Num players = num("players", "Players (blocks, 0 = normal)", 0, 0, 256, 8);
	private final Setting.Num hostile = num("hostile", "Hostile mobs (blocks)", 0, 0, 256, 8);
	private final Setting.Num mobs = num("mobs", "Other mobs (blocks)", 0, 0, 256, 8);
	private final Setting.Num items = num("items", "Items and orbs (blocks)", 0, 0, 256, 8);
	private final Setting.Num other = num("other", "Everything else (blocks)", 0, 0, 256, 8);

	private EntityDistanceMod() {
		super("entity_distance", "Entity Distance", "How far away each kind of entity is still drawn");
	}

	/** True when the entity is farther from the camera than its kind's limit. */
	public boolean tooFar(Entity e, double camX, double camY, double camZ) {
		Setting.Num limit = e instanceof Player ? players : e instanceof Enemy ? hostile
				: e instanceof Mob ? mobs : e instanceof ItemEntity || e instanceof ExperienceOrb ? items : other;
		double max = limit.get();
		return max > 0 && e.distanceToSqr(camX, camY, camZ) > max * max;
	}
}
