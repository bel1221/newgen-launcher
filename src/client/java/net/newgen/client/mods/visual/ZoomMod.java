package net.newgen.client.mods.visual;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

public class ZoomMod extends Mod {
	public static final ZoomMod I = new ZoomMod();
	private final Setting.Key key = key("key", "Zoom key (hold)", GLFW.GLFW_KEY_C);
	private final Setting.Num factor = num("factor", "Zoom factor", 4, 1.5, 10, 0.5);
	private final Setting.Bool smooth = bool("smooth", "Smooth zoom", true);
	private final Setting.Bool cinematic = bool("cinematic", "Cinematic camera while zoomed", false);

	private float prev, cur;
	private boolean held, savedSmoothCamera;

	private ZoomMod() {
		super("zoom", "Zoom", "Hold a key to zoom in");
		indicator(0.5f, 0.7f, mc -> held ? List.of("[Zoom " + factor.text() + "x]") : List.of());
	}

	public void tick(Minecraft mc) {
		boolean now = enabled && key.isDown(mc);
		if (now != held) {
			if (now) {
				savedSmoothCamera = mc.options.smoothCamera;
				if (cinematic.value) mc.options.smoothCamera = true;
			} else {
				mc.options.smoothCamera = savedSmoothCamera;
			}
			held = now;
		}
		prev = cur;
		float target = now ? 1 : 0;
		cur = smooth.value ? cur + (target - cur) * 0.5f : target;
		if (Math.abs(target - cur) < 0.01f) cur = target;
	}

	/** What the FOV is divided by this frame; 1 = no zoom. */
	public float divisor(float partialTick) {
		return 1 + ((float) factor.get() - 1) * Mth.lerp(partialTick, prev, cur);
	}
}
