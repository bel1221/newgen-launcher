package net.newgen.client.mods.utility;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/**
 * Chat: history length, timestamps, stacking repeats, mentions, links, emoji shortcodes, shortcuts, chat size and the
 * peek / hide keys. The hooks are in ChatComponentMixin and ChatScreenMixin. Setting ids of the first version
 * (history, timestamps, seconds, twelve_hour) are kept, so older configs still load.
 */
public class ChatMod extends Mod {
	public static ChatMod INSTANCE;

	// History
	private final Setting.Num history = num("history", "History length (lines)", 1000, 100, 5000, 100);
	private final Setting.Bool unlimited = bool("no_line_limit", "No line limit", false);
	private final Setting.Bool stack = bool("stack_repeats", "Stack repeated messages", true);
	// Timestamps
	private final Setting.Bool timestamps = bool("timestamps", "Timestamps", false);
	private final Setting.Bool seconds = bool("seconds", "Show seconds", false);
	private final Setting.Bool twelveHour = bool("twelve_hour", "12-hour clock", false);
	private final Setting.Choice timestampStyle = choice("timestamp_style", "Timestamp style", "[12:00]", "[12:00]", "(12:00)", "12:00 |", "12:00");
	private final Setting.Color timestampColor = color("timestamp_color", "Timestamp colour", 0xFFAAAAAA);
	// Mentions
	private final Setting.Bool mentions = bool("mentions", "Highlight mentions of your name", true);
	private final Setting.Text mentionWords = text("mention_words", "Also highlight (words, comma separated)", "");
	private final Setting.Bool mentionSound = bool("mention_sound", "Mention sound", true);
	private final Setting.Color mentionColor = color("mention_color", "Mention colour", 0xFFFFD25A);
	// Links and typing
	private final Setting.Bool copyDelete = bool("copy_delete", "Right-click copies a message (Shift: deletes)", true);
	private final Setting.Bool underlineLinks = bool("underline_links", "Underline links", true);
	private final Setting.Bool emoji = bool("emoji", "Emoji shortcodes (:heart: :star: …)", true);
	private final Setting.Text shortcuts = text("shortcuts", "Shortcuts (gg=Good game!; /h=/home)", "");
	// Size
	private final Setting.Bool customSize = bool("custom_size", "Custom chat size", false);
	private final Setting.Num width = num("width", "Width (pixels)", 320, 80, 640, 10);
	private final Setting.Num focusedHeight = num("focused_height", "Height while typing (pixels)", 180, 20, 400, 10);
	private final Setting.Num unfocusedHeight = num("unfocused_height", "Height while playing (pixels)", 90, 20, 400, 10);
	// Look
	private final Setting.Bool heads = bool("heads", "Chat heads", true);
	private final Setting.Bool headsOnRepeat = bool("heads_repeat", "Head on every message from the same player", false);
	private final Setting.Bool customBackground = bool("custom_background", "Custom background colour", false);
	private final Setting.Color backgroundColor = color("background_color", "Background colour", 0x80000000);
	private final Setting.Bool noScrollbar = bool("no_scrollbar", "Hide the scrollbar", false);
	private final Setting.Bool smooth = bool("smooth", "Slide new messages in", true);
	private final Setting.Num moveUp = num("move_up", "Move chat up (pixels)", 0, 0, 200, 2);
	// Keys
	private final Setting.Key peekKey = key("peek_key", "Peek key (hold to read chat)", -1);
	private final Setting.Key toggleKey = key("toggle_key", "Hide / show chat key", -1);

	/** The last message as shown before stacking (plain text) and how often it came in a row. */
	private String lastText;
	private int repeats;
	private String lastSender;
	private long lastMessageAt;
	private boolean hidden, toggleWasDown;

	public ChatMod() {
		super("chat", "Chat", "Timestamps, stacked repeats, mentions, emoji, shortcuts, chat size and peek");
		INSTANCE = this;
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		boolean down = enabled && toggleKey.isDown(mc);
		if (down && !toggleWasDown) hidden = !hidden;
		toggleWasDown = down;
		if (!enabled) hidden = false;
	}

	private static ChatMod on() {
		ChatMod m = INSTANCE;
		return m != null && m.enabled ? m : null;
	}

	public static int historyLimit(int vanilla) {
		ChatMod m = on();
		if (m == null) return vanilla;
		return m.unlimited.value ? Integer.MAX_VALUE : (int) m.history.get();
	}

	// ---- incoming messages ----

	/** What ChatComponent needs from us to stack a repeat: drop the newest message it shows. */
	public interface Lines {
		void newgen$removeNewest();

