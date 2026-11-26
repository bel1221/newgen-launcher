package net.newgen.client.mods.utility;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

// Wave 3 utility 1
/** Short enchantment labels ("S5", "P4", "U3") on item slots, in inventories and the hotbar. Hooks: slot render mixins. */
public class EnchantLabelsMod extends Mod {
	public static EnchantLabelsMod INSTANCE;

	private final Setting.Bool inventories = bool("inventories", "Show in inventories", true);
	private final Setting.Bool hotbar = bool("hotbar", "Show on the hotbar", true);
	private final Setting.Num size = num("text_size", "Text size", 0.5, 0.3, 1.0, 0.1);
	private final Setting.Num max = num("max_labels", "Labels per item", 2, 1, 4, 1);
	private final Setting.Bool level = bool("show_level", "Show level", true);
	private final Setting.Bool books = bool("books", "Enchanted books too", true);
	private final Setting.Text only = text("only", "Only these enchantments", "");
	private final Setting.Color color = color("color", "Label colour", 0xFFFFFF55);

	private String onlySource;
	private java.util.Set<String> onlyIds = java.util.Set.of();

	public EnchantLabelsMod() {
		super("enchant_labels", "Enchant Labels", "Short enchantment labels on item slots");
		INSTANCE = this;
	}

	/** Draws the labels of stack on the 16x16 slot at (x, y). inHotbar picks which switch applies. */
	public static void draw(GuiGraphics g, ItemStack stack, int x, int y, boolean inHotbar) {
		EnchantLabelsMod m = INSTANCE;
		if (m == null || !m.enabled || stack.isEmpty() || !(inHotbar ? m.hotbar.value : m.inventories.value)) return;
		if (!m.books.value && stack.has(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)) return;
		ItemEnchantments ench = EnchantmentHelper.getEnchantmentsForCrafting(stack);
		if (ench.isEmpty()) return;
		if (!m.only.value.equals(m.onlySource)) {
			m.onlySource = m.only.value;
			m.onlyIds = InvLogic.parseIds(m.only.value);
		}
		List<String> labels = new ArrayList<>();
		for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
			String id = e.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse("");
			if (!m.onlyIds.isEmpty() && !m.onlyIds.contains(id)) continue;
			String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
			labels.add(InvLogic.enchantLabel(path, e.getIntValue(), m.level.value));
			if (labels.size() >= (int) m.max.get()) break;
		}
		if (labels.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		float s = (float) m.size.get();
		int line = (int) Math.ceil(9 * s);
		g.pose().pushMatrix();
		for (int i = 0; i < labels.size(); i++) {
			g.pose().pushMatrix();
			g.pose().translate(x, y + i * line);
			g.pose().scale(s, s);
			g.drawString(mc.font, labels.get(i), 0, 0, m.color.current(), true);
			g.pose().popMatrix();
		}
		g.pose().popMatrix();
	}
}
