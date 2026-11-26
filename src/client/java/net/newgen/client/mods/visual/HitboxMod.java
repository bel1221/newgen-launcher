package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class HitboxMod extends Mod {
	public static final HitboxMod I = new HitboxMod();

	private final Setting.Choice show = choice("show", "Show for", "All", "All", "Players", "Mobs and players");
	private final Setting.Color color = color("color", "Colour", 0xFFFFFFFF);
	private final Setting.Num width = num("width", "Line width", 2, 1, 5, 0.5);
	private final Setting.Num range = num("range", "Range (blocks)", 32, 4, 128, 4);
	private final Setting.Bool look = bool("look", "Show where they look", false);
	private final Setting.Color lookColor = color("look_color", "Look line colour", 0xFF3060FF);
	private final Setting.Bool players = bool("players", "Players", true);
	private final Setting.Color playersColor = color("players_color", "Players colour", 0xFFFFFFFF);
	private final Setting.Bool mobs = bool("mobs", "Mobs", true);
	private final Setting.Color mobsColor = color("mobs_color", "Mobs colour", 0xFFFFFFFF);
	private final Setting.Bool items = bool("items", "Items", true);
	private final Setting.Color itemsColor = color("items_color", "Items colour", 0xFFFFFFFF);
	private final Setting.Bool projectiles = bool("projectiles", "Projectiles", true);
	private final Setting.Color projectilesColor = color("projectiles_color", "Projectiles colour", 0xFFFFFFFF);
	private final Setting.Choice pattern = choice("pattern", "Line pattern", "Solid", "Solid", "Dashed", "Dotted");
	private final Setting.Bool hurt = bool("hurt", "Colour when damaged", false);
	private final Setting.Color hurtColor = color("hurt_color", "Damaged colour", 0xFFFF3030);
	private final Setting.Bool reach = bool("reach", "Colour when in reach", false);
	private final Setting.Color reachColor = color("reach_color", "In reach colour", 0xFF30FF30);

	private HitboxMod() {
		super("hitbox", "Hitboxes", "Draws the hitbox of entities");
	}

	/** The colour to draw e with, or 0 when its kind is switched off. */
	private int colorFor(Minecraft mc, Entity e) {
		Setting.Color c;
		if (e instanceof Player) {
			if (!players.value) return 0;
			c = playersColor;
		} else if (e instanceof LivingEntity) {
			if (!mobs.value) return 0;
			c = mobsColor;
		} else if (e instanceof ItemEntity) {
			if (!items.value) return 0;
			c = itemsColor;
		} else if (e instanceof Projectile) {
			if (!projectiles.value) return 0;
			c = projectilesColor;
		} else {
			c = color;
		}
		if (hurt.value && e instanceof LivingEntity living && living.hurtTime > 0) return hurtColor.current();
		if (reach.value && mc.player != null && e != mc.player) {
			double r = mc.player.entityInteractionRange();
			Vec3 eye = mc.player.getEyePosition();
			Vec3 near = closest(e.getBoundingBox(), eye);
			if (near.distanceToSqr(eye) <= r * r) return reachColor.current();
		}
		// A per-type colour left at plain white falls back to the general colour, as before these sections existed.
		return c != color && c.value == 0xFFFFFFFF && !c.chroma ? color.current() : c.current();
	}

	private static Vec3 closest(AABB box, Vec3 p) {
		return new Vec3(Math.max(box.minX, Math.min(box.maxX, p.x)), Math.max(box.minY, Math.min(box.maxY, p.y)),
				Math.max(box.minZ, Math.min(box.maxZ, p.z)));
	}

	/** Called once per frame while the level is drawn. */
	public void emit(Minecraft mc, Entity camera, float partialTick) {
		if (!enabled) return;
		double max = range.get() * range.get();
		float w = (float) width.get();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e == camera && mc.options.getCameraType().isFirstPerson()) continue;
			if (show.value.equals("Players") && !(e instanceof Player)) continue;
			if (show.value.equals("Mobs and players") && !(e instanceof LivingEntity)) continue;
			if (e.isInvisible() || e.distanceToSqr(camera) > max) continue;
			int c = colorFor(mc, e);
			if (c == 0) continue;
			// The box follows the smoothed position, like the model does.
			Vec3 pos = e.getPosition(partialTick);
			AABB box = e.getBoundingBox().move(pos.subtract(e.position()));
			if (pattern.value.equals("Solid")) Gizmos.cuboid(box, GizmoStyle.stroke(c, w));
			else edges(box, c, w, pattern.value.equals("Dotted"));
			if (look.value) {
				Vec3 eye = pos.add(0, e.getEyeHeight(), 0);
				Gizmos.line(eye, eye.add(e.getViewVector(partialTick).scale(2)), lookColor.current(), w);
			}
		}
	}

	/** The 12 edges of the box drawn as dashes (or dots). */
	private static void edges(AABB b, int c, float w, boolean dots) {
		double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
		for (double y : ys) for (double z : zs) dashed(new Vec3(b.minX, y, z), new Vec3(b.maxX, y, z), c, w, dots);
		for (double x : xs) for (double z : zs) dashed(new Vec3(x, b.minY, z), new Vec3(x, b.maxY, z), c, w, dots);
		for (double x : xs) for (double y : ys) dashed(new Vec3(x, y, b.minZ), new Vec3(x, y, b.maxZ), c, w, dots);
	}

	private static void dashed(Vec3 a, Vec3 b, int c, float w, boolean dots) {
		double len = a.distanceTo(b);
		double on = dots ? 0.04 : 0.15, period = dots ? 0.12 : 0.25;
		for (double t = 0; t < len; t += period) {
			Gizmos.line(a.lerp(b, t / len), a.lerp(b, Math.min(t + on, len) / len), c, w);
		}
	}
}
