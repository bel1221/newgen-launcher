package net.newgen.client.mods.controls;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Sends a chat message or command once when something happens: joining a world, dying. */
public class AutoTextActionsMod extends Mod {
	private final Setting.Text onJoin = text("on_join", "On join", "");
	private final Setting.Text onDeath = text("on_death", "On death", "");

	private Object lastConnection;
	private boolean wasDead;

	public AutoTextActionsMod() {
		super("auto_text_actions", "Auto Text Actions", "Send a message or command when you join or die");
	}

	void tick(Minecraft mc) {
		if (mc.player == null) {
			lastConnection = null;
			wasDead = false;
			return;
		}
		// The connection object is new for every world or server joined.
		boolean joined = mc.player.connection != lastConnection;
		lastConnection = mc.player.connection;
		boolean dead = mc.player.isDeadOrDying();
		if (enabled) {
			if (joined) ControlsMods.send(mc, onJoin.value);
			if (dead && !wasDead) ControlsMods.send(mc, onDeath.value);
		}
		wasDead = dead;
	}
}
