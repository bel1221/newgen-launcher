package net.newgen.client.mods.utility;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Simple list of the waypoints of the current world and dimension, with a delete button each. */
class WaypointListScreen extends Screen {
	private static final int ROW = 22, PER_PAGE = 8;

	private final WaypointsMod mod;
	private int page;

	WaypointListScreen(WaypointsMod mod) {
		super(Component.translatable("newgen_client.waypoints.list_title"));
		this.mod = mod;
	}

	@Override
	protected void init() {
		Minecraft mc = Minecraft.getInstance();
		List<WaypointsMod.Waypoint> list = mod.editable(mc);
		int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.min(page, pages - 1);
		int top = 40, right = width / 2 + 150;
		for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
			WaypointsMod.Waypoint w = list.get(i);
			int y = top + (i - page * PER_PAGE) * ROW;
			addRenderableWidget(Button.builder(Component.translatable("newgen_client.waypoints.delete"), b -> {
				list.remove(w);
				mod.changed();
				rebuildWidgets();
			}).bounds(right - 60, y, 60, 20).build());
		}
		int by = height - 30;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page--;
			rebuildWidgets();
		}).bounds(width / 2 - 154, by, 30, 20).build()).active = page > 0;
		addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildWidgets();
		}).bounds(width / 2 + 124, by, 30, 20).build()).active = page < pages - 1;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(width / 2 - 60, by, 120, 20).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Minecraft mc = Minecraft.getInstance();
		g.drawCenteredString(font, title, width / 2, 15, 0xFFFFFFFF);
		List<WaypointsMod.Waypoint> list = mod.editable(mc);
		if (list.isEmpty()) {
			g.drawCenteredString(font, Component.translatable("newgen_client.waypoints.empty"), width / 2, 60, 0xFFAAAAAA);
			return;
		}
		int top = 40, left = width / 2 - 150;
		for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
			WaypointsMod.Waypoint w = list.get(i);
			int y = top + (i - page * PER_PAGE) * ROW;
			g.fill(left, y, width / 2 + 150 - 64, y + 20, 0x60000000);
			WaypointsMod.drawMarker(g, "Diamond", left + 8, y + 10, 0xFFFFD75E);
			g.drawString(font, w.name, left + 18, y + 2, 0xFFFFFFFF, true);
			String info = (int) Math.floor(w.x) + ", " + (int) Math.floor(w.y) + ", " + (int) Math.floor(w.z);
			if (mc.player != null) info += "  (" + (int) Math.sqrt(mc.player.distanceToSqr(w.x, w.y, w.z)) + "m)";
			g.drawString(font, info, left + 18, y + 11, 0xFFAAAAAA, true);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
