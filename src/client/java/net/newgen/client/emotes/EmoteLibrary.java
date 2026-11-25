package net.newgen.client.emotes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zigythebird.playeranimcore.animation.Animation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.newgen.client.NewgenClient;

/**
 * The bundled emotes: assets/newgen_client/emotes/&lt;id&gt;.emotecraft with a sidecar &lt;id&gt;.json
 * ({"author": ..., "sound": optional sound id}), the icon textures/emotes/&lt;id&gt;.png and the display name
 * newgen_client.emote.&lt;id&gt; in the lang files. Loaded once, on first use.
 */
public final class EmoteLibrary {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("newgen_client");
	private static Map<String, Emote> all;

	/** One emote. sound is null when it has none. */
	public record Emote(String id, Animation animation, String author, Identifier sound, boolean hasIcon) {
		public String name() {
			return I18n.get("newgen_client.emote." + id);
		}

		public Identifier icon() {
			return Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/emotes/" + id + ".png");
		}

		/** Draws the icon, or a plain glyph when the emote file had none. */
		public void drawIcon(net.minecraft.client.gui.GuiGraphics g, int x, int y, int size) {
			if (hasIcon) g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon(), x, y, 0, 0, size, size, 64, 64, 64, 64);
			else net.newgen.client.gui.Ui.glyph(g, net.newgen.client.gui.Ui.I_USER, x + size / 6, y + size / 6, size * 2 / 3, net.newgen.client.gui.Ui.SOFT);
		}
	}

	private EmoteLibrary() {
	}

	/** Every emote by id, sorted by id. */
	public static synchronized Map<String, Emote> all() {
		if (all == null) all = load(Minecraft.getInstance().getResourceManager());
		return all;
	}

	/** Loads the emotes on a background thread once the game's resources are ready, so no menu waits for them. */
	public static void preload() {
		Thread t = new Thread(EmoteLibrary::all, "Newgen emote loader");
		t.setDaemon(true);
		t.start();
	}

	public static Emote get(String id) {
		return id == null ? null : all().get(id);
	}

	private static Map<String, Emote> load(ResourceManager resources) {
		Map<String, Emote> out = new TreeMap<>();
		var files = resources.listResources("emotes", file -> file.getPath().endsWith(".emotecraft"));
		for (var entry : files.entrySet()) {
			Identifier file = entry.getKey();
			if (!file.getNamespace().equals(NewgenClient.ID)) continue;
			String path = file.getPath(), id = path.substring("emotes/".length(), path.length() - ".emotecraft".length());
			try {
				Animation animation;
				try (InputStream in = entry.getValue().open()) {
					animation = EmoteFile.read(in.readAllBytes());
				}
				String author = "";
				Identifier sound = null;
				var side = resources.getResource(Identifier.fromNamespaceAndPath(NewgenClient.ID, "emotes/" + id + ".json"));
				if (side.isPresent()) {
					try (var reader = new InputStreamReader(side.get().open(), StandardCharsets.UTF_8)) {
						JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
						if (json.has("author")) author = json.get("author").getAsString();
						if (json.has("sound")) sound = Identifier.parse(json.get("sound").getAsString());
					}
				}
				boolean icon = resources.getResource(Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/emotes/" + id + ".png")).isPresent();
				out.put(id, new Emote(id, animation, author, sound, icon));
			} catch (Throwable e) { // one bad file never takes the other emotes down
				LOGGER.warn("Emote {} could not be loaded", file, e);
			}
		}
		return Collections.unmodifiableMap(out);
	}
}
