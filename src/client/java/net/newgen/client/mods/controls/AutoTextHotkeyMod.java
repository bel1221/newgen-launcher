package net.newgen.client.mods.controls;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Keys that send a chat message or command. One send per key press. */
public class AutoTextHotkeyMod extends Mod {
	private static final int SLOTS = 5;

	private final Setting.Key[] keys = new Setting.Key[SLOTS];
	private final Setting.Text[] texts = new Setting.Text[SLOTS];
	private final boolean[] wasDown = new boolean[SLOTS];

	public AutoTextHotkeyMod() {
		super("auto_text_hotkey", "Auto Text Hotkey", "Send a chat message or command with one key");
		for (int i = 0; i < SLOTS; i++) {
			keys[i] = key("key_" + (i + 1), "Key " + (i + 1), -1);
			texts[i] = text("text_" + (i + 1), "Text " + (i + 1), "");
		}
	}

	void tick(Minecraft mc) {
		for (int i = 0; i < SLOTS; i++) {
			boolean down = enabled && keys[i].isDown(mc);
			if (down && !wasDown[i]) ControlsMods.send(mc, texts[i].value);
			wasDown[i] = down;
		}
	}
}
