package net.newgen.client.mods.utility;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Shows another name in place of your own in all text the game draws (chat, tab list, scoreboard). Hook: StringDecomposerMixin. */
public class NickHiderMod extends Mod {
	public static NickHiderMod INSTANCE;

	private final Setting.Text nick = text("nick", "Shown name", "You");

	public NickHiderMod() {
		super("nick_hider", "Nick Hider", "Hide your name on your own screen (for streaming)");
		INSTANCE = this;
	}

	public static String apply(String text) {
		NickHiderMod m = INSTANCE;
		if (m == null || !m.enabled || m.nick.value.isEmpty()) return text;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return text;
		String name = mc.getUser().getName();
		return name.isEmpty() || !text.contains(name) ? text : text.replace(name, m.nick.value);
	}
}
