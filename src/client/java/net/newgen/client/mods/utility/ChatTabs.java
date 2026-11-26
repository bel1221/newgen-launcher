package net.newgen.client.mods.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Chat tabs: All, Server (lines no online player wrote), Private (whispers), Mentions, plus custom tabs written as
 * "Name=regex; Name=regex". Picking a tab filters the chat; a tab that got a new line since it was last open shows a
 * dot.
 */
public final class ChatTabs {
	/** One tab. Built-in tabs have a lang key as label; custom ones their own name. */
	/** One tab. Built-in tabs have a lang key as label; custom ones their own name. invalid: its regex did not compile. */
	public record Tab(String id, String label, boolean builtIn, Pattern pattern, boolean invalid) {
		Tab(String id, String label, boolean builtIn, Pattern pattern) {
			this(id, label, builtIn, pattern, false);
		}
	}

	/** Longest custom regex accepted, longest part of a line matched, time budget per match. */
	static final int MAX_PATTERN = 200, MAX_TEXT = 512;
	static final long BUDGET_NANOS = 2_000_000L;

	/** Common whisper formats: "X whispers to you: …", "[X -> me] …", "From X: …", "[msg] …". */
	static final Pattern WHISPER = Pattern.compile(
			"(?i)(\\bwhispers to you\\b|^\\[?[A-Za-z0-9_]{2,16} ?(->|→|») ?(me|you)\\]?|^from [A-Za-z0-9_]{2,16}:|^\\[msg\\])");

	private static List<Tab> tabs = List.of();
	private static String builtFrom;
	private static int active;
	private static final Set<String> UNREAD = new HashSet<>();

	private ChatTabs() {
	}

	/** The tabs for this custom-tab table (rebuilt only when it changes). */
	public static List<Tab> tabs(String custom) {
		if (!custom.equals(builtFrom)) {
			tabs = build(custom);
			builtFrom = custom;
			if (active >= tabs.size()) active = 0;
		}
		return tabs;
	}

	static List<Tab> build(String custom) {
		List<Tab> list = new ArrayList<>(List.of(
				new Tab("all", "newgen_client.chat.tab.all", true, null),
				new Tab("server", "newgen_client.chat.tab.server", true, null),
				new Tab("private", "newgen_client.chat.tab.private", true, WHISPER),
				new Tab("mentions", "newgen_client.chat.tab.mentions", true, null)));
		for (String entry : custom.split(";")) {
			int eq = entry.indexOf('=');
			if (eq <= 0) continue;
			String name = entry.substring(0, eq).strip();
			if (name.isEmpty()) continue;
			String regex = entry.substring(eq + 1).strip();
			Pattern pattern = null;
			if (regex.length() <= MAX_PATTERN) {
				try {
					pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				} catch (PatternSyntaxException e) {
					// shown as an invalid tab (marked "(!)"), matching nothing; the other tabs still work
				}
			}
			list.add(new Tab("custom:" + name, pattern == null ? name + " (!)" : name, false, pattern, pattern == null));
		}
		return list;
	}

	/** Whether a line belongs in a tab. fromPlayer: an online player wrote it; mention: it mentions the player. */
	public static boolean belongs(Tab tab, String text, boolean fromPlayer, boolean mention) {
		return switch (tab.id()) {
			case "all" -> true;
			case "server" -> !fromPlayer;
			case "mentions" -> mention;
			case "private" -> tab.pattern().matcher(text).find();
			default -> tab.pattern() != null && safeFind(tab.pattern(), text);
		};
	}

	/**
	 * find() on at most MAX_TEXT characters within BUDGET_NANOS: a player's regex can backtrack catastrophically
	 * (e.g. (a+)+$), so the text throws once the budget is spent, and that counts as no match.
	 */
	static boolean safeFind(Pattern pattern, String text) {
		String part = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
		try {
			return pattern.matcher(new Budgeted(part, System.nanoTime() + BUDGET_NANOS)).find();
		} catch (OutOfBudget e) {
			return false;
		}
	}

	private static final class OutOfBudget extends RuntimeException {
		OutOfBudget() {
			super(null, null, false, false);
		}
	}

	/** Text that throws OutOfBudget when read after its deadline (checked every 256 reads). */
	private record Budgeted(String text, long deadline, int[] reads) implements CharSequence {
		Budgeted(String text, long deadline) {
			this(text, deadline, new int[1]);
		}

		@Override
		public char charAt(int index) {
			if ((++reads[0] & 255) == 0 && System.nanoTime() > deadline) throw new OutOfBudget();
			return text.charAt(index);
		}

		@Override
		public int length() {
			return text.length();
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new Budgeted(text.substring(start, end), deadline, reads);
		}

		@Override
		public String toString() {
			return text;
		}
	}

	public static int active() {
		return active;
	}

	public static Tab activeTab() {
		return active < tabs.size() ? tabs.get(active) : null;
	}

	public static void select(int index) {
		active = index;
		if (index < tabs.size()) UNREAD.remove(tabs.get(index).id());
	}

	/** A new line came in: every other tab it belongs to gets the unread dot. */
	public static void arrived(String text, boolean fromPlayer, boolean mention) {
		for (int i = 0; i < tabs.size(); i++)
			if (i != active && !tabs.get(i).id().equals("all") && belongs(tabs.get(i), text, fromPlayer, mention)) UNREAD.add(tabs.get(i).id());
	}

	public static boolean unread(Tab tab) {
		return UNREAD.contains(tab.id());
	}
}
