package net.newgen.client.mods.controls;

import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/** Look around in third person without turning the player. */
public class FreelookMod extends Mod {
	/** Read by the mixins: while true the mouse turns the camera, not the player. */
	public static boolean active;
	public static float yaw, pitch;

	private final Setting.Key key = key("key", "Key", GLFW.GLFW_KEY_LEFT_ALT);
	private final Setting.Choice mode = choice("mode", "Mode", "Hold", "Hold", "Toggle");
	private final Setting.Bool invertPitch = bool("invert_pitch", "Invert up/down", false);

	private static boolean invert;
	private boolean wasDown, toggled;
	private CameraType before;

	public FreelookMod() {
		super("freelook", "Freelook", "Hold a key to look around in third person");
		indicator(0.5f, 0.75f, mc -> active ? List.of("[Freelook]") : List.of());
	}

	void tick(Minecraft mc) {
		boolean down = key.isDown(mc);
		if (down && !wasDown) toggled = !toggled;
		wasDown = down;
		boolean want = enabled && mc.player != null && (mode.value.equals("Toggle") ? toggled : down);
		if (!want) toggled = false;
		invert = invertPitch.value;
		if (want == active) return;

		active = want;
		if (active) {
			yaw = mc.player.getYRot();
			pitch = mc.player.getXRot();
			before = mc.options.getCameraType();
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		} else {
			mc.options.setCameraType(before);
		}
	}

	/** Mouse movement while active; same scale as Entity.turn. */
	public static void turn(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = Mth.clamp(pitch + (float) dy * (invert ? -0.15f : 0.15f), -90f, 90f);
	}
}
