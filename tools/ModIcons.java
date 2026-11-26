import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
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
 * Source of the mod icons in the Mods menu: one 16x16 white pixel icon per mod, drawn here from simple shapes on a
 * 16 px grid (no anti-aliasing, so they stay crisp when the game scales them by whole numbers). Run from the project
 * folder:
 * <pre>  java tools/ModIcons.java</pre>
 * It writes assets/newgen_client/textures/mod_icons.png (16 icons a row) and mod_icons.txt (the mod ids in atlas
 * order). To change an icon, edit its entry below and run it again. Coordinates are pixels 0..15, y down.
 */
public class ModIcons {
	static final Path OUT = Path.of("src/main/resources/assets/newgen_client/textures");
	static final Map<String, Consumer<P>> ICONS = new LinkedHashMap<>();

	/** The pen: every call draws white pixels on the 16x16 tile. */
	static final class P {
		final Graphics2D g;

		P(Graphics2D g) {
			this.g = g;
		}

		P line(int x1, int y1, int x2, int y2) {
			g.drawLine(x1, y1, x2, y2);
			return this;
		}

		P rect(int x, int y, int w, int h) { // outline, w x h pixels including the border
			g.drawRect(x, y, w - 1, h - 1);
			return this;
		}

		P fill(int x, int y, int w, int h) {
			g.fillRect(x, y, w, h);
			return this;
		}

		P dot(int x, int y) {
			return fill(x, y, 1, 1);
		}

		P oval(int x, int y, int w, int h) {
			g.drawOval(x, y, w - 1, h - 1);
			return this;
		}

		P disc(int x, int y, int w, int h) {
			g.fillOval(x, y, w, h);
			return this;
		}

		P poly(int... xy) {
			Polygon p = new Polygon();
			for (int i = 0; i < xy.length; i += 2) p.addPoint(xy[i], xy[i + 1]);
			g.drawPolygon(p);
			return this;
		}

		P arc(int x, int y, int w, int h, int start, int extent) {
			g.drawArc(x, y, w - 1, h - 1, start, extent);
			return this;
		}

		/** Text in a 3x5 pixel font (the few letters and digits the icons need), 4 px per character. */
		P text(int x, int y, String s) {
			for (char c : s.toCharArray()) {
				String[] rows = FONT.get(c);
				for (int r = 0; r < 5; r++) for (int k = 0; k < 3; k++) if (rows[r].charAt(k) == '#') dot(x + k, y + r);
				x += 4;
			}
			return this;
		}
	}

	static final Map<Character, String[]> FONT = Map.of(
			'F', new String[]{"###", "#..", "##.", "#..", "#.."},
			'3', new String[]{"##.", "..#", ".#.", "..#", "##."},
			'?', new String[]{"##.", "..#", ".#.", "...", ".#."},
			'1', new String[]{".#.", "##.", ".#.", ".#.", "###"},
			'8', new String[]{"###", "#.#", "###", "#.#", "###"},
			'.', new String[]{"...", "...", "...", "...", ".#."});

	static void icon(String id, Consumer<P> draw) {
		ICONS.put(id, draw);
	}

