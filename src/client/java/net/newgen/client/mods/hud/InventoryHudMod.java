package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Your main inventory (the 27 slots above the hotbar) on the HUD. */
public class InventoryHudMod extends HudMod {
	private static final int CELL = 18;

	private final Setting.Choice slotStyle = radio("slot_style", "Slot style", "Boxes", "Boxes", "Outline", "None");
	private final Setting.Color slotColor = color("slot_color", "Slot colour", 0x40FFFFFF);
	private final Setting.Bool counts = bool("show_counts", "Show counts", true);
	private final Setting.Bool hideEmpty = bool("hide_when_empty", "Hide when empty", false);

	public InventoryHudMod() {
		super("inventory_hud", "Inventory HUD", "Your inventory on the screen, with item counts", 0.5f, 0.75f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		if (mc.player == null) return editing ? new int[]{9 * CELL, 3 * CELL} : null;
		if (hideEmpty.value && !editing) {
			boolean any = false;
			for (int i = 9; i < 36 && !any; i++) any = !mc.player.getInventory().getItem(i).isEmpty();
			if (!any) return null;
		}
		return new int[]{9 * CELL, 3 * CELL};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		int c = slotColor.current();
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				int x = col * CELL, y = row * CELL;
				if (slotStyle.value.equals("Boxes")) g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, c);
				else if (slotStyle.value.equals("Outline")) g.renderOutline(x, y, CELL, CELL, c);
				if (mc.player == null) continue;
				ItemStack stack = mc.player.getInventory().getItem(9 + row * 9 + col);
				if (stack.isEmpty()) continue;
				g.renderItem(stack, x + 1, y + 1);
				if (counts.value) g.renderItemDecorations(mc.font, stack, x + 1, y + 1);
			}
		}
	}
}
