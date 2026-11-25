package net.newgen.client.mods.visual;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class TntCountdownMod extends Mod {
	public static final TntCountdownMod I = new TntCountdownMod();
	private final Setting.Num scale = num("scale", "Text size", 0.5, 0.2, 1.5, 0.1);
	private final Setting.Bool colored = bool("colored", "Green to red as it runs out", true);

	private TntCountdownMod() {
		super("tnt_countdown", "TNT Countdown", "Shows the seconds left above lit TNT");
	}

	/** Called once per frame while the level is drawn. */
	public void emit(Minecraft mc, float partialTick) {
		if (!enabled) return;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof PrimedTnt tnt)) continue;
			float seconds = Math.max(0, tnt.getFuse() - partialTick) / 20f;
			int color = !colored.value ? 0xFFFFFFFF : seconds > 2 ? 0xFF55FF55 : seconds > 1 ? 0xFFFFFF55 : 0xFFFF5555;
			Gizmos.billboardText(String.format(Locale.ROOT, "%.1f", seconds), tnt.getPosition(partialTick).add(0, 1.4, 0),
					TextGizmo.Style.forColorAndCentered(color).withScale((float) scale.get()));
		}
	}
}
