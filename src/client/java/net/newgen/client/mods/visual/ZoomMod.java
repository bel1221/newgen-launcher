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
	private final Setting.Choice mode = choice("mode", "Zoom mode", "Hold", "Hold", "Toggle");
	private final Setting.Bool scroll = bool("scroll", "Scroll to change zoom", true);
	private final Setting.Bool hand = bool("hand", "Show hand while zoomed", true);
	private final Setting.Choice curve = choice("curve", "Smooth zoom curve", "Exponential", "Exponential", "Linear", "Ease in", "Ease out", "Ease in-out");

	private float prev, cur, progress;
	private boolean held, wasDown, toggled, savedSmoothCamera;
	/** The zoom this time, changed with the wheel; back to the factor setting each time zooming starts. */
	private double zoom = 4;

	private ZoomMod() {
		super("zoom", "Zoom", "Hold a key to zoom in");
		indicator(0.5f, 0.7f, mc -> held ? List.of("[Zoom " + String.format("%.1f", zoom) + "x]") : List.of());
	}

	public void tick(Minecraft mc) {
		boolean down = enabled && key.isDown(mc);
		if (down && !wasDown) toggled = !toggled;
		wasDown = down;
		if (!enabled) toggled = false;
		boolean now = enabled && (mode.value.equals("Toggle") ? toggled : down);
		if (now != held) {
			if (now) {
				zoom = factor.get();
				savedSmoothCamera = mc.options.smoothCamera;
				if (cinematic.value) mc.options.smoothCamera = true;
			} else {
				mc.options.smoothCamera = savedSmoothCamera;
			}
			held = now;
		}
		prev = cur;
		float target = now ? 1 : 0;
		if (!smooth.value) {
			progress = cur = target;
		} else if (curve.value.equals("Exponential")) {
			cur = cur + (target - cur) * 0.5f;
			if (Math.abs(target - cur) < 0.01f) cur = target;
			progress = cur;
		} else {
			progress = Mth.clamp(progress + (now ? 0.25f : -0.25f), 0, 1);
			cur = VisualMath.ease(curve.value, progress);
		}
	}

	/** True while the zoom key is in effect. */
	public boolean zooming() {
		return enabled && held;
	}

	/** Mouse wheel while zooming; true when the wheel was used for the zoom (the hotbar must not scroll). */
	public boolean onScroll(double amount) {
		if (!zooming() || !scroll.value || amount == 0) return false;
		zoom = VisualMath.scrollZoom(zoom, amount, 1.1, 50);
		return true;
	}

	/** False while zoomed with "Show hand while zoomed" off. */
	public boolean showHand() {
		return !zooming() || hand.value;
	}

	/** What the FOV is divided by this frame; 1 = no zoom. */
	public float divisor(float partialTick) {
		return 1 + ((float) zoom - 1) * Mth.lerp(partialTick, prev, cur);
	}
}
