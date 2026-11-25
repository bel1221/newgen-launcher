package net.newgen.client.mods.utility;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Setting;

/** Turns a held Setting.Key into single presses. Call clicked() once per client tick. */
final class KeyPoll {
	private boolean wasDown;

	boolean clicked(Minecraft mc, Setting.Key key) {
		boolean down = key.isDown(mc);
		boolean clicked = down && !wasDown;
		wasDown = down;
		return clicked;
	}
}
