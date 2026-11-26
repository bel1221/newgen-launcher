package net.newgen.client.mods.visual;

/** Pure maths behind some visual mods (no game classes, so it can be unit tested). */
public final class VisualMath {
	private VisualMath() {
	}

	/** Zoom: one wheel notch multiplies (up) or divides (down) the zoom by 1.25, kept inside [min, max]. */
	public static double scrollZoom(double zoom, double notches, double min, double max) {
		if (notches == 0) return Math.max(min, Math.min(max, zoom));
		double z = zoom * Math.pow(1.25, Math.signum(notches) * Math.min(Math.abs(notches), 10));
		return Math.max(min, Math.min(max, z));
	}

	/** Zoom: the eased zoom amount for progress p in [0, 1]. */
	public static float ease(String curve, float p) {
		p = Math.max(0, Math.min(1, p));
		return switch (curve) {
			case "Ease in" -> p * p;
			case "Ease out" -> 1 - (1 - p) * (1 - p);
			case "Ease in-out" -> p < 0.5f ? 2 * p * p : 1 - 2 * (1 - p) * (1 - p);
			default -> p;
		};
	}

	/**
	 * FOV: vanilla's dynamic-FOV modifier with each cause scaled by its own factor (1 = vanilla, 0 = none, 2 = double).
	 * speedRatio is movement speed / walking speed; sprintRatio the part of it that comes from sprinting (1.3 or 1);
	 * bowPull is in [0, 1].
	 */
	public static float fovModifier(boolean flying, float speedRatio, float sprintRatio, float bowPull,
			double fly, double sprint, double speed, double slow, double bow) {
		float g = 1;
		if (flying) g *= 1 + 0.1f * (float) fly;
		float s = 1 + (sprintRatio - 1) * (float) sprint;
		float rest = sprintRatio == 0 ? 1 : speedRatio / sprintRatio;
		float d = rest - 1;
		float other = 1 + d * (float) (d > 0 ? speed : slow);
		float ratio = s * other;
		g *= (ratio + 1) / 2;
		if (bowPull > 0) g *= 1 - bowPull * bowPull * 0.15f * (float) bow;
		return g;
	}
}
