package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Rendered entities and all entities the client knows of, as the debug screen counts them. */
public class EntityCounterMod extends HudMod {
	private final Setting.Bool total = bool("show_total", "Show total", true);
	private final Setting.Num rate = num("update_rate", "Update every (ms)", 250, 0, 2000, 50);

	private long last;
	private String shown = "";

	public EntityCounterMod() {
		super("entity_counter", "Entity Counter", "How many entities are being rendered, out of all loaded ones", 0f, 0.34f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.level == null) return List.of();
		long now = System.currentTimeMillis();
		if (now - last >= rate.get() || shown.isEmpty()) {
			last = now;
			int[] stats = HudLogic2.parseEntityStats(mc.levelRenderer.getEntityStatistics());
			int rendered = stats == null ? 0 : stats[0], all = stats == null ? mc.level.getEntityCount() : stats[1];
			shown = I18n.get("newgen_client.hud.entity_counter.text", total.value ? rendered + "/" + all : String.valueOf(rendered));
		}
		return List.of(shown);
	}
}