	static {
		// HUD
		icon("fps", p -> p.arc(1, 3, 14, 14, 0, 180).line(1, 10, 14, 10).line(8, 10, 12, 5).dot(8, 10).dot(3, 7).dot(12, 7));
		icon("cps", p -> p.rect(4, 3, 8, 11).line(8, 3, 8, 7).line(4, 7, 11, 7).line(1, 1, 3, 3).line(14, 1, 12, 3).line(8, 0, 8, 1));
		icon("coordinates", p -> p.line(3, 1, 3, 12).line(3, 12, 14, 12).line(3, 12, 0, 15).line(2, 2, 3, 1).line(4, 2, 3, 1).line(13, 11, 14, 12).line(13, 13, 14, 12));
		icon("action_bar", p -> p.rect(1, 3, 14, 10).line(5, 7, 10, 7).fill(3, 9, 2, 2).fill(6, 9, 2, 2).fill(9, 9, 2, 2).fill(12, 9, 1, 2));
		icon("armor_status", p -> p.poly(2, 2, 5, 1, 6, 3, 9, 3, 10, 1, 13, 2, 13, 6, 11, 6, 11, 14, 4, 14, 4, 6, 2, 6));
		icon("attack_indicator", p -> p.line(2, 12, 11, 3).line(11, 3, 13, 1).line(4, 8, 7, 11).line(1, 13, 2, 12).line(0, 15, 15, 15).fill(0, 14, 9, 1));
		icon("bossbar", p -> p.rect(1, 3, 14, 10).fill(4, 5, 8, 1).dot(8, 7));
		icon("clock", p -> p.oval(1, 1, 14, 14).line(8, 8, 8, 4).line(8, 8, 11, 9));
		icon("combo", p -> p.line(1, 4, 5, 8).line(5, 8, 1, 12).line(6, 4, 10, 8).line(10, 8, 6, 12).line(11, 4, 15, 8).line(15, 8, 11, 12));
		icon("cooldowns", p -> p.line(3, 1, 12, 1).line(3, 14, 12, 14).line(4, 2, 7, 7).line(11, 2, 8, 7).line(7, 8, 4, 13).line(8, 8, 11, 13).fill(6, 11, 4, 2));
		icon("day_counter", p -> p.disc(5, 5, 6, 6).line(8, 0, 8, 2).line(8, 13, 8, 15).line(0, 8, 2, 8).line(13, 8, 15, 8).line(2, 2, 3, 3).line(12, 12, 13, 13).line(13, 2, 12, 3).line(2, 13, 3, 12));
		icon("direction_hud", p -> p.oval(1, 1, 14, 14).poly(8, 3, 10, 8, 8, 12, 6, 8).fill(7, 4, 2, 4));
		icon("f3_display", p -> p.rect(0, 2, 16, 12).text(4, 5, "F3"));
		icon("item_counter", p -> p.rect(2, 2, 10, 10).line(2, 5, 11, 5).disc(11, 11, 4, 4));
		icon("item_tracker", p -> p.rect(2, 8, 12, 7).line(2, 8, 5, 5).line(13, 8, 10, 5).line(8, 0, 8, 5).line(6, 3, 8, 5).line(10, 3, 8, 5));
		icon("keystrokes", p -> p.rect(5, 1, 6, 6).rect(0, 8, 5, 6).rect(5, 8, 6, 6).rect(11, 8, 5, 6).dot(7, 3).dot(8, 3));
		icon("memory", p -> p.rect(1, 4, 14, 8).fill(3, 6, 3, 4).fill(7, 6, 3, 4).line(2, 2, 2, 3).line(5, 2, 5, 3).line(8, 2, 8, 3).line(11, 2, 11, 3).line(13, 2, 13, 3).line(2, 12, 2, 13).line(5, 12, 5, 13).line(8, 12, 8, 13).line(11, 12, 11, 13).line(13, 12, 13, 13));
		icon("momentum", p -> p.line(5, 8, 14, 8).line(14, 8, 11, 5).line(14, 8, 11, 11).line(0, 5, 3, 5).line(1, 8, 3, 8).line(0, 11, 3, 11));
		icon("pack_display", p -> p.rect(1, 5, 10, 10).rect(5, 1, 10, 10).fill(6, 6, 4, 4));
		icon("ping", p -> p.fill(1, 12, 3, 3).fill(5, 9, 3, 6).fill(9, 6, 3, 9).rect(13, 2, 3, 13));
		icon("playtime", p -> p.oval(1, 1, 14, 14).poly(6, 5, 11, 8, 6, 11));
		icon("potion_counter", p -> p.rect(5, 1, 4, 3).poly(5, 4, 1, 9, 1, 14, 12, 14, 12, 9, 8, 4).fill(2, 10, 10, 4).dot(14, 2).dot(14, 4).line(13, 3, 15, 3));
		icon("potion_effects", p -> p.rect(6, 1, 4, 3).poly(6, 4, 2, 9, 2, 14, 13, 14, 13, 9, 9, 4).dot(5, 11).dot(8, 9).dot(10, 12).dot(7, 12));
		icon("pvp_info", p -> p.line(2, 2, 12, 12).line(13, 2, 3, 12).line(1, 10, 5, 14).line(14, 10, 10, 14).line(0, 15, 2, 13).line(15, 15, 13, 13));
		icon("reach_display", p -> p.line(1, 3, 1, 12).line(14, 3, 14, 12).line(3, 8, 12, 8).line(3, 8, 5, 6).line(3, 8, 5, 10).line(12, 8, 10, 6).line(12, 8, 10, 10));
		icon("saturation", p -> p.disc(4, 1, 10, 10).line(5, 10, 2, 13).line(2, 13, 1, 12).line(2, 13, 3, 15));
		icon("server_address", p -> p.rect(1, 1, 14, 5).rect(1, 7, 14, 5).dot(3, 3).dot(3, 9).line(9, 3, 12, 3).line(9, 9, 12, 9).line(8, 12, 8, 15).line(3, 15, 13, 15));
		icon("stopwatch", p -> p.oval(1, 3, 14, 13).line(6, 0, 9, 0).line(8, 0, 8, 3).line(8, 9, 8, 6).line(13, 3, 14, 4));
		icon("totem_counter", p -> p.rect(3, 0, 7, 5).dot(5, 2).dot(7, 2).line(0, 6, 12, 6).rect(4, 7, 5, 6).disc(12, 12, 4, 4));
		icon("view_angles", p -> p.line(1, 14, 14, 14).line(1, 14, 12, 3).arc(-5, 8, 14, 14, 0, 45).dot(14, 2));
		icon("waila", p -> p.rect(1, 4, 9, 9).line(10, 3, 15, 3).line(15, 3, 15, 8).line(10, 8, 15, 8).text(11, 4, "?").line(10, 3, 10, 8));
		icon("keep_cursor", p -> {}); // unused id guard
		icon("height_limit", p -> p.line(0, 1, 15, 1).line(8, 15, 8, 3).line(8, 3, 5, 6).line(8, 3, 11, 6).line(0, 3, 0, 1).line(15, 3, 15, 1));
		icon("horse_stats", p -> p.arc(2, 1, 12, 14, 180, -180).line(2, 8, 2, 14).line(13, 8, 13, 14).line(5, 8, 5, 14).line(10, 8, 10, 14).dot(3, 11).dot(12, 11));
		icon("locator_bar", p -> p.rect(1, 3, 14, 10).line(3, 10, 12, 10).dot(4, 9).dot(8, 9).dot(11, 9));
		icon("minimap", p -> p.rect(0, 0, 16, 16).poly(8, 4, 11, 11, 8, 9, 5, 11).line(2, 2, 4, 2).line(12, 13, 13, 13));
		icon("scoreboard", p -> p.rect(1, 3, 14, 10).line(10, 5, 12, 5).line(10, 7, 12, 7).line(10, 9, 12, 9).line(9, 4, 9, 11));
		icon("tab", p -> p.rect(1, 3, 14, 10).line(5, 5, 10, 5).line(5, 7, 10, 7).line(5, 9, 10, 9));
		icon("titles", p -> p.rect(1, 3, 14, 10).fill(4, 7, 8, 2));
		icon("tnt_countdown", p -> p.rect(1, 4, 14, 12).fill(1, 8, 14, 4).line(8, 4, 8, 2).line(8, 2, 10, 0).dot(11, 0));
		icon("emotes", p -> p.oval(5, 0, 5, 5).line(7, 5, 7, 10).line(7, 6, 12, 2).line(7, 7, 3, 9).line(7, 10, 4, 15).line(7, 10, 10, 15));
		icon("waypoints", p -> p.line(4, 1, 4, 15).poly(5, 1, 13, 4, 5, 7).line(2, 15, 7, 15));
		// Visual
		icon("animatium", p -> p.line(2, 9, 8, 3).line(8, 3, 9, 2).line(3, 6, 6, 9).line(1, 10, 2, 9).text(4, 11, "1.8"));
		icon("block_outline", p -> p.line(2, 2, 5, 2).line(2, 2, 2, 5).line(13, 2, 10, 2).line(13, 2, 13, 5).line(2, 13, 5, 13).line(2, 13, 2, 10).line(13, 13, 10, 13).line(13, 13, 13, 10));
		icon("chunk_borders", p -> p.rect(0, 0, 16, 16).line(5, 0, 5, 15).line(10, 0, 10, 15).line(0, 5, 15, 5).line(0, 10, 15, 10));
		icon("crosshair", p -> p.line(8, 1, 8, 5).line(8, 10, 8, 14).line(1, 8, 5, 8).line(10, 8, 14, 8).dot(8, 8));
		icon("damage_tint", p -> p.poly(8, 14, 1, 7, 1, 3, 4, 1, 8, 4, 11, 1, 14, 3, 14, 7).fill(2, 4, 6, 3).fill(3, 7, 5, 2).fill(5, 9, 3, 2));
		icon("fog", p -> p.line(1, 4, 5, 3).line(5, 3, 10, 5).line(10, 5, 14, 4).line(1, 8, 5, 7).line(5, 7, 10, 9).line(10, 9, 14, 8).line(1, 12, 5, 11).line(5, 11, 10, 13).line(10, 13, 14, 12));
		icon("fov", p -> p.line(1, 8, 14, 1).line(1, 8, 14, 15).arc(4, 3, 10, 10, -40, 80).disc(0, 7, 3, 3));
		icon("freelook", p -> p.arc(1, 4, 14, 9, 0, 180).arc(1, 3, 14, 9, 180, 180).disc(6, 6, 4, 4).arc(0, 0, 16, 16, 30, 80).line(13, 1, 14, 3));
		icon("gui_scale", p -> p.rect(0, 0, 16, 16).rect(0, 9, 7, 7).line(8, 8, 14, 2).line(14, 2, 10, 2).line(14, 2, 14, 6));
		icon("hit_color", p -> p.line(1, 14, 9, 6).line(9, 6, 11, 4).line(3, 9, 6, 12).line(12, 1, 12, 3).line(14, 4, 15, 4).line(13, 2, 14, 1).line(10, 1, 10, 2));
		icon("hitbox", p -> p.line(1, 1, 3, 1).line(6, 1, 9, 1).line(12, 1, 14, 1).line(1, 14, 3, 14).line(6, 14, 9, 14).line(12, 14, 14, 14).line(1, 1, 1, 3).line(1, 6, 1, 9).line(1, 12, 1, 14).line(14, 1, 14, 3).line(14, 6, 14, 9).line(14, 12, 14, 14).fill(6, 4, 4, 4).fill(7, 8, 2, 4));
		icon("hurt_cam", p -> p.rect(0, 4, 16, 10).rect(6, 6, 5, 5).fill(2, 2, 4, 2).line(11, 4, 9, 8).line(9, 8, 12, 10).line(12, 10, 10, 13));
		icon("item_customizer", p -> p.line(1, 14, 9, 6).line(9, 6, 11, 4).line(3, 9, 6, 12).line(12, 8, 15, 8).line(12, 12, 15, 12).dot(13, 7).dot(13, 9).dot(14, 11).dot(14, 13));
		icon("light_overlay", p -> p.fill(7, 6, 2, 9).disc(5, 1, 6, 6).line(1, 3, 2, 3).line(13, 3, 14, 3).line(3, 0, 4, 1).line(12, 0, 11, 1));
		icon("lighting", p -> p.oval(3, 0, 10, 10).line(6, 10, 6, 12).line(9, 10, 9, 12).line(6, 13, 9, 13).line(7, 15, 8, 15).line(6, 4, 8, 7).line(10, 4, 8, 7));
		icon("menu_blur", p -> { for (int y = 0; y < 16; y += 2) for (int x = (y / 2) % 2; x < 16; x += 2) p.dot(x, y); p.rect(3, 3, 10, 10); });
		icon("nametag", p -> p.poly(3, 3, 15, 3, 15, 12, 3, 12, 0, 8).disc(3, 6, 3, 3).line(8, 6, 13, 6).line(8, 9, 12, 9));
		icon("nick_hider", p -> p.poly(3, 3, 15, 3, 15, 12, 3, 12, 0, 8).line(7, 7, 13, 7).line(1, 15, 15, 0));
		icon("overlay", p -> p.rect(1, 3, 14, 10).poly(8, 6, 10, 9, 9, 11, 7, 11, 6, 9).dot(8, 9));
		icon("particle_changer", p -> p.dot(3, 2).dot(12, 3).dot(7, 6).dot(2, 9).dot(10, 10).dot(5, 13).dot(14, 13).line(8, 0, 8, 2).line(7, 1, 9, 1).line(13, 7, 13, 9).line(12, 8, 14, 8).line(4, 11, 6, 11).line(5, 10, 5, 12));
		icon("shields", p -> p.poly(2, 1, 13, 1, 13, 8, 8, 14, 2, 8).line(8, 1, 8, 14).line(2, 6, 13, 6));
		icon("shiny_pots", p -> p.rect(4, 1, 4, 3).poly(4, 4, 1, 9, 1, 14, 11, 14, 11, 9, 7, 4).line(13, 5, 13, 9).line(11, 7, 15, 7).dot(14, 1).dot(12, 12));
		icon("time_changer", p -> p.arc(0, 0, 16, 16, 90, 180).line(8, 0, 8, 15).disc(9, 4, 7, 8).line(12, 0, 12, 2).line(15, 2, 14, 3));
		icon("weather_changer", p -> p.arc(0, 2, 8, 8, 90, 180).arc(3, 0, 10, 9, 0, 180).arc(8, 3, 8, 7, -90, 180).line(3, 9, 12, 9).line(4, 12, 3, 14).line(8, 12, 7, 14).line(12, 12, 11, 14));
		icon("zoom", p -> p.oval(0, 0, 11, 11).line(9, 9, 14, 14).line(10, 9, 15, 14).line(3, 5, 7, 5).line(5, 3, 5, 7));
		icon("hit_color_alt", p -> {});
		// Controls
		icon("freelook_unused", p -> {});
		icon("snaplook", p -> p.arc(1, 4, 14, 9, 0, 180).arc(1, 3, 14, 9, 180, 180).disc(6, 6, 4, 4).line(1, 1, 5, 1).line(1, 1, 3, 0).line(1, 1, 3, 3));
		icon("toggle_sneak", p -> p.rect(0, 4, 16, 8).disc(9, 6, 4, 4).line(3, 8, 5, 8));
		icon("auto_text_hotkey", p -> p.rect(1, 3, 14, 11).line(1, 11, 14, 11).line(4, 6, 11, 6).line(4, 8, 9, 8));
		icon("auto_text_actions", p -> p.rect(0, 1, 16, 10).line(3, 11, 3, 14).line(3, 14, 6, 11).line(9, 3, 6, 7).line(6, 7, 10, 7).line(10, 7, 7, 10));
		// Utility
		icon("chat", p -> p.rect(0, 1, 16, 10).line(3, 11, 3, 14).line(3, 14, 6, 11).line(3, 4, 12, 4).line(3, 7, 9, 7));
		icon("inventory_mod", p -> p.rect(0, 0, 16, 16).line(4, 3, 4, 12).line(4, 3, 11, 10).line(11, 10, 8, 10).line(8, 10, 10, 13).line(6, 11, 4, 12));
		icon("screenshot", p -> p.rect(0, 4, 16, 10).oval(5, 6, 6, 6).fill(2, 2, 4, 2).dot(13, 6));
		icon("scrollable_tooltips", p -> p.rect(0, 2, 12, 12).line(2, 5, 9, 5).line(2, 8, 7, 8).line(2, 11, 9, 11).line(14, 3, 14, 13).line(13, 4, 15, 4).line(13, 12, 15, 12));
		icon("shulker_preview", p -> p.rect(1, 6, 14, 9).poly(1, 6, 3, 1, 13, 1, 15, 6).fill(6, 8, 4, 3).line(1, 10, 14, 10));
		icon("stopwatch_unused", p -> {});
		icon("theme", p -> p.oval(1, 1, 14, 14).dot(5, 5).dot(8, 4).dot(11, 6).disc(9, 9, 3, 3));
	}

