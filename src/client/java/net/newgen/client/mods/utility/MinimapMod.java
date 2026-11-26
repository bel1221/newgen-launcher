package net.newgen.client.mods.utility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Small top-down map of the surface around the player, north up. */
public class MinimapMod extends HudMod {
	private static final int GRID = 64, HALF = GRID / 2;

	private final Setting.Num size = num("size", "Map size (pixels)", 96, 48, 192, 8);
	private final Setting.Choice range = choice("range", "Range (blocks)", "32", "32", "64", "128");
	private final Setting.Bool border = bool("border", "Border", true);
	private final Setting.Color borderColor = color("border_color", "Border colour", 0xFF202020);
	private final Setting.Color playerColor = color("player_color", "Player marker colour", 0xFFFF3030);
	private final Setting.Bool waypoints = bool("waypoints", "Show waypoints", true);
	private final Setting.Color waypointColor = color("waypoint_color", "Waypoint marker colour", 0xFFFFD75E);
	private final Setting.Bool coordinates = bool("coordinates", "Show coordinates", true);
	private final Setting.Action export = action("export", "Export map as PNG", "newgen_client.minimap.export", this::exportPng);

	private final int[] colors = new int[GRID * GRID];
	private ClientLevel mappedLevel;
	private int centerX, centerZ, mappedStep;
	private long mappedAt;

	public MinimapMod() {
		super("minimap", "Minimap", "Top-down map of the surface around you", 1f, 0f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of(); // draws its own picture in render()
	}

	@Override
	public void render(GuiGraphics g, Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		int px = (int) size.get();
		int textHeight = coordinates.value ? 11 : 0;
		float s = (float) scale.get();
		width = Math.round(px * s);
		height = Math.round((px + textHeight) * s);
		left = Math.round(x * Math.max(0, g.guiWidth() - width));
		top = Math.round(y * Math.max(0, g.guiHeight() - height));

		int step = Integer.parseInt(range.value) * 2 / GRID; // blocks per map cell
		BlockPos p = mc.player.blockPosition();
		long now = System.currentTimeMillis();
		if (mc.level != mappedLevel || step != mappedStep || now - mappedAt > 500
				|| Math.abs(p.getX() - centerX) >= step || Math.abs(p.getZ() - centerZ) >= step) {
			mappedLevel = mc.level;
			mappedStep = step;
			mappedAt = now;
			centerX = p.getX();
			centerZ = p.getZ();
			rebuild(mc.level);
		}

		g.pose().pushMatrix();
		g.pose().translate(left, top);
		g.pose().scale(s, s);
		if (border.value) g.fill(0, 0, px, px, borderColor.value);
		else if (background.value) g.fill(0, 0, px, px, backgroundColor());
		if (coordinates.value) {
			String text = p.getX() + ", " + p.getY() + ", " + p.getZ();
			g.drawString(mc.font, text, (px - mc.font.width(text)) / 2, px + 2, textColor.value, textShadow.value);
		}
		// From here on one unit is one map cell; the map sits one pixel inside the border.
		g.pose().translate(1, 1);
		g.pose().scale((px - 2) / (float) GRID, (px - 2) / (float) GRID);
		for (int z = 0; z < GRID; z++) {
			// Neighbouring cells of one colour are drawn as one rectangle to keep the count low.
			for (int x0 = 0; x0 < GRID; ) {
				int c = colors[z * GRID + x0], x1 = x0 + 1;
				while (x1 < GRID && colors[z * GRID + x1] == c) x1++;
				if (c != 0) g.fill(x0, z, x1, z + 1, c);
				x0 = x1;
			}
		}
		if (waypoints.value && WaypointsMod.INSTANCE != null) {
			for (WaypointsMod.Waypoint w : WaypointsMod.INSTANCE.current(mc)) {
				int wx = HALF + (int) Math.floor((w.x - centerX) / step), wz = HALF + (int) Math.floor((w.z - centerZ) / step);
				if (wx < 1 || wz < 1 || wx >= GRID - 1 || wz >= GRID - 1) continue;
				g.fill(wx - 1, wz - 1, wx + 2, wz + 2, 0xFF000000);
				g.fill(wx, wz, wx + 1, wz + 1, waypointColor.value);
			}
		}
		// Player: a dot in the middle and a short line the way they face (yaw 0 = south = down on the map).
		g.pose().translate(HALF, HALF);
		g.pose().rotate((float) Math.toRadians(mc.player.getYRot()));
		g.fill(-1, -1, 1, 1, playerColor.value);
		g.pose().scale(0.5f, 1f);
		g.fill(-1, 1, 1, 5, playerColor.value);
		g.pose().popMatrix();
	}

	// shortcut: uses the top-most block, so the Nether shows only its roof and caves are not mapped;
	// needs a per-dimension height scan to fix.
	private void rebuild(ClientLevel level) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int gz = 0; gz < GRID; gz++) {
			for (int gx = 0; gx < GRID; gx++) {
				int wx = centerX + (gx - HALF) * mappedStep, wz = centerZ + (gz - HALF) * mappedStep;
				int color = 0;
				if (level.hasChunk(wx >> 4, wz >> 4)) {
					int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
					pos.set(wx, h, wz);
					int rgb = level.getBlockState(pos).getMapColor(level, pos).col;
					// Slopes facing north are lit, slopes facing south are dark, like on vanilla maps.
					int north = level.hasChunk(wx >> 4, (wz - 1) >> 4) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz - 1) - 1 : h;
					int light = h > north ? 255 : h < north ? 180 : 220;
					color = 0xFF000000 | ((rgb >> 16 & 255) * light / 255) << 16 | ((rgb >> 8 & 255) * light / 255) << 8 | (rgb & 255) * light / 255;
				}
				colors[gz * GRID + gx] = color;
			}
		}
	}

	/** Saves the map as it is now (4 pixels per cell) to screenshots/minimap_<time>.png and says so in chat. */
	private void exportPng() {
		Minecraft mc = Minecraft.getInstance();
		if (mappedLevel == null || mc.player == null) return;
		int px = 4;
		String name = "minimap_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ".png";
		Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
		try (NativeImage image = new NativeImage(GRID * px, GRID * px, true)) {
			for (int z = 0; z < GRID * px; z++) {
				for (int x = 0; x < GRID * px; x++) image.setPixel(x, z, colors[(z / px) * GRID + x / px]);
			}
			// Player in the middle, in the marker colour.
			int c = HALF * px;
			for (int z = c - 3; z < c + 3; z++) for (int x = c - 3; x < c + 3; x++) image.setPixel(x, z, playerColor.value | 0xFF000000);
			Files.createDirectories(file.getParent());
			image.writeToFile(file);
			mc.player.displayClientMessage(Component.translatable("newgen_client.minimap.exported", Component.literal(name)
					.withStyle(s -> s.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(file.toFile())))), false);
		} catch (Exception e) {
			LoggerFactory.getLogger("newgen_client").error("Could not export the minimap to {}", file, e);
			mc.player.displayClientMessage(Component.translatable("newgen_client.minimap.export_failed"), false);
		}
	}
}
