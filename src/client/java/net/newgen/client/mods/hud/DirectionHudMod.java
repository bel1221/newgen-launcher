package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class DirectionHudMod extends HudMod {
	// In the order of Minecraft's yaw: 0 = south, 90 = west.
	private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private final Setting.Bool degrees = bool("degrees", "Show degrees", true);
	private final Setting.Num stripWidth = num("width", "Width", 160, 80, 320, 20);
	private final Setting.Color minorColor = color("minor_color", "Colour of NE, SE, SW, NW", 0xFFAAAAAA);
	private final Setting.Bool ticks = bool("ticks", "Tick marks", true);
	private final Setting.Bool fadeEdges = bool("fade_edges", "Fade at the edges", true);
	private final Setting.Bool headings = bool("headings", "Heading numbers on the strip", false);
	// Wave 3 vanilla HUD: markers for the Waypoints mod.
	private final Setting.Bool showWaypoints = bool("show_waypoints", "Show waypoints", true);
	private final Setting.Color waypointColor = color("waypoint_color", "Waypoint marker colour", 0xFFFFD75E);

	public DirectionHudMod() {
		super("direction_hud", "Direction HUD", "Compass strip showing where you face", 0.5f, 0.02f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	private int stripHeight() {
		return 9 + (ticks.value ? 4 : 0);
	}

	@Override
	protected int[] size(Minecraft mc) {
		return mc.player == null ? null : new int[] {(int) stripWidth.get(), stripHeight() + (degrees.value ? 11 : 0)};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		float yaw = mc.player.getYRot();
		int width = (int) stripWidth.get();
		// Every 15 degrees: a letter at the eight directions, else a tick (and maybe the bearing number).
		for (int step = 0; step < 24; step++) {
			int angle = step * 15; // yaw angle
			float off = Mth.wrapDegrees(angle - yaw);
			if (Math.abs(off) > 85) continue;
			int center = width / 2 + Math.round(off * width / 180f);
			double alpha = fadeEdges.value ? Math.min(1, (85 - Math.abs(off)) / 30.0) : 1;
			if (angle % 45 == 0) {
				int i = angle / 45;
				centered(g, mc, NAMES[i], center, 0, HudValues.fade(i % 2 == 0 ? textColor.current() : minorColor.current(), alpha));
			} else if (headings.value && angle % 30 == 0) {
				int bearing = Math.floorMod(angle + 180, 360);
				centered(g, mc, String.valueOf(bearing), center, 0, HudValues.fade(minorColor.current(), alpha * 0.8));
			}
			if (ticks.value) {
				int h = angle % 45 == 0 ? 3 : 2;
				g.fill(center, 10, center + 1, 10 + h, HudValues.fade(minorColor.current(), alpha));
			}
		}
		if (showWaypoints.value) drawWaypoints(g, mc, yaw, width);
		if (ticks.value) g.fill(width / 2, 9, width / 2 + 1, 13, textColor.current()); // where you face
		if (degrees.value) {
			// Compass bearing: north = 0, east = 90.
			int bearing = Math.floorMod(Math.round(yaw + 180), 360);
			centered(g, mc, bearing + "°", width / 2, stripHeight() + 2, textColor.current());
		}
	}

	private void centered(GuiGraphics g, Minecraft mc, String text, int center, int y, int color) {
		if ((color >>> 24) < 5) return; // fully faded: the font would draw it opaque
		g.drawString(mc.font, text, center - mc.font.width(text) / 2, y, color, textShadow.value);
	}

	// Wave 3 vanilla HUD
	/** A small diamond at each waypoint's bearing on the strip (on the tick row, or the letter row without ticks). */
	private void drawWaypoints(GuiGraphics g, Minecraft mc, float yaw, int width) {
		net.newgen.client.mods.utility.WaypointsMod waypoints = net.newgen.client.mods.utility.WaypointsMod.INSTANCE;
		if (waypoints == null) return;
		int cy = ticks.value ? 11 : 4;
		for (net.newgen.client.mods.utility.WaypointsMod.Waypoint w : waypoints.current(mc)) {
			float off = Mth.wrapDegrees(yawTo(w.x - mc.player.getX(), w.z - mc.player.getZ()) - yaw);
			if (Math.abs(off) > 85) continue;
			int cx = width / 2 + Math.round(off * width / 180f);
			int color = HudValues.fade(waypointColor.current(), fadeEdges.value ? Math.min(1, (85 - Math.abs(off)) / 30.0) : 1);
			for (int i = -2; i <= 2; i++) {
				int h = 2 - Math.abs(i);
				g.fill(cx - h, cy + i, cx + h + 1, cy + i + 1, color);
			}
		}
	}

	/** Minecraft yaw (0 = south, 90 = west) that faces a point dx, dz blocks away. */
	static float yawTo(double dx, double dz) {
		return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
	}
}
