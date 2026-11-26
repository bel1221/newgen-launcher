package net.newgen.client.mods.utility;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Dropped items from a list (gold, golden apples, heads by default) drawn larger in the world. Hooks: ItemEntityRenderer/RenderState mixins. */
public class UhcOverlayMod extends Mod {
	public static UhcOverlayMod I;

	private final Setting.Num scale = num("drop_scale", "Size of listed dropped items", 1.5, 1, 3, 0.1);
	private final Setting.Text items = text("items", "Items (ids, comma separated)",
			"gold_ingot, gold_nugget, gold_block, golden_apple, enchanted_golden_apple, player_head");

	private String parsedFrom;
	private Set<String> ids = Set.of();

	/** Added to ItemEntityRenderState: the extra scale of that dropped item (1 = normal). */
	public interface Scaled {
		float newgen$uhcScale();

		void newgen$setUhcScale(float scale);
	}

	public UhcOverlayMod() {
		super("uhc_overlay", "UHC Overlay", "Gold, golden apples and heads on the ground drawn bigger so they are easy to spot");
		I = this;
	}

	/** The scale for a dropped stack: the setting for listed items, else 1. */
	public static float scale(ItemStack stack) {
		UhcOverlayMod m = I;
		if (m == null || !m.enabled || stack.isEmpty()) return 1f;
		if (!m.items.value.equals(m.parsedFrom)) {
			m.ids = parse(m.items.value);
			m.parsedFrom = m.items.value;
		}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		return m.ids.contains(id) ? (float) m.scale.get() : 1f;
	}

	/** "gold_ingot, minecraft:golden_apple" to full ids ("minecraft:gold_ingot", ...). */
	static Set<String> parse(String text) {
		Set<String> out = new HashSet<>();
		if (text == null) return out;
		for (String s : text.split("[,;\\s]+")) {
			s = s.trim().toLowerCase(Locale.ROOT);
			if (s.isEmpty()) continue;
			out.add(s.contains(":") ? s : "minecraft:" + s);
		}
		return out;
	}
}
