package net.newgen.client.mods.controls;

import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/** Switches to another camera view only while a key is held. */
public class SnaplookMod extends Mod {
	private final Setting.Key key = key("key", "Key", GLFW.GLFW_KEY_V);
	private final Setting.Choice view = choice("view", "View", "Front", "Front", "Back", "First person");

	private CameraType before; // null = not snapped

	public SnaplookMod() {
		super("snaplook", "Snaplook", "Hold a key to switch camera view");
		indicator(0.5f, 0.8f, mc -> before != null ? List.of("[Snaplook: " + view.value + "]") : List.of());
	}

	void tick(Minecraft mc) {
		boolean want = enabled && mc.player != null && !FreelookMod.active && key.isDown(mc);
		if (want && before == null) {
			before = mc.options.getCameraType();
			mc.options.setCameraType(switch (view.value) {
				case "Back" -> CameraType.THIRD_PERSON_BACK;
				case "First person" -> CameraType.FIRST_PERSON;
				default -> CameraType.THIRD_PERSON_FRONT;
			});
		} else if (!want && before != null) {
			mc.options.setCameraType(before);
			before = null;
		}
	}
}
