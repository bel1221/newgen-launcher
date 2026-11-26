package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Pure helpers of the Tab mod, kept free of game classes so they can be unit tested. */
public final class TabLogic {
	private TabLogic() {
	}

	/** Colour (ARGB) for a ping in milliseconds; negative = unknown. */
	public static int pingColor(int ms) {
		if (ms < 0) return 0xFFAAAAAA;
		if (ms < 75) return 0xFF55FF55;
		if (ms < 150) return 0xFFC8FF55;
		if (ms < 250) return 0xFFFFFF55;
		if (ms < 400) return 0xFFFFAA00;
		return 0xFFFF5555;
	}

	/** The already sorted list, with the player matching self moved to the top if asked, cut to limit entries. */
	public static <T> List<T> arrange(List<T> sorted, Predicate<T> self, boolean selfOnTop, int limit) {
		List<T> out = new ArrayList<>(sorted.size());
		if (selfOnTop) {
			for (T t : sorted) if (self.test(t)) out.add(t);
			for (T t : sorted) if (!self.test(t)) out.add(t);
		} else {
			out.addAll(sorted);
		}
		return out.size() > limit ? new ArrayList<>(out.subList(0, Math.max(0, limit))) : out;
	}
}
