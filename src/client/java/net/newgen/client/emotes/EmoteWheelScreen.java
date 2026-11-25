package net.newgen.client.emotes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.newgen.client.gui.Ui;

/**
 * The emote wheel, open while the wheel key is held: 8 round slots on a ring around the middle of the screen.
 * The slot in the direction of the mouse lights up in blue; letting go of the key (or a click) plays it.
 * The middle shows the lit emote's name and author.
 * <pre>
 *              ( 1 )
 *        ( 8 )       ( 2 )
 *     ( 7 )   [ name  ]   ( 3 )
 *        ( 6 )  by x  ( 4 )
 *              ( 5 )
 * </pre>
 */
public class EmoteWheelScreen extends Screen {
	private static final int RADIUS = 62, SLOT = 34, ICON = 24, CENTER = 46;
	private final EmotesMod mod;
	private int hovered = -1;

	public EmoteWheelScreen(EmotesMod mod) {
		super(Component.literal("Emotes"));
		this.mod = mod;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0x30000000); // the game stays visible
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		int cx = width / 2, cy = height / 2;
		double dx = mouseX - cx, dy = mouseY - cy;
		// Slot 0 is at the top, then clockwise every 45 degrees; the middle (under 22 units) chooses nothing.
		hovered = dx * dx + dy * dy < 22 * 22 ? -1 : Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dx, -dy)) / 45), EmotesMod.SLOTS);

		for (int i = 0; i < EmotesMod.SLOTS; i++) {
			double a = Math.toRadians(i * 45);
			int x = cx + (int) Math.round(Math.sin(a) * RADIUS) - SLOT / 2, y = cy - (int) Math.round(Math.cos(a) * RADIUS) - SLOT / 2;
			EmoteLibrary.Emote emote = EmoteLibrary.get(mod.slot(i));
			boolean on = i == hovered && emote != null;
			Ui.round(g, x, y, SLOT, SLOT, SLOT / 2, on ? Ui.BLUE : Ui.GLASS);
			Ui.ring(g, x, y, SLOT, SLOT, SLOT / 2, on ? 0x80FFFFFF : Ui.LINE);
			if (emote != null) {
				int o = (SLOT - ICON) / 2;
				emote.drawIcon(g, x + o, y + o, ICON);
			}
		}

		Ui.round(g, cx - CENTER / 2, cy - CENTER / 2, CENTER, CENTER, CENTER / 2, Ui.GLASS);
		EmoteLibrary.Emote lit = hovered < 0 ? null : EmoteLibrary.get(mod.slot(hovered));
		int textY = cy + RADIUS + SLOT / 2 + 8;
		if (lit != null) {
			Ui.centered(g, Ui.fit(lit.name(), width - 16, Ui.TITLE, false), cx, textY, Ui.TEXT, Ui.TITLE);
			if (!lit.author().isEmpty()) Ui.centered(g, I18n.get("newgen_client.emotes.by", lit.author()), cx, textY + 13, Ui.DIM, Ui.UI);
		} else {
			Ui.centered(g, Ui.fit(Ui.tr("emotes.hint"), width - 16, Ui.UI, false), cx, textY, Ui.SOFT, Ui.UI);
		}
	}

	@Override
	public void tick() {
		if (!mod.keyHeld(minecraft)) choose();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) choose();
		return true;
	}

	private void choose() {
		EmoteLibrary.Emote emote = hovered < 0 ? null : EmoteLibrary.get(mod.slot(hovered));
		onClose();
		if (emote != null) mod.play(minecraft, emote);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
