package net.newgen.client.mods.utility;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

// Wave 3 utility 1
/** Pairs a hotbar slot with an inventory slot; shift-click on either swaps the two. Hooks: InventoryGuard. */
public class SlotBindingMod extends Mod {
	public static SlotBindingMod INSTANCE;

	final Setting.Key bindKey = key("bind_key", "Bind key (hover a slot)", GLFW.GLFW_KEY_B);
	private final Setting.Color marker = color("marker", "Link marker colour", 0xFF40C0FF);
	private final Setting.Bool number = bool("number", "Show hotbar number", true);
	/** Pairs "hotbar:inventory", inventory indices. */
	private final Setting.Hidden pairs = new Setting.Hidden("pairs", "");

	/** Inventory index picked by the first key press, waiting for its partner; -1 = none. */
	private int pending = -1;

	public SlotBindingMod() {
		super("slot_binding", "Slot Binding", "Bind a hotbar slot to an inventory slot; shift-click swaps them");
		INSTANCE = this;
		settings.add(pairs);
		action("unbind_all", "Bound slots", "Unbind all", () -> {
			pairs.value = "";
			pending = -1;
			Mods.save();
		});
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.enabled;
	}

	Map<Integer, Integer> pairs() {
		return InvLogic.parsePairs(pairs.value);
	}

	/** Bind key pressed over the slot with this inventory index. */
	void press(int index) {
		Map<Integer, Integer> p = new LinkedHashMap<>(pairs());
		String msg;
		if (pending < 0) {
			int partner = InvLogic.partner(p, index);
			if (partner >= 0) {
				p.remove(index);
				p.remove(partner);
				msg = "newgen_client.slot_binding.unbound";
			} else if (index <= 35) {
				pending = index;
				msg = "newgen_client.slot_binding.pick";
			} else return;
		} else {
			int first = pending;
			pending = -1;
			if (first == index) msg = "newgen_client.slot_binding.cancelled";
			else if (InvLogic.bind(p, first, index)) msg = "newgen_client.slot_binding.bound";
			else msg = "newgen_client.slot_binding.invalid";
		}
		pairs.value = InvLogic.formatPairs(p);
		Mods.save();
		Minecraft.getInstance().gui.setOverlayMessage(Component.literal(I18n.get(msg)), false);
	}

	/** Marks a bound (or pending) slot: a coloured corner and the hotbar number of the pair. */
	void draw(GuiGraphics g, Minecraft mc, int x, int y, int index) {
		int c = marker.current();
		if (index == pending) {
			g.fill(x, y, x + 16, y + 1, c);
			g.fill(x, y + 15, x + 16, y + 16, c);
			g.fill(x, y, x + 1, y + 16, c);
			g.fill(x + 15, y, x + 16, y + 16, c);
			return;
		}
		Map<Integer, Integer> p = pairs();
		int partner = InvLogic.partner(p, index);
		if (partner < 0) return;
		g.fill(x, y, x + 4, y + 1, c);
		g.fill(x, y, x + 1, y + 4, c);
		if (number.value) {
			int hotbar = index <= 8 ? index : partner;
			g.pose().pushMatrix();
			g.pose().translate(x + 1, y + 1);
			g.pose().scale(0.5f, 0.5f);
			g.drawString(mc.font, String.valueOf(hotbar + 1), 2, 2, c, true);
			g.pose().popMatrix();
		}
	}
}
