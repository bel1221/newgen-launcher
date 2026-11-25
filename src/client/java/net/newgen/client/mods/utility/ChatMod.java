package net.newgen.client.mods.utility;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Longer chat history and timestamps. The hooks are in ChatComponentMixin. */
public class ChatMod extends Mod {
	public static ChatMod INSTANCE;

	private final Setting.Num history = num("history", "History length (lines)", 1000, 100, 5000, 100);
	private final Setting.Bool timestamps = bool("timestamps", "Timestamps", false);
	private final Setting.Bool seconds = bool("seconds", "Show seconds", false);
	private final Setting.Bool twelveHour = bool("twelve_hour", "12-hour clock", false);

	public ChatMod() {
		super("chat", "Chat", "Longer chat history and message timestamps");
		INSTANCE = this;
	}

	public static int historyLimit(int vanilla) {
		ChatMod m = INSTANCE;
		return m != null && m.enabled ? (int) m.history.get() : vanilla;
	}

	public static Component decorate(Component message) {
		ChatMod m = INSTANCE;
		if (m == null || !m.enabled || !m.timestamps.value) return message;
		String pattern = (m.twelveHour.value ? "hh:mm" : "HH:mm") + (m.seconds.value ? ":ss" : "") + (m.twelveHour.value ? " a" : "");
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern(pattern));
		return Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY).append(message);
	}
}
