package net.newgen.client.mods.utility;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/**
 * Resource pack screen: search by name (several words, accents and case ignored), favourites (right-click a pack or the
 * star button) listed first among the available packs. Hooks: PackSelectionScreenMixin, PackEntryMixin.
 */
public class PackOrganizerMod extends Mod {
	public static PackOrganizerMod I;

	public final Setting.Bool favouritesFirst = bool("favourites_first", "Favourites first", true);
	public final Setting.Bool searchDescriptions = bool("search_descriptions", "Search descriptions too", true);
	public final Setting.Bool favouritesOnly = bool("favourites_only", "Show only favourites (available packs)", false);
	/** A favourite changed from a list click: the screen sorts again on its next tick. */
	public boolean dirty;
	/** Pack ids, one per line. */
	private final Setting.Hidden favourites = new Setting.Hidden("favourites", "");

	/** Added to PackSelectionScreen: filter and sort the lists again (after a favourite changes). */
	public interface Refresh {
		void newgen$refreshPacks();
	}

	public PackOrganizerMod() {
		super("pack_organizer", "Pack Organizer", "Search resource packs and keep your favourites at the top");
		I = this;
		settings.add(favourites);
	}

	public static boolean active() {
		return I != null && I.enabled;
	}

	public boolean isFavourite(String id) {
		return ids().contains(id);
	}

	public void toggleFavourite(String id) {
		Set<String> set = ids();
		if (!set.remove(id)) set.add(id);
		favourites.value = String.join("\n", set);
		net.newgen.client.core.Mods.save();
	}

	private Set<String> ids() {
		Set<String> set = new LinkedHashSet<>();
		for (String s : favourites.value.split("\n")) if (!s.isBlank()) set.add(s);
		return set;
	}

	/** Lower case without accents ("Đồ" -> "do"). */
	static String fold(String s) {
		String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		return n.replace('đ', 'd');
	}

	/** Every word of the query appears in at least one of the fields. A blank query matches everything. */
	public static boolean matches(String query, String... fields) {
		if (query == null || query.isBlank()) return true;
		String all = fold(String.join("\n", Arrays.stream(fields).map(f -> f == null ? "" : f).toList()));
		for (String word : fold(query).trim().split("\\s+")) if (!all.contains(word)) return false;
		return true;
	}
}
