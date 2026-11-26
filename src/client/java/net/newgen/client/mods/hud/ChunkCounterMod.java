package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Rendered chunk sections, as the debug screen counts them. */
public class ChunkCounterMod extends HudMod {
	private final Setting.Bool total = bool("show_total", "Show total", true);
	private final Setting.Num rate = num("update_rate", "Update every (ms)", 250, 0, 2000, 50);

	private long last;
	private String shown = "";

	public ChunkCounterMod() {
		super("chunk_counter", "Chunk Counter", "How many chunk sections are being rendered", 0f, 0.3f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.level == null) return List.of();
		long now = System.currentTimeMillis();
		if (now - last >= rate.get() || shown.isEmpty()) {
			last = now;
			int rendered = mc.levelRenderer.countRenderedSections();
			String n = total.value ? rendered + "/" + (int) mc.levelRenderer.getTotalSections() : String.valueOf(rendered);
			shown = I18n.get("newgen_client.hud.chunk_counter.text", n);
		}
		return List.of(shown);
	}
}
