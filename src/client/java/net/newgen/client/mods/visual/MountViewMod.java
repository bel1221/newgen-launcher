package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** The animal (or boat) you ride, hidden in first person so it does not block the view. */
public class MountViewMod extends Mod {
	public static final MountViewMod I = new MountViewMod();
	public final Setting.Bool hide = bool("hide", "Hide in first person", true);

	private MountViewMod() {
		super("mount_view", "Mount View", "Hides the horse or boat you ride in first person, for a clear view");
	}
}
