package net.newgen.client.mods.utility;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Wave 3 utility 1
/**
 * The one place the inventory mods (Drop Protection, Slot Locking, Slot Binding) look at clicks, keys and slot drawing.
 * Called from mixin.utility.MultiPlayerGameModeMixin, LocalPlayerDropMixin and AbstractContainerScreenMixin.
 */
public final class InventoryGuard {
	private static boolean redirecting;

	private InventoryGuard() {
	}

	/** Player inventory index of a menu slot (0-8 hotbar, 9-35 main, 36-40 armour/offhand), or -1. */
	public static int index(Slot slot) {
		return slot != null && slot.container instanceof Inventory ? slot.getContainerSlot() : -1;
	}

	private static Slot slot(AbstractContainerMenu menu, int slotId) {
		return slotId >= 0 && slotId < menu.slots.size() ? menu.slots.get(slotId) : null;
	}

	private static boolean locked(int index) {
		return index >= 0 && SlotLockingMod.active() && SlotLockingMod.INSTANCE.isLocked(index);
	}

	/** A container click about to be sent; true = drop it. May send a different click instead (slot binding). */
	public static boolean onClick(MultiPlayerGameMode gameMode, int containerId, int slotId, int button, ClickType type, Player player) {
		if (redirecting) return false;
		AbstractContainerMenu menu = player.containerMenu;
		if (containerId != menu.containerId) return false;
		Slot slot = slot(menu, slotId);
		int index = index(slot);

		// Slot locking: nothing touches a locked slot, nothing swaps into one.
		if (SlotLockingMod.active()) {
			if (locked(index)) return true;
			if (type == ClickType.SWAP && (button >= 0 && button <= 8 || button == Inventory.SLOT_OFFHAND) && locked(button)) return true;
			if (type == ClickType.PICKUP_ALL && !menu.getCarried().isEmpty()) {
				for (Slot s : menu.slots) if (locked(index(s)) && ItemStack.isSameItemSameComponents(s.getItem(), menu.getCarried())) return true;
			}
		}

		// Drop protection: Q over a slot, or letting go of the carried stack outside the window.
		if (type == ClickType.THROW && slot != null && DropProtectionMod.block(slot.getItem())) return true;
		if (slotId == -999 && (type == ClickType.THROW || type == ClickType.PICKUP) && DropProtectionMod.block(menu.getCarried())) return true;

		// Slot binding: shift-click on a bound slot swaps it with its partner.
		if (type == ClickType.QUICK_MOVE && index >= 0 && index <= 35 && SlotBindingMod.active()) {
			Map<Integer, Integer> pairs = SlotBindingMod.INSTANCE.pairs();
			int partner = InvLogic.partner(pairs, index);
			if (partner >= 0) {
				int hotbar = index <= 8 ? index : partner, inv = index <= 8 ? partner : index;
				Slot target = null;
				for (Slot s : menu.slots) if (index(s) == inv) target = s;
				if (target != null) {
					redirecting = true;
					try {
						if (!locked(inv) && !locked(hotbar)) gameMode.handleInventoryMouseClick(containerId, target.index, hotbar, ClickType.SWAP, player);
					} finally {
						redirecting = false;
					}
					return true;
				}
			}
		}
		return false;
	}

	/** Q in the world on the selected hotbar slot; true = don't drop. */
	public static boolean onWorldDrop(Player player) {
		int selected = player.getInventory().getSelectedSlot();
		if (locked(selected) && SlotLockingMod.INSTANCE.worldDrop.value) return true;
		return DropProtectionMod.block(player.getInventory().getSelectedItem());
	}

	/** A key in a container screen while hovering slot; true = used by a mod. */
	public static boolean onKey(Slot hovered, int key) {
		int index = index(hovered);
		if (index < 0 || key <= 0) return false;
		if (SlotLockingMod.active() && key == SlotLockingMod.INSTANCE.lockKey.value) {
			SlotLockingMod.INSTANCE.toggle(index);
			return true;
		}
		if (SlotBindingMod.active() && key == SlotBindingMod.INSTANCE.bindKey.value && index <= 35) {
			SlotBindingMod.INSTANCE.press(index);
			return true;
		}
		return false;
	}

	/** Drawn over each slot of a container screen (coordinates relative to the window's corner). */
	public static void drawSlot(GuiGraphics g, Slot slot) {
		EnchantLabelsMod.draw(g, slot.getItem(), slot.x, slot.y, false);
		int index = index(slot);
		if (index < 0) return;
		if (locked(index)) SlotLockingMod.INSTANCE.draw(g, slot.x, slot.y);
		if (SlotBindingMod.active()) SlotBindingMod.INSTANCE.draw(g, Minecraft.getInstance(), slot.x, slot.y, index);
	}
}
