package net.newgen.client.mods.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

// Wave 3 utility 1
/** Stops chosen, enchanted or renamed items from being dropped (Q, or thrown out of an inventory). Hooks: InventoryGuard. */
public class DropProtectionMod extends Mod {
	public static DropProtectionMod INSTANCE;

	private final Setting.Pick item1 = item("item_1", "Protected item 1", "minecraft:totem_of_undying");
	private final Setting.Pick item2 = item("item_2", "Protected item 2", "minecraft:enchanted_golden_apple");
	private final Setting.Pick item3 = item("item_3", "Protected item 3", "minecraft:elytra");
	private final Setting.Text more = text("more_items", "More protected items", "");
	private final Setting.Bool enchanted = bool("enchanted", "Protect enchanted items", true);
	private final Setting.Bool renamed = bool("renamed", "Protect renamed items", true);
	private final Setting.Bool doublePress = bool("double_press", "Press twice to drop anyway", true);
	private final Setting.Num window = num("double_press_ms", "Double press time (ms)", 400, 100, 1500, 50);
	private final Setting.Bool message = bool("message", "Show a message when blocked", true);

	private long lastBlocked;
	private boolean releasedSince;
	private String idsSource;
	private java.util.Set<String> ids = java.util.Set.of();

	public DropProtectionMod() {
		super("drop_protection", "Drop Protection", "Stops valuable items from being dropped by accident");
		INSTANCE = this;
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	/** Watches for the drop key and the mouse being let go, so a held key never counts as a second press. */
	private void tick(Minecraft mc) {
		if (!enabled || lastBlocked == 0) return;
		boolean held = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mc.options.keyDrop);
		if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() > 0) held |= InputConstants.isKeyDown(mc.getWindow(), key.getValue());
		if (!held) releasedSince = true;
	}

	public boolean isProtected(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (enchanted.value && !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()) return true;
		if (renamed.value && stack.has(DataComponents.CUSTOM_NAME)) return true;
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		if (id.equals(item1.value) || id.equals(item2.value) || id.equals(item3.value)) return true;
		if (!more.value.equals(idsSource)) {
			idsSource = more.value;
			ids = InvLogic.parseIds(more.value);
		}
		return ids.contains(id);
	}

	/** True when dropping this stack must be stopped now. */
	public static boolean block(ItemStack stack) {
		DropProtectionMod m = INSTANCE;
		if (m == null || !m.enabled || !m.isProtected(stack)) return false;
		long now = System.currentTimeMillis();
		if (m.doublePress.value && InvLogic.secondPress(m.lastBlocked, now, (long) m.window.get(), m.releasedSince)) {
			m.lastBlocked = 0;
			return false;
		}
		m.lastBlocked = now;
		m.releasedSince = false;
		if (m.message.value) {
			String text = m.doublePress.value ? I18n.get("newgen_client.drop_protection.blocked_twice") : I18n.get("newgen_client.drop_protection.blocked");
			Minecraft.getInstance().gui.setOverlayMessage(Component.literal(text), false);
		}
		return true;
	}
}
