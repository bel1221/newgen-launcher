package net.newgen.client.mods.controls;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Press sneak or sprint once to keep it on, press again to release. */
public class ToggleSneakMod extends Mod {
	/** Read by the mixin: forced on in the player's input while true. */
	public static boolean sneak, sprint;
	/** Read by LocalPlayerMixin: double-tapping forward does not start sprinting. */
	public static boolean noDoubleTap;
	/** Read by PlayerMixin: multiplier on the flying speed (1 = off). */
	public static float flyBoost = 1f;

	private final Setting.Bool toggleSneak = bool("sneak", "Toggle sneak", true);
	private final Setting.Bool toggleSprint = bool("sprint", "Toggle sprint", true);
	private final Setting.Bool flySneak = bool("fly_sneak", "Keep sneak while flying", false);
	// Unbound = the normal sneak/sprint keys toggle; bound = these keys toggle and the normal keys work as hold.
	private final Setting.Key sneakKey = key("sneak_key", "Separate toggle sneak key", -1);
	private final Setting.Key sprintKey = key("sprint_key", "Separate toggle sprint key", -1);
	private final Setting.Bool disableDoubleTap = bool("no_double_tap", "Disable double-tap W to sprint", false);
	private final Setting.Bool unsneakOnFly = bool("unsneak_fly", "Unsneak when you start flying", true);
	private final Setting.Bool boost = bool("fly_boost", "Fly boost (creative / singleplayer)", false);
	private final Setting.Num boostAmount = num("fly_boost_amount", "Fly boost amount", 2, 1, 10, 0.5);

	private final ToggleState sneakState = new ToggleState(), sprintState = new ToggleState();
	private boolean wasFlying, boosting;

	public ToggleSneakMod() {
		super("toggle_sneak", "Toggle Sneak/Sprint", "Sneak and sprint stay on after one key press");
		indicator(0f, 0.9f, this::indicatorLines);
	}

	private List<String> indicatorLines(Minecraft mc) {
		List<String> out = new ArrayList<>();
		if (mc.player == null) return out;
		if (mc.player.getAbilities().flying) {
			out.add(tr(boosting ? "flying_boost" : "flying", boosting ? "[Flying (%sx boost)]" : "[Flying]",
					boosting ? fmt(flyBoost) : null));
			if (mc.options.keyShift.isDown() || sneak) out.add(tr("descending", "[Descending]", null));
			return out;
		}
		if (sprint) out.add(tr("sprint_toggled", "[Sprinting (Toggled)]", null));
		else if (mc.options.keySprint.isDown()) out.add(tr("sprint_held", "[Sprinting (Key Held)]", null));
		else if (mc.player.isSprinting()) out.add(tr("sprint_vanilla", "[Sprinting (Vanilla)]", null));
		if (sneak) out.add(tr("sneak_toggled", "[Sneaking (Toggled)]", null));
		else if (mc.options.keyShift.isDown()) out.add(tr("sneak_held", "[Sneaking (Key Held)]", null));
		return out;
	}

	private static String fmt(float f) {
		return f == Math.rint(f) ? String.valueOf((int) f) : String.valueOf(f);
	}

	private static String tr(String key, String fallback, Object arg) {
		String k = "newgen_client.toggle_sneak." + key;
		return (arg == null ? Component.translatableWithFallback(k, fallback)
				: Component.translatableWithFallback(k, fallback, arg)).getString();
	}

	void tick(Minecraft mc) {
		if (!enabled || mc.player == null) {
			sneakState.set(false);
			sprintState.set(false);
			sneak = sprint = noDoubleTap = boosting = false;
			flyBoost = 1f;
			return;
		}
		// Keys read as released while a screen is open, so the toggles survive opening the inventory.
		boolean sneakDown = sneakKey.value != -1 ? sneakKey.isDown(mc) : mc.options.keyShift.isDown();
		boolean sprintDown = sprintKey.value != -1 ? sprintKey.isDown(mc) : mc.options.keySprint.isDown();
		sneakState.update(sneakDown);
		sprintState.update(sprintDown);
		if (!toggleSneak.value) sneakState.set(false);
		if (!toggleSprint.value) sprintState.set(false);

		boolean flying = mc.player.getAbilities().flying;
		if (flying && !wasFlying && unsneakOnFly.value) sneakState.set(false);
		wasFlying = flying;

		// Sneak while flying means "descend"; a stuck toggle would sink the player.
		sneak = sneakState.on() && (flySneak.value || !flying);
		sprint = sprintState.on();
		noDoubleTap = disableDoubleTap.value;

		// Fly boost only where flying fast is allowed anyway: creative / spectator flight or your own world.
		boolean allowed = mc.player.getAbilities().instabuild || mc.player.isSpectator() || mc.hasSingleplayerServer();
		boosting = boost.value && allowed && flying;
		flyBoost = boosting ? (float) boostAmount.get() : 1f;
	}
}
