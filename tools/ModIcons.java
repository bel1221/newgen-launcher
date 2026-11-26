import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

/**
 * Source of the mod icons in the Mods menu (v2). One system for every icon, the same as the menu's own glyphs:
 * <ul>
 *   <li>a 24 unit design grid with 2 units of padding (the drawing lives in 2..22), front views only;</li>
 *   <li>one stroke weight (2 units), round caps and joins, rounded corners (radius 2 on frames, 1 on small parts);</li>
 *   <li>solid fills only for small accents (a dot, a fill bar), never large blocks; one idea per icon;</li>
 *   <li>families share a base: the screen frame (vanilla HUD parts), the clock face, the bottle, the counter dot.</li>
 * </ul>
 * Rendered anti-aliased at 64 px a tile (like textures/icons.png), sampled smoothly in game.
 * <pre>  java tools/ModIcons.java [contact-sheet.png]</pre>
 * writes assets/newgen_client/textures/mod_icons.png (16 a row) and mod_icons.txt (ids in atlas order).
 */
public class ModIcons {
	static final Path OUT = Path.of("src/main/resources/assets/newgen_client/textures");
	/** The tile being drawn (pixels) and its scale from design units; one atlas per on-screen size. */
	static int TILE = 64;
	static float S = TILE / 24f;
	static final Map<String, Consumer<P>> ICONS = new LinkedHashMap<>();

	/** The pen, in design units (0..24). */
	static final class P {
		final Graphics2D g;

		P(Graphics2D g) {
			this.g = g;
		}

		P line(double x1, double y1, double x2, double y2) {
			g.draw(new Line2D.Double(x1 * S, y1 * S, x2 * S, y2 * S));
			return this;
		}

