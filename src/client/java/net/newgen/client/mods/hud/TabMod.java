package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.newgen.client.core.Setting;
import net.newgen.client.mods.controls.ToggleState;

/** Player list options. The hooks are in PlayerTabOverlayMixin and GuiMixin. */
public class TabMod extends VanillaPartMod {
	public final Setting.Bool compact = bool("compact", "Compact rows", false);
	public final Setting.Bool pingNumbers = bool("ping_numbers", "Ping as numbers", false);
	public final Setting.Bool toggle = bool("toggle", "Press Tab once to open/close", false);
	public final Setting.Num limit = num("player_limit", "Player limit", 80, 1, 200, 1);
	public final Setting.Bool selfOnTop = bool("self_on_top", "Show yourself on top", false);
	public final Setting.Bool heads = bool("heads", "Player heads", true);
	public final Setting.Bool hideHeader = bool("hide_header", "Hide header", false);
	public final Setting.Bool hideFooter = bool("hide_footer", "Hide footer", false);

	private final ToggleState toggled = new ToggleState();

	public TabMod() {
		super("tab", "Tab", "Hide, scale or move the player list", 0.5f, 0f);
	}

	/** Whether the list is open, given the vanilla key state (read once per frame). */
	public boolean open(boolean keyDown) {
		boolean on = toggled.update(keyDown);
		if (!enabled || !toggle.value) {
			toggled.set(false);
			return keyDown;
		}
		return on;
	}

	public List<PlayerInfo> arrange(List<PlayerInfo> sorted) {
		if (!enabled) return sorted.size() > 80 ? sorted.subList(0, 80) : sorted;
		Minecraft mc = Minecraft.getInstance();
		java.util.UUID me = mc.player != null ? mc.player.getUUID() : null;
		return TabLogic.arrange(sorted, p -> p.getProfile().id().equals(me), selfOnTop.value, (int) limit.get());
	}

	public boolean showHeads(boolean vanilla) {
		return vanilla && (!enabled || heads.value);
	}
}
