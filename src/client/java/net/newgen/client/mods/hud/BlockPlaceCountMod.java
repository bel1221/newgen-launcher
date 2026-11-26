package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Blocks you placed per second, per minute and in total. The BlockItem mixin reports each placement. */
public class BlockPlaceCountMod extends HudMod {
	private static final HudLogic2.PlaceCounter COUNTER = new HudLogic2.PlaceCounter();

	private final Setting.Multi show = multi("show", "Show", new String[]{"Per second", "Per minute", "Total"}, "Per second", "Per minute");
	private final Setting.Num resetMinutes = num("reset_minutes", "Reset every (minutes, 0 = never)", 0, 0, 60, 1);

	public BlockPlaceCountMod() {
		super("block_place_count", "Block Place Count", "How many blocks you place per second and per minute", 0f, 0.38f);
		action("reset", "Counter", "newgen_client.hud.block_place_count.reset", () -> COUNTER.reset(System.currentTimeMillis()));
	}

	/** Called on the client when one of your block placements succeeds. */
	public static void placed() {
		COUNTER.place(System.currentTimeMillis());
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		long now = System.currentTimeMillis();
		COUNTER.autoReset(now, (long) (resetMinutes.get() * 60_000));
		List<String> out = new ArrayList<>();
		if (show.has("Per second")) out.add(I18n.get("newgen_client.hud.block_place_count.second", COUNTER.within(now, 1000)));
		if (show.has("Per minute")) out.add(I18n.get("newgen_client.hud.block_place_count.minute", COUNTER.within(now, 60_000)));
		if (show.has("Total")) out.add(I18n.get("newgen_client.hud.block_place_count.total", COUNTER.total()));
		return out;
	}
}