		/** An open polyline through the points. */
		P path(double... xy) {
			Path2D p = new Path2D.Double();
			p.moveTo(xy[0] * S, xy[1] * S);
			for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i] * S, xy[i + 1] * S);
			g.draw(p);
			return this;
		}

		/** A closed outline through the points. */
		P shape(double... xy) {
			Path2D p = new Path2D.Double();
			p.moveTo(xy[0] * S, xy[1] * S);
			for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i] * S, xy[i + 1] * S);
			p.closePath();
			g.draw(p);
			return this;
		}

		P solid(double... xy) {
			Path2D p = new Path2D.Double();
			p.moveTo(xy[0] * S, xy[1] * S);
			for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i] * S, xy[i + 1] * S);
			p.closePath();
			g.fill(p);
			return this;
		}

		P rect(double x, double y, double w, double h, double r) {
			g.draw(new RoundRectangle2D.Double(x * S, y * S, w * S, h * S, 2 * r * S, 2 * r * S));
			return this;
		}

		P fill(double x, double y, double w, double h, double r) {
			g.fill(new RoundRectangle2D.Double(x * S, y * S, w * S, h * S, 2 * r * S, 2 * r * S));
			return this;
		}

		P circle(double cx, double cy, double r) {
			g.draw(new Ellipse2D.Double((cx - r) * S, (cy - r) * S, 2 * r * S, 2 * r * S));
			return this;
		}

		P disc(double cx, double cy, double r) {
			g.fill(new Ellipse2D.Double((cx - r) * S, (cy - r) * S, 2 * r * S, 2 * r * S));
			return this;
		}

		P dot(double cx, double cy) {
			return disc(cx, cy, 1.25);
		}

		/** An arc of a circle; angles in degrees, 0 = east, counter-clockwise. */
		P arc(double cx, double cy, double r, double start, double extent) {
			g.draw(new Arc2D.Double((cx - r) * S, (cy - r) * S, 2 * r * S, 2 * r * S, start, extent, Arc2D.OPEN));
			return this;
		}

		/** An arrow head at (x, y) pointing along (dx, dy). */
		P head(double x, double y, double dx, double dy) {
			double len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len, s = 3;
			return path(x - ux * s - uy * s, y - uy * s + ux * s, x, y, x - ux * s + uy * s, y - uy * s - ux * s);
		}

		/** Bases shared by families. */
		P screen() { // the vanilla-HUD-part family: a wide screen frame
			return rect(2, 4, 20, 16, 2);
		}

		P clock() { // the clock family
			return circle(12, 12, 9);
		}

		P bottle(double x) { // the potion family: neck, shoulders, round body
			return path(x - 2, 3, x + 2, 3).path(x - 1.5, 3, x - 1.5, 8, x - 6, 14).arc(x, 15.5, 6.2, 195, 150).path(x + 1.5, 3, x + 1.5, 8, x + 6, 14);
		}

		P badge() { // the counter family: a count dot at the bottom right
			return disc(19.5, 19.5, 2.5);
		}

		P sword(double x1, double y1, double x2, double y2) { // blade from hilt (x1, y1) to tip (x2, y2), with a guard
			double dx = x2 - x1, dy = y2 - y1, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
			line(x1, y1, x2, y2);
			double gx = x1 + ux * 3, gy = y1 + uy * 3;
			return line(gx - uy * 3, gy + ux * 3, gx + uy * 3, gy - ux * 3);
		}
	}

	static void icon(String id, Consumer<P> draw) {
		ICONS.put(id, draw);
	}

	static {
		// Keylines (24 grid, live area 2..22): circle 20, square 18, portrait 16x20, landscape 20x16. Stroke 2, round.
		// Square corners mean a Minecraft block; rounded corners mean UI.

		// ---- screen-frame family (vanilla HUD parts): landscape frame 20x16, each mark in its own place
		icon("action_bar", p -> p.screen().line(9, 10, 15, 10).rect(5, 13, 14, 4, 1));
		icon("bossbar", p -> p.screen().line(6, 8, 18, 8).dot(12, 12));
		icon("scoreboard", p -> p.screen().line(15, 4, 15, 20).line(17.5, 9, 19, 9).line(17.5, 12, 19, 12).line(17.5, 15, 19, 15));
		icon("tab", p -> p.screen().line(8, 8.5, 16, 8.5).line(8, 12, 16, 12));
		icon("titles", p -> p.screen().fill(6, 11, 12, 3, 1.5));
		icon("locator_bar", p -> p.screen().line(5, 16.5, 19, 16.5).dot(8, 13.5).dot(12, 13.5).dot(16, 13.5));
		icon("overlay", p -> p.screen().path(12, 9, 14.5, 12.5, 13.5, 17, 10.5, 17, 9.5, 12.5, 12, 9));
		icon("damage_tint", p -> p.screen().solid(2.8, 5, 7, 5, 2.8, 9).solid(21.2, 5, 17, 5, 21.2, 9).solid(2.8, 19, 7, 19, 2.8, 15).solid(21.2, 19, 17, 19, 21.2, 15));
		icon("menu_blur", p -> p.screen().rect(7, 8.5, 10, 7, 1).dot(4.8, 16.8).dot(19.2, 7.2));

		// ---- clock family
		icon("clock", p -> p.clock().path(8.5, 8.5, 12, 12, 15.5, 8.5));
		icon("stopwatch", p -> p.circle(12, 13.5, 8.5).line(10, 2.5, 14, 2.5).line(12, 2.5, 12, 5).line(12, 13.5, 12, 9));
		icon("time_changer", p -> p.clock().line(12, 3, 12, 21).arc(12, 12, 4.5, 90, 180));
		icon("day_counter", p -> p.arc(12, 16, 6, 0, 180).line(2, 16, 22, 16).line(12, 5, 12, 7).line(5.5, 9.5, 7, 11).line(18.5, 9.5, 17, 11).line(4, 20, 20, 20));
		icon("playtime", p -> p.line(6, 3, 18, 3).line(6, 21, 18, 21).path(7, 3, 7, 6, 12, 12, 7, 18, 7, 21).path(17, 3, 17, 6, 12, 12, 17, 18, 17, 21).solid(9.5, 19.5, 14.5, 19.5, 12, 16.5));
		icon("cooldowns", p -> p.arc(12, 12, 9, 90, 270).path(12, 3, 12, 12, 21, 12).dot(12, 12));

		// ---- bottle family
		icon("potion_effects", p -> p.bottle(12).dot(10.5, 16).dot(14, 13.5));
		icon("shiny_pots", p -> p.bottle(10).path(19, 3, 19, 9).path(16, 6, 22, 6));
		icon("potion_counter", p -> p.bottle(10).badge());

		// ---- tag family
		icon("nametag", p -> p.path(9, 6, 21, 6, 21, 18, 9, 18, 3, 12, 9, 6).circle(8.5, 12, 1.2).path(7.5, 11, 4, 7.5, 2.5, 4));
		icon("nick_hider", p -> p.path(9, 6, 21, 6, 21, 18, 9, 18, 3, 12, 9, 6).circle(8.5, 12, 1.2).line(5, 21, 21, 3));

		// ---- HUD
		icon("fps", p -> p.arc(12, 16, 9, 0, 180).line(12, 16, 17, 10).dot(12, 16).line(5.5, 9.5, 6.8, 10.8).line(12, 7, 12, 8.8).line(18.5, 9.5, 17.2, 10.8));
		icon("cps", p -> p.rect(6, 7, 12, 15, 6).line(12, 7, 12, 13).line(6, 13, 18, 13).line(4.5, 2.5, 6, 4.5).line(9, 1.5, 9.5, 3.5));
		icon("coordinates", p -> p.path(3, 4, 3, 21, 20, 21).circle(14, 8, 3.5).path(11, 10.5, 14, 15.5, 17, 10.5).dot(14, 8));
		icon("keystrokes", p -> p.rect(9, 3, 6, 6, 1.5).rect(2, 11, 6, 6, 1.5).rect(9, 11, 6, 6, 1.5).rect(16, 11, 6, 6, 1.5));
		icon("armor_status", p -> p.path(4, 3, 8, 3, 8, 6, 9.5, 7.5, 14.5, 7.5, 16, 6, 16, 3, 20, 3, 20, 10, 18, 11, 18, 21, 6, 21, 6, 11, 4, 10, 4, 3).line(12, 9, 12, 18));
		icon("attack_indicator", p -> p.sword(5, 14, 17, 3).rect(3, 17.5, 18, 4, 2).fill(3, 17.5, 9, 4, 2));
		icon("combo", p -> p.path(3, 9, 6, 12, 3, 15).path(8, 7, 12, 12, 8, 17).path(14, 4, 20, 12, 14, 20));
		icon("direction_hud", p -> p.clock().line(12, 1.5, 12, 3).shape(12, 6, 14.5, 12, 12, 18, 9.5, 12).solid(12, 6, 14.5, 12, 9.5, 12));
		icon("f3_display", p -> p.rect(3, 4, 18, 16, 3).path(7.5, 15.5, 7.5, 8.5, 11, 8.5).line(7.5, 12, 10, 12).path(13, 8.5, 16.5, 8.5, 14.5, 11.5, 16.5, 13, 15.5, 15.5, 13, 15.5));
		icon("item_counter", p -> p.rect(3, 5, 16, 14, 0).line(3, 10, 19, 10).fill(10, 9, 2, 3, 0.5).badge());
		icon("item_tracker", p -> p.rect(7, 3, 8, 8, 0).path(3, 14, 3, 19, 21, 19, 21, 14).line(11, 11.5, 11, 15.5).head(11, 15.5, 0, 1).path(19, 4, 19, 8).path(17, 6, 21, 6));
		icon("memory", p -> p.rect(2, 7, 20, 9, 2).line(11, 16, 11, 14).line(13, 16, 13, 14).line(5, 16, 5, 19).line(9, 16, 9, 19).line(15, 16, 15, 19).line(19, 16, 19, 19));
		icon("momentum", p -> p.path(11, 4, 11, 14, 18, 16, 21, 18, 21, 20, 8, 20, 8, 4).line(2, 10, 6, 10).line(3, 14, 6, 14).line(2, 18, 5, 18));
		icon("pack_display", p -> p.rect(6, 2, 16, 16, 2).rect(2, 6, 16, 16, 2).path(5, 19, 9, 14, 12, 17, 13.5, 15.5, 15, 19));
		icon("ping", p -> p.fill(3, 15, 2.5, 5, 1).fill(8, 11, 2.5, 9, 1).rect(13, 7, 2.5, 13, 1).rect(18, 3, 2.5, 17, 1));
		icon("pvp_info", p -> p.sword(4, 20, 19, 5).sword(20, 20, 5, 5));
		icon("reach_display", p -> p.sword(3, 9, 21, 9).line(3, 16, 21, 16).line(3, 14, 3, 18).line(21, 14, 21, 18));
		icon("saturation", p -> p.shape(9.5, 14.5, 9, 9.5, 11.5, 5, 16, 3.5, 20, 6, 20.5, 10.5, 17, 14, 12.5, 15).line(10.5, 13.5, 6, 18).circle(4.8, 17.2, 1.4).circle(6.8, 19.2, 1.4));
		icon("server_address", p -> p.rect(6, 2, 12, 20, 2).line(9, 7, 15, 7).line(9, 11, 15, 11).line(9, 15, 15, 15).dot(12, 19));
		icon("totem_counter", p -> p.rect(5, 2, 8, 7, 1).dot(7.5, 5).dot(10.5, 5).path(5, 11, 2, 11).path(13, 11, 16, 11).path(5, 9.5, 5, 13, 6, 21, 12, 21, 13, 13, 13, 9.5).disc(19.5, 19.5, 1.6));
		icon("view_angles", p -> p.path(2, 15, 6, 11, 12, 11, 16, 15, 12, 19, 6, 19, 2, 15).dot(9, 15).arc(9, 15, 12, 20, 70).line(19.5, 10.5, 21, 9));
		icon("waila", p -> p.rect(2, 9, 12, 12, 0).path(14, 3, 22, 3, 22, 10, 17, 10, 14, 12, 14, 3).path(16.5, 5.5, 17, 4.8, 18.5, 4.8, 19.5, 5.8, 18, 7, 18, 7.5).dot(18, 8.8));
		icon("height_limit", p -> p.line(3, 3, 21, 3).line(12, 21, 12, 7).head(12, 7, 0, -1));
		icon("tnt_countdown", p -> p.rect(3, 6, 18, 16, 0).line(3, 12, 21, 12).line(3, 16, 21, 16).path(12, 6, 12, 4, 14, 2.5).dot(16, 2));
		icon("horse_stats", p -> p.path(8, 21, 8, 13, 4.5, 10, 8, 3, 14.5, 4, 19, 10, 17.5, 12.5, 13, 9.5, 14, 21));

		// ---- Visual
		icon("animatium", p -> p.line(6, 18, 19, 5).line(5, 14, 10, 19).line(6, 18, 3.5, 20.5).dot(3, 21).arc(6, 18, 15, 52, 10).arc(6, 18, 15, 67, 10).arc(6, 18, 15, 82, 8));
		icon("block_outline", p -> p.path(3, 8, 3, 3, 8, 3).path(16, 3, 21, 3, 21, 8).path(21, 16, 21, 21, 16, 21).path(8, 21, 3, 21, 3, 16));
		icon("chunk_borders", p -> { for (int k = 0; k < 4; k++) { double o = 3 + k * 4.5; p.line(o, 3, o + 2, 3).line(o, 21, o + 2, 21).line(3, o, 3, o + 2).line(21, o, 21, o + 2); }
			p.line(12, 7, 12, 17).line(7, 12, 17, 12); });
		icon("crosshair", p -> p.line(12, 3, 12, 8).line(12, 16, 12, 21).line(3, 12, 8, 12).line(16, 12, 21, 12).dot(12, 12));
		icon("fog", p -> p.path(3, 7, 6, 5.5, 9, 7, 12, 5.5, 15, 7, 18, 5.5, 21, 7).path(3, 12, 6, 10.5, 9, 12, 12, 10.5, 15, 12, 18, 10.5, 21, 12).path(3, 17, 6, 15.5, 9, 17, 12, 15.5, 15, 17, 18, 15.5, 21, 17));
		icon("fov", p -> p.line(5, 12, 21, 4).line(5, 12, 21, 20).arc(5, 12, 11, -26, 52).path(2, 12, 3.5, 10.5, 5, 12, 3.5, 13.5, 2, 12));
		icon("gui_scale", p -> p.rect(3, 3, 18, 18, 2).rect(3, 13, 8, 8, 1).line(13, 11, 18, 6).head(18, 6, 1, -1));
		icon("hit_color", p -> p.circle(8, 6, 2.5).path(8, 9, 8, 15).path(4, 12, 12, 12).path(8, 15, 5, 21).path(8, 15, 11, 21).path(18, 9, 18.6, 11.4, 21, 12, 18.6, 12.6, 18, 15, 17.4, 12.6, 15, 12, 17.4, 11.4, 18, 9));
		icon("hitbox", p -> { for (int k = 0; k < 3; k++) { double o = 3 + k * 7; p.line(o, 2, o + 3, 2).line(o, 22, o + 3, 22); }
			p.line(4, 5, 4, 8).line(4, 11, 4, 14).line(4, 17, 4, 20).line(20, 5, 20, 8).line(20, 11, 20, 14).line(20, 17, 20, 20)
					.circle(12, 7, 2.5).path(12, 10, 12, 15).path(8.5, 12, 15.5, 12).path(12, 15, 9.5, 19.5).path(12, 15, 14.5, 19.5); });
		icon("hurt_cam", p -> p.rect(5, 7, 14, 12, 2).circle(12, 13, 3).path(9, 7, 10, 4.5, 14, 4.5, 15, 7).line(2, 10, 2, 16).line(22, 10, 22, 16));
		icon("item_customizer", p -> p.line(10, 2, 10, 21).line(7, 5.5, 13, 5.5).rect(5, 10, 10, 7, 2).arc(6.8, 10, 1.3, 0, 180).arc(9.4, 10, 1.3, 0, 180).arc(12, 10, 1.3, 0, 180)
				.line(16, 20, 22, 20).head(16, 20, -1, 0).head(22, 20, 1, 0));
		icon("light_overlay", p -> p.rect(3, 8, 18, 13, 0).line(8, 11.5, 16, 17.5).line(16, 11.5, 8, 17.5));
		icon("lighting", p -> p.circle(12, 12, 4.5).line(12, 2, 12, 4.5).line(12, 19.5, 12, 22).line(2, 12, 4.5, 12).line(19.5, 12, 22, 12)
				.line(4.9, 4.9, 6.7, 6.7).line(17.3, 17.3, 19.1, 19.1).line(19.1, 4.9, 17.3, 6.7).line(4.9, 19.1, 6.7, 17.3));
		icon("particle_changer", p -> p.dot(12, 12).dot(12, 4).dot(19.6, 9.5).dot(16.7, 18.5).dot(7.3, 18.5).dot(4.4, 9.5));
		icon("shields", p -> p.path(3, 7, 15, 7, 15, 14, 9, 21, 3, 14, 3, 7).line(9, 7, 9, 21).line(19, 3, 19, 10).head(19, 10, 0, 1));
		icon("weather_changer", p -> p.path(6, 15, 4, 13, 4.5, 10, 7.5, 9).arc(12, 9, 5, 0, 180).path(17, 9, 20, 10, 20.5, 13, 18, 15).line(6, 15, 18, 15).line(8, 18, 7, 21).line(12, 18, 11, 21).line(16, 18, 15, 21));
		icon("zoom", p -> p.circle(10, 10, 7.5).line(15.5, 15.5, 21, 21).line(7, 10, 13, 10).line(10, 7, 10, 13));

		// ---- Controls
		icon("freelook", p -> p.path(4, 12, 8, 8, 16, 8, 20, 12, 16, 16, 8, 16, 4, 12).circle(12, 12, 2).arc(12, 12, 10, 20, 140).head(3.4, 7, -0.6, 1));
		icon("snaplook", p -> p.circle(12, 12, 4).arc(12, 12, 9, 30, 120).head(19.8, 7.5, 0.6, 1).arc(12, 12, 9, 210, 120).head(4.2, 16.5, -0.6, -1));
		icon("toggle_sneak", p -> p.circle(9, 4, 2).path(9, 7, 8, 13, 4, 17).path(8, 13, 11, 16, 11, 20).path(4, 10, 9, 8.5, 12, 11).rect(14, 15, 8, 5, 2.5).dot(19.5, 17.5));
		icon("auto_text_hotkey", p -> p.rect(2, 6, 14, 14, 3).line(2, 16, 16, 16).path(13, 2, 22, 2, 22, 8, 17, 8, 15, 10, 15, 8));
		icon("auto_text_actions", p -> p.path(3, 4, 21, 4, 21, 16, 10, 16, 6, 20, 6, 16, 3, 16, 3, 4).path(13, 6.5, 10, 10.5, 14, 10.5, 11, 14));
		icon("emotes", p -> p.circle(10, 5, 2.5).path(10, 8, 10, 15).path(10, 10, 6, 13).path(10, 10, 15, 6).path(15, 6, 16, 2.5).path(10, 15, 7, 21).path(10, 15, 13, 21));

		// ---- Utility
		icon("chat", p -> p.path(2, 3, 15, 3, 15, 12, 7, 12, 4, 15, 4, 12, 2, 12, 2, 3).path(17, 7, 22, 7, 22, 16, 20, 16, 20, 19, 17, 16, 10, 16, 10, 14));
		icon("inventory_mod", p -> p.rect(2, 2, 14, 14, 0).line(6.7, 2, 6.7, 16).line(11.3, 2, 11.3, 16).line(2, 6.7, 16, 6.7).line(2, 11.3, 16, 11.3)
				.shape(14, 11, 14, 22, 16.5, 19.5, 19, 22.5, 20.5, 21.5, 18.5, 18.5, 22, 18, 14, 11));
		icon("screenshot", p -> p.rect(2, 6, 20, 14, 2).circle(12, 13, 3.5).path(8, 6, 9.5, 3.5, 14.5, 3.5, 16, 6).dot(18.5, 9));
		icon("scrollable_tooltips", p -> p.rect(2, 4, 14, 16, 2).line(5.5, 9, 12.5, 9).line(5.5, 13, 10.5, 13).line(20, 6, 20, 18).head(20, 6, 0, -1).head(20, 18, 0, 1));
		icon("shulker_preview", p -> p.rect(3, 3, 18, 6, 1).path(3, 11, 3, 21, 21, 21, 21, 11).circle(12, 14.5, 2).dot(12, 14.5));
		icon("waypoints", p -> p.line(6, 3, 6, 21).shape(6, 4, 19, 7.5, 6, 11).line(3, 21, 9, 21));
		icon("minimap", p -> p.path(3, 5, 9, 3, 15, 5, 21, 3, 21, 19, 15, 21, 9, 19, 3, 21, 3, 5).line(9, 3, 9, 19).line(15, 5, 15, 21).solid(12, 9, 14, 14, 12, 13, 10, 14));
		icon("theme", p -> p.arc(12, 12, 9, 45, 280).path(18.4, 18.4, 15.5, 16, 16.5, 13, 19.5, 12.5, 21, 12).dot(8, 9).dot(12, 6.5).dot(16, 9));
	}

	public static void main(String[] args) throws Exception {
		List<String> ids = new ArrayList<>(ICONS.keySet());
		// One atlas per on-screen size: 16 units (cards) and 14 (headers) at GUI scales 1-4, each drawn at its exact pixel
		// size, so the game shows every icon 1:1 with no resampling.
		for (int unit : new int[]{16, 14}) for (int scale = 1; scale <= 4; scale++) write(ids, unit * scale);
		Files.write(OUT.resolve("mod_icons.txt"), ids);
		System.out.println(ids.size() + " icons");
		if (args.length > 0) { TILE = 64; S = TILE / 24f; ImageIO.write(draw(ids), "png", new File(args[0])); }
	}

	static void write(List<String> ids, int tile) throws Exception {
		TILE = tile;
		S = TILE / 24f;
		ImageIO.write(draw(ids), "png", OUT.resolve("mod_icons_" + tile + ".png").toFile());
	}

	static BufferedImage draw(List<String> ids) {
		int cols = 16, rows = (ids.size() + cols - 1) / cols;
		BufferedImage atlas = new BufferedImage(cols * TILE, rows * TILE, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < ids.size(); i++) {
			Graphics2D t = atlas.createGraphics();
			t.translate((i % cols) * TILE, (i / cols) * TILE);
			t.setClip(0, 0, TILE, TILE);
			t.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			t.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			t.setColor(Color.WHITE);
			t.setStroke(new BasicStroke(2 * S, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			ICONS.get(ids.get(i)).accept(new P(t));
			t.dispose();
		}
		return atlas;
	}
}
