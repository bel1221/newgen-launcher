package net.newgen.client.mods.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;
import net.newgen.client.core.Setting;

/** A screen listing the screenshots folder with a large preview; open folder, delete, copy path, favourite. */
public class ScreenshotManagerMod extends Mod {
	public static ScreenshotManagerMod I;

	private final Setting.Key openKey = key("open_key", "Open screenshot manager", -1);
	/** File names, one per line. */
	private final Setting.Hidden favourites = new Setting.Hidden("favourites", "");
	private final KeyPoll poll = new KeyPoll();

	public ScreenshotManagerMod() {
		super("screenshot_manager", "Screenshot Manager", "Browse, preview, favourite and delete your screenshots");
		I = this;
		action("open_manager", "Screenshots", "newgen_client.screenshot_manager.open", () -> open(Minecraft.getInstance()));
		settings.add(favourites);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (poll.clicked(mc, openKey) && enabled) open(mc);
		});
	}

	private void open(Minecraft mc) {
		mc.setScreen(new ScreenshotManagerScreen(this, mc.screen));
	}

	static File folder() {
		return new File(Minecraft.getInstance().gameDirectory, "screenshots");
	}

	/** The PNG files of the folder, newest first. */
	static List<File> list() {
		File[] files = folder().listFiles(f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
		List<File> out = new ArrayList<>();
		if (files != null) out.addAll(List.of(files));
		out.sort(Comparator.comparingLong(File::lastModified).reversed().thenComparing(File::getName, Comparator.reverseOrder()));
		return out;
	}

	boolean isFavourite(String name) {
		return names().contains(name);
	}

	void toggleFavourite(String name) {
		Set<String> set = names();
		if (!set.remove(name)) set.add(name);
		favourites.value = String.join("\n", set);
		Mods.save();
	}

	void forget(String name) {
		Set<String> set = names();
		if (set.remove(name)) {
			favourites.value = String.join("\n", set);
			Mods.save();
		}
	}

	private Set<String> names() {
		Set<String> set = new LinkedHashSet<>();
		for (String s : favourites.value.split("\n")) if (!s.isBlank()) set.add(s);
		return set;
	}

	/** A width and height that fit inside maxW x maxH, keeping the aspect ratio. */
	static int[] fit(int w, int h, int maxW, int maxH) {
		if (w <= 0 || h <= 0) return new int[] {0, 0};
		double s = Math.min((double) maxW / w, (double) maxH / h);
		return new int[] {Math.max(1, (int) Math.floor(w * s)), Math.max(1, (int) Math.floor(h * s))};
	}
}
