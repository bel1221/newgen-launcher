package net.newgen.client.mods.utility;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Named points in the world, shown as labels on screen. Saved per server/world and dimension in config/newgen_waypoints.json. */
public class WaypointsMod extends Mod {
	public static WaypointsMod INSTANCE;

	private static final Logger LOG = LoggerFactory.getLogger("newgen_client");
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("newgen_waypoints.json");
	private static final Type TYPE = new TypeToken<Map<String, List<Waypoint>>>() {
	}.getType();

	public static class Waypoint {
		public String name;
		public double x, y, z;
	}

	private final Setting.Key addKey = key("add_key", "Add waypoint here", GLFW.GLFW_KEY_B);
	private final Setting.Key removeKey = key("remove_key", "Remove nearest waypoint", GLFW.GLFW_KEY_DELETE);
	private final Setting.Text nextName = text("next_name", "Name for new waypoints", "Waypoint");
	private final Setting.Bool deathWaypoint = bool("death_waypoint", "Add a waypoint where you die", true);
	private final Setting.Bool showDistance = bool("show_distance", "Show distance", true);
	private final Setting.Color labelColor = color("label_color", "Label colour", 0xFFFFD75E);
	private final Setting.Bool labelBackground = bool("label_background", "Label background", true);
	private final Setting.Num labelScale = num("label_scale", "Label scale", 1.0, 0.5, 3.0, 0.1);
	private final Setting.Num maxDistance = num("max_distance", "Hide beyond (blocks, 0 = never)", 0, 0, 5000, 100);
	private final Setting.Bool beam = bool("beam", "Beacon beam", true);
	private final Setting.Num beamWidth = num("beam_width", "Beam width", 3, 1, 8, 1);
	private final Setting.Choice marker = choice("marker", "Marker shape", "Diamond", "None", "Diamond", "Square", "Circle", "Arrow");
	private final Setting.Key menuKey = key("menu_key", "Open waypoint list", -1);

	/** Key: server address or world name, then "|", then the dimension id. */
	private Map<String, List<Waypoint>> all = new HashMap<>();
	private final KeyPoll addPoll = new KeyPoll(), removePoll = new KeyPoll(), menuPoll = new KeyPoll();
	private boolean wasDead;

