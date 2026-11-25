package net.newgen.client.mods.controls;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Press sneak or sprint once to keep it on, press again to release. */
public class ToggleSneakMod extends Mod {
	/** Read by the mixin: forced on in the player's input while true. */
	public static boolean sneak, sprint;

	private final Setting.Bool toggleSneak = bool("sneak", "Toggle sneak", true);
	private final Setting.Bool toggleSprint = bool("sprint", "Toggle sprint", true);
	private final Setting.Bool flySneak = bool("fly_sneak", "Keep sneak while flying", false);

	private boolean sneakOn, sprintOn, sneakWasDown, sprintWasDown;

	public ToggleSneakMod() {
		super("toggle_sneak", "Toggle Sneak/Sprint", "Sneak and sprint stay on after one key press");
		indicator(0f, 0.9f, mc -> {
			List<String> out = new ArrayList<>();
			if (sprint) out.add("[Sprinting (Toggled)]");
			if (sneak) out.add("[Sneaking (Toggled)]");
			return out;
		});
	}

	void tick(Minecraft mc) {
		if (!enabled || mc.player == null) {
			sneakOn = sprintOn = sneak = sprint = false;
			return;
		}
		// Keys read as released while a screen is open, so the toggles survive opening the inventory.
		boolean sneakDown = mc.options.keyShift.isDown();
		boolean sprintDown = mc.options.keySprint.isDown();
		if (sneakDown && !sneakWasDown) sneakOn = !sneakOn;
		if (sprintDown && !sprintWasDown) sprintOn = !sprintOn;
		sneakWasDown = sneakDown;
		sprintWasDown = sprintDown;
		if (!toggleSneak.value) sneakOn = false;
		if (!toggleSprint.value) sprintOn = false;

		// Sneak while flying means "descend"; a stuck toggle would sink the player.
		sneak = sneakOn && (flySneak.value || !mc.player.getAbilities().flying);
		sprint = sprintOn;
	}
}
