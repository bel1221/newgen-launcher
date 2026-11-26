package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class PotionEffectsMod extends HudMod {
	/** Read by GuiMixin to switch off the vanilla effect icons. */
	public final Setting.Bool hideVanilla = bool("hide_vanilla", "Hide vanilla effect icons", true);

	private final Setting.Color timeColor = color("time_color", "Time colour", 0xFFAAAAAA);
	private final Setting.Bool blink = bool("blink", "Blink when running out", true);
	private final Setting.Num blinkSeconds = num("blink_seconds", "Blink below (seconds)", 10, 1, 60, 1);
	private final Setting.Bool compact = bool("compact", "Compact (one line each)", false);
	private final Setting.Bool roman = bool("roman", "Roman numerals", false);
	private final Setting.Choice sort = choice("sort", "Sort by", "Vanilla", "Vanilla", "Time left", "Name");
	private final Setting.Pick hidden = effect("hidden_effect", "Hide this effect", "");

	private final List<MobEffectInstance> effects = new ArrayList<>();

	public PotionEffectsMod() {
		super("potion_effects", "Potion Effects", "Your active effects with name and time left", 1f, 0.2f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	private int row() {
		return compact.value ? 11 : 20;
	}

	@Override
	protected int[] size(Minecraft mc) {
		effects.clear();
		if (mc.player == null) return null;
		Identifier hide = hidden.value.isBlank() ? null : Identifier.tryParse(hidden.value.trim());
		for (MobEffectInstance e : mc.player.getActiveEffects()) {
			if (hide != null && e.getEffect().unwrapKey().map(k -> k.identifier().equals(hide)).orElse(false)) continue;
			effects.add(e);
		}
		if (effects.isEmpty()) return null;
		switch (sort.value) {
			case "Time left" -> effects.sort(Comparator.comparingInt(
					(MobEffectInstance e) -> e.isInfiniteDuration() ? Integer.MAX_VALUE : e.getDuration()));
			case "Name" -> effects.sort(Comparator.comparing(this::name));
			default -> effects.sort(Comparator.reverseOrder()); // the vanilla HUD's order
		}
		int textWidth = 0;
		for (MobEffectInstance e : effects) {
			textWidth = Math.max(textWidth, compact.value ? mc.font.width(name(e) + " " + time(e))
					: Math.max(mc.font.width(name(e)), mc.font.width(time(e))));
		}
		return new int[] {(compact.value ? 11 : 20) + textWidth, effects.size() * row() - 2};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		long now = System.currentTimeMillis();
		for (int i = 0; i < effects.size(); i++) {
			MobEffectInstance e = effects.get(i);
			int y = i * row();
			boolean ending = blink.value && !e.isInfiniteDuration() && e.getDuration() < blinkSeconds.get() * 20;
			if (ending && now / 300 % 2 == 1) continue; // off half of the blink
			if (compact.value) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(e.getEffect()), 0, y, 9, 9);
				String name = name(e) + " ";
				g.drawString(mc.font, name, 11, y + 1, textColor.current(), textShadow.value);
				g.drawString(mc.font, time(e), 11 + mc.font.width(name), y + 1, timeColor.current(), textShadow.value);
			} else {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(e.getEffect()), 0, y, 18, 18);
				g.drawString(mc.font, name(e), 20, y, textColor.current(), textShadow.value);
				g.drawString(mc.font, time(e), 20, y + 9, timeColor.current(), textShadow.value);
			}
		}
	}

	private String name(MobEffectInstance e) {
		String name = e.getEffect().value().getDisplayName().getString();
		int level = e.getAmplifier() + 1;
		if (level <= 1) return name;
		return name + " " + (roman.value ? HudValues.roman(level) : String.valueOf(level));
	}

	private static String time(MobEffectInstance e) {
		if (e.isInfiniteDuration()) return "Infinite";
		int seconds = e.getDuration() / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