	public WaypointsMod() {
		super("waypoints", "Waypoints", "Mark places and see them through the world");
		INSTANCE = this;
		load();
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("newgen_client", "waypoints"), (g, delta) -> render(g));
	}

	/** Waypoints of the world and dimension the player is in; empty when the mod is off or no world is loaded. */
	public List<Waypoint> current(Minecraft mc) {
		if (!enabled || mc.level == null) return List.of();
		return all.getOrDefault(worldKey(mc), List.of());
	}

	private static String worldKey(Minecraft mc) {
		String world = mc.getCurrentServer() != null ? mc.getCurrentServer().ip
				: mc.getSingleplayerServer() != null ? mc.getSingleplayerServer().getWorldData().getLevelName() : "unknown";
		return world + "|" + mc.level.dimension().identifier();
	}

	private void tick(Minecraft mc) {
		// Poll every tick, even when off, so a key held while switching the mod on does not fire.
		boolean add = addPoll.clicked(mc, addKey), remove = removePoll.clicked(mc, removeKey), menu = menuPoll.clicked(mc, menuKey);
		if (!enabled || mc.player == null || mc.level == null) return;
		if (menu) mc.setScreen(new WaypointListScreen(this));

		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead && deathWaypoint.value) add(mc, "Death");
		wasDead = dead;

		if (add) add(mc, nextName.value.isBlank() ? "Waypoint" : nextName.value);
		if (remove) {
			List<Waypoint> list = all.get(worldKey(mc));
			Waypoint nearest = null;
			if (list != null) {
				for (Waypoint w : list) {
					if (nearest == null || mc.player.distanceToSqr(w.x, w.y, w.z) < mc.player.distanceToSqr(nearest.x, nearest.y, nearest.z)) nearest = w;
				}
			}
			if (nearest != null) {
				list.remove(nearest);
				save();
				mc.player.displayClientMessage(Component.literal("Removed waypoint " + nearest.name), true);
			}
		}
	}

	private void add(Minecraft mc, String name) {
		Waypoint w = new Waypoint();
		w.name = name;
		w.x = mc.player.getX();
		w.y = mc.player.getY();
		w.z = mc.player.getZ();
		all.computeIfAbsent(worldKey(mc), k -> new ArrayList<>()).add(w);
		save();
		mc.player.displayClientMessage(Component.literal("Added waypoint " + name), true);
	}

	private void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null) return;
		Vec3 cam = mc.gameRenderer.getMainCamera().position();
		Vector3fc forward = mc.gameRenderer.getMainCamera().forwardVector();
		for (Waypoint w : current(mc)) {
			Vec3 pos = new Vec3(w.x, w.y + 1, w.z);
			Vec3 d = pos.subtract(cam);
			if (maxDistance.get() > 0 && d.length() > maxDistance.get()) continue;
			if (beam.value) drawBeam(g, mc, w, cam, forward);
			if (d.x * forward.x() + d.y * forward.y() + d.z * forward.z() <= 0) continue; // behind the camera
			// shortcut: the game's own projection ignores view bobbing, so labels sway slightly while walking.
			Vec3 ndc = mc.gameRenderer.projectPointToScreen(pos);
			if (Math.abs(ndc.x) > 1 || Math.abs(ndc.y) > 1) continue;
			int sx = (int) ((ndc.x + 1) / 2 * g.guiWidth()), sy = (int) ((1 - ndc.y) / 2 * g.guiHeight());
			String label = showDistance.value ? w.name + " (" + (int) d.length() + "m)" : w.name;
			int tw = mc.font.width(label);
			float s = (float) labelScale.get();
			g.pose().pushMatrix();
			g.pose().translate(sx, sy);
			g.pose().scale(s, s);
			if (labelBackground.value) g.fill(-tw / 2 - 2, -6, tw / 2 + 2, 5, 0x80000000);
			int color = labelColor.current();
			g.drawString(mc.font, label, -tw / 2, -4, color, true);
			drawMarker(g, marker.value, 0, -12, color);
			g.pose().popMatrix();
		}
	}

	// A light column straight up from the waypoint, like a beacon. Drawn on the HUD from projected points of the
	// world column, so it shows through blocks like the labels do.
	private void drawBeam(GuiGraphics g, Minecraft mc, Waypoint w, Vec3 cam, Vector3fc forward) {
		int color = labelColor.current();
		int glow = (color & 0x00FFFFFF) | 0x50000000, core = (color & 0x00FFFFFF) | 0xA0000000;
		int half = (int) beamWidth.get();
		double prevX = Double.NaN, prevY = 0;
		for (int i = 0; i <= 32; i++) {
			Vec3 p = new Vec3(w.x, w.y + i * 10, w.z);
			Vec3 d = p.subtract(cam);
			double x = Double.NaN, y = 0;
			if (d.x * forward.x() + d.y * forward.y() + d.z * forward.z() > 0.5) {
				Vec3 ndc = mc.gameRenderer.projectPointToScreen(p);
				if (Math.abs(ndc.x) < 3 && Math.abs(ndc.y) < 3) {
					x = (ndc.x + 1) / 2 * g.guiWidth();
					y = (1 - ndc.y) / 2 * g.guiHeight();
				}
			}
			if (!Double.isNaN(x) && !Double.isNaN(prevX)) {
				int cx = (int) Math.round((x + prevX) / 2);
				int top = (int) Math.floor(Math.min(y, prevY)), bottom = (int) Math.ceil(Math.max(y, prevY));
				g.fill(cx - half, top, cx + half + 1, bottom, glow);
				g.fill(cx - Math.max(0, half / 3), top, cx + Math.max(0, half / 3) + 1, bottom, core);
			}
			prevX = x;
			prevY = y;
		}
	}

	/** A small marker centred on x, y; shape is one of the "Marker shape" options. */
	static void drawMarker(GuiGraphics g, String shape, int x, int y, int color) {
		for (int pass = 0; pass < 2; pass++) {
			int c = pass == 0 ? 0xC0000000 : color, o = pass == 0 ? 1 : 0; // shadow first
			switch (shape) {
				case "Diamond" -> {
					for (int i = -3; i <= 3; i++) {
						int h = 3 - Math.abs(i);
						g.fill(x - h + o, y + i + o, x + h + 1 + o, y + i + 1 + o, c);
					}
				}
				case "Square" -> g.fill(x - 3 + o, y - 3 + o, x + 4 + o, y + 4 + o, c);
				case "Circle" -> {
					for (int i = -3; i <= 3; i++) {
						int h = (int) Math.round(Math.sqrt(12.25 - i * i));
						g.fill(x - h + o, y + i + o, x + h + 1 + o, y + i + 1 + o, c);
					}
				}
				case "Arrow" -> {
					for (int i = 0; i <= 3; i++) {
						int h = 3 - i;
						g.fill(x - h + o, y - 1 + i + o, x + h + 1 + o, y + i + o, c);
					}
				}
				default -> {
					return;
				}
			}
		}
	}

	/** Every waypoint of the current world and dimension, for the list screen; changes must be followed by changed(). */
	List<Waypoint> editable(Minecraft mc) {
		return all.computeIfAbsent(worldKey(mc), k -> new ArrayList<>());
	}

	void changed() {
		save();
	}

	private void load() {
		if (!Files.exists(FILE)) return;
		try {
			Map<String, List<Waypoint>> read = new GsonBuilder().create().fromJson(Files.readString(FILE), TYPE);
			if (read != null) {
				read.values().removeIf(list -> list == null);
				for (List<Waypoint> list : read.values()) list.removeIf(w -> w == null || w.name == null);
				all = read;
			}
		} catch (Exception e) {
			LOG.warn("Could not read {}, starting with no waypoints", FILE, e);
		}
	}

	private void save() {
		try {
			// Temp file first so a crash mid-write cannot destroy the saved waypoints.
			Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
			Files.createDirectories(FILE.getParent());
			Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(all, TYPE));
			Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			LOG.error("Could not save {}", FILE, e);
		}
	}
}
