package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

	private HitboxMod() {
		super("hitbox", "Hitboxes", "Draws the hitbox of entities");
	}

	/** Called once per frame while the level is drawn. */
	public void emit(Minecraft mc, Entity camera, float partialTick) {
		if (!enabled) return;
		double max = range.get() * range.get();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e == camera && mc.options.getCameraType().isFirstPerson()) continue;
			if (show.value.equals("Players") && !(e instanceof Player)) continue;
			if (show.value.equals("Mobs and players") && !(e instanceof LivingEntity)) continue;
			if (e.isInvisible() || e.distanceToSqr(camera) > max) continue;
			// The box follows the smoothed position, like the model does.
			Vec3 pos = e.getPosition(partialTick);
			Gizmos.cuboid(e.getBoundingBox().move(pos.subtract(e.position())), GizmoStyle.stroke(color.value, (float) width.get()));
			if (look.value) {
				Vec3 eye = pos.add(0, e.getEyeHeight(), 0);
				Gizmos.line(eye, eye.add(e.getViewVector(partialTick).scale(2)), lookColor.value, (float) width.get());
			}
		}
	}
}
