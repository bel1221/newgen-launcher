package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class OverlayMod extends Mod {
	public static final OverlayMod I = new OverlayMod();
	public final Setting.Bool hideFire = bool("hide_fire", "Hide fire overlay", false);
	public final Setting.Num lowerFire = num("lower_fire", "Lower fire overlay by", 0.3, 0, 0.6, 0.1);
	public final Setting.Bool hidePumpkin = bool("hide_pumpkin", "Hide pumpkin overlay", true);
	public final Setting.Bool hidePowderSnow = bool("hide_powder_snow", "Hide powder snow frost", false);

	private OverlayMod() {
		super("overlay", "Overlays", "Lowers or hides the fire, pumpkin and powder snow screen overlays");
	}
}
