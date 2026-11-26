package net.newgen.client.mods.hud;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** The active Iris shader pack. Iris is read by reflection when it is installed; there is no dependency on it. */
public class ShadersMod extends HudMod {
	private final Setting.Bool hideOff = bool("hide_when_off", "Hide when off", false);
	private final Setting.Bool extension = bool("show_extension", "Show .zip extension", false);

	private static boolean looked;
	private static Object api;
	private static Method inUse, packName;

	private long last;
	/** The pack name; "" = shaders off; null = Iris not installed. */
	private String pack;

	public ShadersMod() {
		super("shaders", "Shaders", "The shader pack in use (with Iris)", 0f, 0.46f);
	}

	private static void lookUp() {
		looked = true;
		try {
			Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			api = c.getMethod("getInstance").invoke(null);
			inUse = c.getMethod("isShaderPackInUse");
		} catch (Throwable t) {
			api = null;
			return;
		}
		try {
			packName = Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName");
		} catch (Throwable t) {
			packName = null;
		}
	}

	private static String read() {
		if (!looked) lookUp();
		if (api == null) return null;
		try {
			if (!(Boolean) inUse.invoke(api)) return "";
			Object name = packName == null ? null : packName.invoke(null);
			if (name instanceof Optional<?> o) name = o.orElse(null);
			return name == null ? "?" : name.toString();
		} catch (Throwable t) {
			return "?";
		}
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (now - last >= 1000) {
			last = now;
			pack = read();
		}
		if (pack == null || pack.isEmpty()) {
			if (hideOff.value) return List.of();
			return List.of(I18n.get("newgen_client.hud.shaders.text", I18n.get("newgen_client.hud.shaders.none")));
		}
		String shown = pack;
		if (!extension.value && shown.toLowerCase().endsWith(".zip")) shown = shown.substring(0, shown.length() - 4);
		return List.of(I18n.get("newgen_client.hud.shaders.text", shown));
	}
}