		/** The message shown at screen height y in the open chat, or null. */
		net.minecraft.client.GuiMessage newgen$messageAt(double y);

		void newgen$delete(net.minecraft.client.GuiMessage message);
	}

	/** The message as it will be shown: stacked, mentions highlighted, links underlined, timestamped. */
	public static Component process(Component message, Lines lines) {
		ChatMod m = on();
		if (m == null) return message;
		String text = message.getString();
		Component shown = message;
		if (m.stack.value && text.equals(m.lastText) && !text.isBlank()) {
			m.repeats++;
			lines.newgen$removeNewest();
			shown = message.copy().append(Component.literal(" (x" + m.repeats + ")").withColor(0xFFAAAAAA));
		} else {
			m.lastText = text;
			m.repeats = 1;
		}
		m.lastMessageAt = System.currentTimeMillis();
		net.minecraft.client.multiplayer.PlayerInfo sender = sender(text);
		if (m.mentions.value && mentions(withoutSender(text, sender), m.mentionTerms())) {
			shown = Component.literal("▌ ").withColor(m.mentionColor.value).append(shown.copy().withColor(m.mentionColor.value));
			if (m.mentionSound.value && m.repeats == 1)
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4f, 0.6f));
		}
		if (m.underlineLinks.value) shown = underlineLinks(shown);
		// Chat heads: the sender's face from the tab list (the skin the game already has; nothing is downloaded for it)
		String senderName = sender == null ? null : sender.getProfile().name();
		if (m.heads.value && sender != null && (m.headsOnRepeat.value || !senderName.equals(m.lastSender) || m.repeats > 1))
			shown = Component.object(new net.minecraft.network.chat.contents.objects.PlayerSprite(
					net.minecraft.world.item.component.ResolvableProfile.createResolved(sender.getProfile()), true)).append(" ").append(shown);
		m.lastSender = senderName;
		if (m.timestamps.value) shown = m.stamp(LocalTime.now()).append(shown);
		return shown;
	}

	/** The tab-list player who wrote this line: the first online name found in the line's first 40 characters. */
	private static net.minecraft.client.multiplayer.PlayerInfo sender(String text) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) return null;
		String head = text.length() > 40 ? text.substring(0, 40) : text;
		net.minecraft.client.multiplayer.PlayerInfo best = null;
		int at = Integer.MAX_VALUE;
		for (net.minecraft.client.multiplayer.PlayerInfo info : connection.getOnlinePlayers()) {
			int i = wordIndex(head, info.getProfile().name());
			if (i >= 0 && i < at) {
				at = i;
				best = info;
			}
		}
		return best;
	}

	private static int wordIndex(String text, String word) {
		Matcher match = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(word) + "(?![A-Za-z0-9_])").matcher(text);
		return match.find() ? match.start() : -1;
	}

	/** The line without its sender's name, so a player's own messages don't count as mentioning them. */
	static String withoutSender(String text, net.minecraft.client.multiplayer.PlayerInfo sender) {
		if (sender == null) return text;
		int i = wordIndex(text, sender.getProfile().name());
		return i < 0 ? text : text.substring(0, i) + text.substring(i + sender.getProfile().name().length());
	}

	// ---- look ----

	/** Whether the chat's drawing is restyled (background colour or no scrollbar). */
	public static boolean restyles() {
		ChatMod m = on();
		return m != null && (m.customBackground.value || m.noScrollbar.value);
	}

	/** The background colour for a vanilla background fill of the given alpha (vanilla's opacity and fading apply). */
	public static int background(int vanillaAlpha) {
		ChatMod m = on();
		if (m == null || !m.customBackground.value) return vanillaAlpha << 24;
		int c = m.backgroundColor.value;
		int alpha = (c >>> 24) * vanillaAlpha / 255;
		return alpha << 24 | c & 0xFFFFFF;
	}

	public static boolean copyAndDelete() {
		ChatMod m = on();
		return m != null && m.copyDelete.value;
	}

	public static boolean hideScrollbar() {
		ChatMod m = on();
		return m != null && m.noScrollbar.value;
	}

	/** How far up the chat is drawn: the "move up" setting, plus a short slide for a new message. */
	public static int verticalOffset() {
		ChatMod m = on();
		if (m == null) return 0;
		int offset = (int) m.moveUp.get();
		if (m.smooth.value) offset -= slide(System.currentTimeMillis() - m.lastMessageAt);
		return offset;
	}

	/** Pixels a message still has to slide up, ms after it arrived: 9 at once, 0 after 150 ms (ease out). */
	static int slide(long ms) {
		if (ms < 0 || ms >= 150) return 0;
		double t = 1 - ms / 150.0;
		return (int) Math.round(9 * t * t);
	}

	private List<String> mentionTerms() {
		List<String> terms = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) terms.add(mc.player.getGameProfile().name());
		for (String w : mentionWords.value.split(",")) if (!w.isBlank()) terms.add(w.strip());
		return terms;
	}

	/** Whether text contains one of the terms as a whole word, ignoring case. */
	static boolean mentions(String text, List<String> terms) {
		for (String term : terms) {
			if (term.isBlank()) continue;
			Matcher match = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(term) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE).matcher(text);
			if (match.find()) return true;
		}
		return false;
	}

	MutableComponent stamp(LocalTime time) {
		String pattern = (twelveHour.value ? "h:mm" : "HH:mm") + (seconds.value ? ":ss" : "") + (twelveHour.value ? " a" : "");
		return Component.literal(stampText(time.format(DateTimeFormatter.ofPattern(pattern, Locale.ROOT)), timestampStyle.value)).withColor(timestampColor.value);
	}

	static String stampText(String time, String style) {
		return switch (style) {
			case "(12:00)" -> "(" + time + ") ";
			case "12:00 |" -> time + " | ";
			case "12:00" -> time + " ";
			default -> "[" + time + "] ";
		};
	}

	/** The same message with every part that opens a URL underlined. */
	static Component underlineLinks(Component message) {
		MutableComponent copy = message.plainCopy();
		Style style = message.getStyle();
		if (style.getClickEvent() instanceof ClickEvent.OpenUrl) style = style.withUnderlined(true);
		copy.setStyle(style);
		for (Component sibling : message.getSiblings()) copy.append(underlineLinks(sibling));
		return copy;
	}

	// ---- outgoing messages ----

	private static final Map<String, String> EMOJI = new LinkedHashMap<>();

	static {
		String[][] pairs = {{"heart", "❤"}, {"star", "★"}, {"smile", "☺"}, {"sad", "☹"}, {"check", "✔"}, {"x", "✘"}, {"sun", "☀"},
				{"cloud", "☁"}, {"umbrella", "☂"}, {"snowman", "☃"}, {"skull", "☠"}, {"music", "♫"}, {"arrow", "➜"}, {"peace", "☮"},
				{"yinyang", "☯"}, {"flower", "✿"}, {"sword", "⚔"}, {"warning", "⚠"}, {"crown", "♛"}, {"diamond", "♦"}, {"spade", "♠"},
				{"club", "♣"}, {"shrug", "¯\\_(ツ)_/¯"}, {"tableflip", "(╯°□°)╯︵ ┻━┻"}};
		for (String[] p : pairs) EMOJI.put(p[0], p[1]);
	}

	/** What the player typed, before it is sent: shortcuts expanded, then :emoji: shortcodes replaced. */
	public static String outgoing(String typed) {
		ChatMod m = on();
		if (m == null) return typed;
		String out = expandShortcut(typed, m.shortcuts.value);
		return m.emoji.value && !out.startsWith("/") ? replaceEmoji(out) : out;
	}

	/** "gg=Good game!; /h=/home": a message that is exactly a shortcut's name becomes its text. */
	static String expandShortcut(String typed, String table) {
		String key = typed.strip();
		for (String entry : table.split(";")) {
			int eq = entry.indexOf('=');
			if (eq <= 0) continue;
			if (entry.substring(0, eq).strip().equalsIgnoreCase(key)) return entry.substring(eq + 1).strip();
		}
		return typed;
	}

	static String replaceEmoji(String text) {
		Matcher m = Pattern.compile(":([a-z]+):").matcher(text);
		StringBuilder out = new StringBuilder();
		while (m.find()) m.appendReplacement(out, Matcher.quoteReplacement(EMOJI.getOrDefault(m.group(1), m.group())));
		m.appendTail(out);
		return out.toString();
	}

	// ---- size and visibility ----

	public static int width(int vanilla) {
		ChatMod m = on();
		return m != null && m.customSize.value ? (int) m.width.get() : vanilla;
	}

	public static int height(int vanilla, boolean focused) {
		ChatMod m = on();
		if (m == null || !m.customSize.value) return vanilla;
		return (int) (focused ? m.focusedHeight : m.unfocusedHeight).get();
	}

	/** Holding the peek key shows the chat as if it were open (every line, no fading). */
	public static boolean peeking() {
		ChatMod m = on();
		return m != null && m.peekKey.isDown(Minecraft.getInstance());
	}

	public static boolean hidden() {
		ChatMod m = on();
		return m != null && m.hidden;
	}
}