	public static void main(String[] args) throws Exception {
		List<String> ids = new ArrayList<>();
		for (var e : ICONS.entrySet()) if (!e.getKey().endsWith("_unused") && !e.getKey().endsWith("_alt") && !e.getKey().equals("keep_cursor")) ids.add(e.getKey());
		int cols = 16, rows = (ids.size() + cols - 1) / cols;
		BufferedImage atlas = new BufferedImage(cols * 16, rows * 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = atlas.createGraphics();
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(1));
		for (int i = 0; i < ids.size(); i++) {
			Graphics2D t = (Graphics2D) g.create((i % cols) * 16, (i / cols) * 16, 16, 16);
			t.setColor(Color.WHITE);
			ICONS.get(ids.get(i)).accept(new P(t));
			t.dispose();
		}
		g.dispose();
		ImageIO.write(atlas, "png", OUT.resolve("mod_icons.png").toFile());
		Files.write(OUT.resolve("mod_icons.txt"), ids);
		System.out.println(ids.size() + " icons");
		if (args.length > 0) contactSheet(atlas, ids, new File(args[0]));
	}

	/** All icons at 4x on a dark ground with their ids, to look at without the game. */
	static void contactSheet(BufferedImage atlas, List<String> ids, File out) throws Exception {
		int cols = 8, cell = 150, rowsN = (ids.size() + cols - 1) / cols;
		BufferedImage img = new BufferedImage(cols * cell, rowsN * 96, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0x1E2126));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(new Color(0xB0B5BD));
		g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
		for (int i = 0; i < ids.size(); i++) {
			int x = (i % cols) * cell, y = (i / cols) * 96, ax = (i % 16) * 16, ay = (i / 16) * 16;
			g.drawImage(atlas, x + (cell - 64) / 2, y + 6, x + (cell - 64) / 2 + 64, y + 70, ax, ay, ax + 16, ay + 16, null);
			String s = ids.get(i);
			g.drawString(s, x + (cell - g.getFontMetrics().stringWidth(s)) / 2, y + 88);
		}
		g.dispose();
		ImageIO.write(img, "png", out);
	}
}
