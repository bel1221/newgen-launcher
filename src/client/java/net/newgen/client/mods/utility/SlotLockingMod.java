package net.newgen.client.mods.utility;

import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

// Wave 3 utility 1
/** Locks inventory slots: a locked slot can't be clicked, moved, swapped into or dropped. Hooks: InventoryGuard. */
public class SlotLockingMod extends Mod {
	public static SlotLockingMod INSTANCE;

	final Setting.Key lockKey = key("lock_key", "Lock key (hover a slot)", GLFW.GLFW_KEY_L);
	private final Setting.Bool padlock = bool("padlock", "Show padlock", true);
	private final Setting.Color tint = color("tint", "Locked slot tint", 0x40FF5050);
	final Setting.Bool worldDrop = bool("world_drop", "Block Q on a locked hotbar slot", true);
	/** Locked slots as player inventory indices (0-8 hotbar, 9-35 main, 36-39 armour, 40 offhand). */
	private final Setting.Hidden slots = new Setting.Hidden("slots", "");

	private String parsedFrom;
	private Set<Integer> locked = Set.of();

	public SlotLockingMod() {
		super("slot_locking", "Slot Locking", "Lock inventory slots so their items can't be moved or dropped");
		INSTANCE = this;
		settings.add(slots);
		action("unlock_all", "Locked slots", "Unlock all", () -> {
			slots.value = "";
			Mods.save();
		});
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.enabled;
	}

	public boolean isLocked(int index) {
		if (!slots.value.equals(parsedFrom)) {
			parsedFrom = slots.value;
			locked = InvLogic.parseInts(slots.value, 40);
		}
		return locked.contains(index);
	}

	void toggle(int index) {
		Set<Integer> s = new java.util.TreeSet<>(InvLogic.parseInts(slots.value, 40));
		if (!s.remove(index)) s.add(index);
		slots.value = InvLogic.formatInts(s);
		Mods.save();
	}

	/** Draws the tint and a small padlock over a slot at (x, y). */
	void draw(GuiGraphics g, int x, int y) {
		g.fill(x, y, x + 16, y + 16, tint.current());
		if (!padlock.value) return;
		int c = 0xFFFFD040, dark = 0xFF000000;
		int px = x + 10, py = y;
		// shackle
		g.fill(px + 1, py, px + 5, py + 1, dark);
		g.fill(px, py + 1, px + 1, py + 4, dark);
		g.fill(px + 5, py + 1, px + 6, py + 4, dark);
		g.fill(px + 1, py + 1, px + 5, py + 2, c);
		g.fill(px + 1, py + 2, px + 2, py + 4, c);
		g.fill(px + 4, py + 2, px + 5, py + 4, c);
		// body
		g.fill(px - 1, py + 4, px + 7, py + 9, dark);
		g.fill(px, py + 5, px + 6, py + 8, c);
		g.fill(px + 2, py + 6, px + 4, py + 7, dark);
	}
}
