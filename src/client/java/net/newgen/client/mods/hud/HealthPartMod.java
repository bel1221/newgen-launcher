package net.newgen.client.mods.hud;

import net.minecraft.util.Util;
import net.newgen.client.core.Setting;

// Wave 3 vanilla HUD
/** The heart bar as a vanilla part, plus smooth health: the hearts ease toward a new value instead of jumping. */
public class HealthPartMod extends VanillaPartMod {
	public final Setting.Bool smooth = bool("smooth_health", "Smooth health", true);
	public final Setting.Num smoothSpeed = num("smooth_speed", "Smooth health speed", 6, 1, 20, 1);
	private final Smoother smoother = new Smoother();

	public HealthPartMod() {
		super("health_bar", "Health Bar", "Hide, scale or move the hearts; smooth health", 0.5f, 1f);
	}

	/** The health (in half hearts) the hearts show this frame. */
	public int shownHealth(int health) {
		if (!enabled || !smooth.value) {
			smoother.reset();
			return health;
		}
		return smoother.update(health, Util.getMillis(), smoothSpeed.get());
	}

	/** Eases a shown value toward its target, exponentially, then snaps when close. */
	static final class Smoother {
		private double shown = Double.NaN;
		private long last;

		void reset() {
			shown = Double.NaN;
		}

		int update(int target, long now, double speed) {
			if (Double.isNaN(shown)) {
				shown = target;
			} else {
				double dt = Math.max(0, Math.min(1000, now - last)) / 1000.0;
				shown += (target - shown) * (1 - Math.exp(-dt * speed));
				if (Math.abs(target - shown) < 0.5) shown = target;
			}
			last = now;
			return (int) Math.round(shown);
		}
	}
}
