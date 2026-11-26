package net.newgen.client.mods.utility;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/**
 * F1 extras: a key that shows the HUD again while held (or toggled), a key that hides only the hand, and an option to keep
 * the hand in F1 screenshots. Vanilla F1 already hides the hand, the HUD and the Newgen HUD elements. Hook: BetterF1HandMixin.
 */
public class BetterF1Mod extends Mod {
	public static BetterF1Mod I;

	private final Setting.Key peekKey = key("peek_key", "Show the HUD while held (in F1)", -1);
	private final Setting.Key handKey = key("hand_key", "Hide only the hand (press)", -1);
	private final Setting.Bool keepHand = bool("keep_hand", "Keep the hand visible in F1", false);

	private final KeyPoll handPoll = new KeyPoll();
	private boolean peeking, handHidden;

	public BetterF1Mod() {
		super("better_f1", "Better F1", "A key to peek at the HUD in F1, a key to hide only the hand, and F1 shots with your hand");
		I = this;
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		boolean hand = handPoll.clicked(mc, handKey);
		if (!enabled) {
			if (peeking) mc.options.hideGui = true;
			peeking = false;
			handHidden = false;
			return;
		}
		if (hand) handHidden = !handHidden;
		boolean peek = peekKey.isDown(mc);
		if (peek && mc.options.hideGui && !peeking) {
			mc.options.hideGui = false;
			peeking = true;
		} else if (!peek && peeking) {
			mc.options.hideGui = true;
			peeking = false;
		}
	}

	/** Whether the hand is hidden, given vanilla's hideGui value as read for the hand. */
	public static boolean hideHand(boolean hideGui) {
		BetterF1Mod m = I;
		if (m == null || !m.enabled) return hideGui;
		if (m.handHidden) return true;
		if (m.peeking) return false;
		return hideGui && !m.keepHand.value;
	}
}
