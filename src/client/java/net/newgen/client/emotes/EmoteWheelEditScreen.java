package net.newgen.client.emotes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Mods;
import net.newgen.client.gui.Ui;

/**
 * "Edit wheel": pick a slot on the left, then an emote on the right to put it there. Right click a slot to empty it.
 * Every emote shows its icon, name and author (the credit the emote files carry).
 * <pre>
 * Emote wheel                                              [x]
 * +-- slots 118 --+  +------+ +------+ +------+ +------+
 * | 1 [i] Hello   |  | icon | | icon | | icon | | icon |   cards 60x62
 * | 2 [i] Hug     |  | name | | name | | name | | name |
 * | ...           |  | by x | | by x | | by x | | by x |
 * +---------------+  +------+ +------+ +------+ +------+
 * Pick a slot, then an emote. Right click a slot to empty it.
 * </pre>
 */
public class EmoteWheelEditScreen extends Screen {
	private static final int PAD = 8, HEADER = 28, SIDE_W = 118, ROW = 18, CARD_W = 60, CARD_H = 62, GAP = 6, TOAST_H = 32;
	private final Screen parent;
	private final EmotesMod mod;
	private int px, py, pw, ph, slot, scroll;

	public EmoteWheelEditScreen(Screen parent, EmotesMod mod) {
		super(Component.literal("Emote wheel"));
		this.parent = parent;
		this.mod = mod;
	}

	@Override
	protected void init() {
		pw = Math.min(440, width - 40);
		ph = Math.min(280, height - TOAST_H - 12);
		px = (width - pw) / 2;
		py = Math.max(TOAST_H + 4, (height - ph) / 2);
	}

	private int gridX() {
		return px + PAD + SIDE_W + PAD;
	}

	private int top() {
		return py + HEADER + PAD;
	}

	private int bottom() {
		return py + ph - PAD - 14;
	}

	private int columns() {
		return Math.max(1, (px + pw - PAD - gridX() + GAP) / (CARD_W + GAP));
	}

	private List<EmoteLibrary.Emote> emotes() {
		return new ArrayList<>(EmoteLibrary.all().values());
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (minecraft.level == null && !net.newgen.client.gui.VideoBackground.render(g, width, height)) renderPanorama(g, delta);
		renderBlurredBackground(g);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Ui.panel(g, px, py, pw, ph);
		g.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, 0x18FFFFFF);
		Ui.text(g, Ui.tr("emotes.wheel"), px + PAD, py + (HEADER - 7) / 2, Ui.TEXT, Ui.TITLE);
		int cx = px + pw - PAD - 14, cy = py + (HEADER - 14) / 2;
		Ui.button(g, cx, cy, 14, 14, "", Ui.I_CLOSE, Ui.inside(mouseX, mouseY, cx, cy, 14, 14) ? 1f : 0f, Ui.DARK, Ui.CAPS);

		// Slots.
		int sx = px + PAD;
		Ui.round(g, sx, top() - 2, SIDE_W, EmotesMod.SLOTS * ROW + 4, Ui.R_MD, Ui.CARD);
		for (int i = 0; i < EmotesMod.SLOTS; i++) {
			int y = top() + i * ROW;
			boolean over = Ui.inside(mouseX, mouseY, sx + 2, y, SIDE_W - 4, ROW);
			if (i == slot || over) Ui.round(g, sx + 2, y, SIDE_W - 4, ROW, Ui.R_SM, i == slot ? 0x26FFFFFF : Ui.HOVER);
			if (i == slot) Ui.round(g, sx + 2, y + 3, 2, ROW - 6, 1, Ui.BLUE);
			Ui.text(g, String.valueOf(i + 1), sx + 9, y + 6, Ui.DIM, Ui.UI);
			EmoteLibrary.Emote e = EmoteLibrary.get(mod.slot(i));
			if (e != null) {
				e.drawIcon(g, sx + 20, y + 2, 14);
				Ui.text(g, Ui.fit(e.name(), SIDE_W - 46, Ui.CAPS, false), sx + 38, y + 6, Ui.TEXT, Ui.CAPS);
			} else {
				Ui.text(g, Ui.tr("emotes.empty"), sx + 38, y + 6, Ui.DIM, Ui.UI);
			}
		}

		// Emotes.
		List<EmoteLibrary.Emote> list = emotes();
		int cols = columns(), rows = (list.size() + cols - 1) / cols, content = rows * (CARD_H + GAP) - GAP;
		scroll = Math.max(0, Math.min(scroll, content - (bottom() - top())));
		g.enableScissor(gridX(), top(), px + pw - PAD, bottom());
		for (int i = 0; i < list.size(); i++) {
			EmoteLibrary.Emote e = list.get(i);
			int x = gridX() + (i % cols) * (CARD_W + GAP), y = top() - scroll + (i / cols) * (CARD_H + GAP);
			boolean inSlot = e.id().equals(mod.slot(slot)), over = Ui.inside(mouseX, mouseY, x, y, CARD_W, CARD_H) && mouseY >= top() && mouseY < bottom();
			Ui.round(g, x, y, CARD_W, CARD_H, Ui.R_MD, Ui.CARD);
			if (over) Ui.round(g, x, y, CARD_W, CARD_H, Ui.R_MD, Ui.HOVER);
			Ui.ring(g, x, y, CARD_W, CARD_H, Ui.R_MD, inSlot ? Ui.BLUE : Ui.LINE);
			e.drawIcon(g, x + (CARD_W - 28) / 2, y + 6, 28);
			Ui.centered(g, Ui.fit(e.name(), CARD_W - 6, Ui.CAPS, false), x + CARD_W / 2, y + 39, Ui.TEXT, Ui.CAPS);
			Ui.centered(g, Ui.fit(I18n.get("newgen_client.emotes.by", e.author()), CARD_W - 6, Ui.UI, false), x + CARD_W / 2, y + 51, Ui.DIM, Ui.UI);
		}
		g.disableScissor();
		Ui.text(g, Ui.fit(Ui.tr("emotes.edit_hint"), pw - PAD * 2, Ui.UI, false), px + PAD, py + ph - PAD - 8, Ui.DIM, Ui.UI);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (Ui.inside(mx, my, px + pw - PAD - 14, py + (HEADER - 14) / 2, 14, 14)) {
			onClose();
			return true;
		}
		for (int i = 0; i < EmotesMod.SLOTS; i++) {
			if (Ui.inside(mx, my, px + PAD + 2, top() + i * ROW, SIDE_W - 4, ROW)) {
				if (event.button() == 1) {
					mod.setSlot(i, "");
					Mods.save();
				}
				slot = i;
				return true;
			}
		}
		if (event.button() != 0 || my < top() || my >= bottom()) return true;
		List<EmoteLibrary.Emote> list = emotes();
		int cols = columns();
		for (int i = 0; i < list.size(); i++) {
			int x = gridX() + (i % cols) * (CARD_W + GAP), y = top() - scroll + (i / cols) * (CARD_H + GAP);
			if (Ui.inside(mx, my, x, y, CARD_W, CARD_H)) {
				mod.setSlot(slot, list.get(i).id());
				Mods.save();
				slot = (slot + 1) % EmotesMod.SLOTS; // the next slot is ready to fill
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= (int) (scrollY * 20);
		return true;
	}

	@Override
	public void onClose() {
		Mods.save();
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
