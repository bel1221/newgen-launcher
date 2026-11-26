package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** A dot that follows how you turn the camera each frame, springs back to the centre and leaves a fading trail. */
public class MouseStrokesMod extends HudMod {
	private final Setting.Num boxSize = num("box_size", "Size", 50, 20, 120, 1);
	private final Setting.Num sensitivity = num("sensitivity", "Sensitivity scale", 1.0, 0.1, 5.0, 0.1);
	private final Setting.Num trailLength = num("trail_length", "Trail length", 20, 2, 60, 1);
	private final Setting.Num returnSpeed = num("return_speed", "Return speed", 0.15, 0.01, 1.0, 0.01);
	private final Setting.Color dotColor = color("dot_color", "Dot colour", 0xFFFFFFFF);
	private final Setting.Color trailColor = color("trail_color", "Trail colour", 0xFFF6B041);
	private final Setting.Bool crosshair = bool("centre_mark", "Centre mark", true);

	private final HudLogic2.MouseTrail trail = new HudLogic2.MouseTrail();
	private float lastYaw, lastPitch;
	private long lastNanos;
	private boolean started;

	public MouseStrokesMod() {
		super("mouse_strokes", "Mouse Strokes", "A trail that shows how you move the mouse", 0f, 0.6f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		int s = (int) boxSize.get();
		return new int[]{s, s};
	}

	private void update(Minecraft mc) {
		long now = System.nanoTime();
		if (mc.player == null) {
			started = false;
			return;
		}
		float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
		double dx = 0, dy = 0;
		if (started) {
			dx = Mth.wrapDegrees(yaw - lastYaw);
			dy = pitch - lastPitch;
			// A teleport or respawn snaps the view: not a mouse movement.
			if (Math.abs(dx) > 90 || Math.abs(dy) > 90) dx = dy = 0;
		}
		double seconds = started ? Math.min(0.25, (now - lastNanos) / 1e9) : 0;
		started = true;
		lastYaw = yaw;
		lastPitch = pitch;
		lastNanos = now;
		double half = boxSize.get() / 2 - 2;
		trail.update(dx * sensitivity.get(), dy * sensitivity.get(), seconds, returnSpeed.get(), half, (int) trailLength.get());
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		update(mc);
		int s = (int) boxSize.get(), c = s / 2;
		if (crosshair.value) {
			int mark = (dotColor.current() & 0xFFFFFF) | 0x40000000;
			g.fill(c - 3, c, c + 4, c + 1, mark);
			g.fill(c, c - 3, c + 1, c + 4, mark);
		}
		List<double[]> pts = trail.points;
		int tc = trailColor.current(), alpha = tc >>> 24;
		for (int i = 1; i < pts.size(); i++) {
			double[] a = pts.get(i - 1), b = pts.get(i);
			int col = ((alpha * i / pts.size()) << 24) | (tc & 0xFFFFFF);
			int steps = (int) Math.ceil(Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])));
			for (int k = 0; k <= steps; k++) {
				double t = steps == 0 ? 1 : (double) k / steps;
				int x = c + (int) Math.round(a[0] + (b[0] - a[0]) * t), y = c + (int) Math.round(a[1] + (b[1] - a[1]) * t);
				g.fill(x, y, x + 1, y + 1, col);
			}
		}
		int x = c + (int) Math.round(trail.x), y = c + (int) Math.round(trail.y);
		g.fill(x - 1, y - 1, x + 2, y + 2, dotColor.current());
	}
}
