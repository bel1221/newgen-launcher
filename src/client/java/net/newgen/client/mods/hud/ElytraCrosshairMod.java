package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;
import net.newgen.client.mixin.hud.GameRendererFovInvoker;

/**
 * While gliding with an elytra, a marker where you are actually heading (your velocity direction on the screen).
 * The marker follows the flight, so its place in the HUD editor does not matter; scale and colour do.
 */
public class ElytraCrosshairMod extends HudMod {
	private final Setting.Choice style = radio("marker_style", "Marker", "Circle", "Circle", "Cross", "Dot");
	private final Setting.Num markerSize = num("marker_size", "Marker size", 4, 2, 10, 1);
	private final Setting.Color markerColor = color("marker_color", "Marker colour", 0xFF55FFFF);

	public ElytraCrosshairMod() {
		super("elytra_crosshair", "Elytra Crosshair", "While gliding, marks where you are heading", 0.5f, 0.5f);
		background.value = false;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	public void render(GuiGraphics g, Minecraft mc) {
		if (editing) {
			super.render(g, mc);
			return;
		}
		if (mc.player == null || !mc.player.isFallFlying()) return;
		Vec3 v = mc.player.getDeltaMovement();
		if (v.lengthSqr() < 1e-4) return;
		Camera camera = mc.gameRenderer.getMainCamera();
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float fov = ((GameRendererFovInvoker) mc.gameRenderer).newgen$getFov(camera, partial, true);
		double[] p = HudLogic2.project(v.x, v.y, v.z, camera.yRot(), camera.xRot(), fov, g.guiWidth(), g.guiHeight());
		if (p == null) return;
		float s = (float) scale.get();
		int r = (int) markerSize.get(), c = markerColor.current();
		g.pose().pushMatrix();
		g.pose().translate((float) p[0], (float) p[1]);
		g.pose().scale(s, s);
		switch (style.value) {
			case "Cross" -> {
				g.fill(-r, 0, r + 1, 1, c);
				g.fill(0, -r, 1, r + 1, c);
			}
			case "Dot" -> g.fill(-1, -1, 2, 2, c);
			default -> ring(g, r, c);
		}
		g.pose().popMatrix();
	}

	/** A one-pixel circle outline of radius r around (0, 0). */
	private static void ring(GuiGraphics g, int r, int c) {
		int steps = Math.max(12, r * 8);
		int px = Integer.MIN_VALUE, py = 0;
		for (int i = 0; i <= steps; i++) {
			double a = 2 * Math.PI * i / steps;
			int x = (int) Math.round(Math.cos(a) * r), y = (int) Math.round(Math.sin(a) * r);
			if (x == px && y == py) continue;
			g.fill(x, y, x + 1, y + 1, c);
			px = x;
			py = y;
		}
	}
}
