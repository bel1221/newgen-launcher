package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** View bobbing split in two: the camera and the hand can each stop bobbing while walking. */
public class NoBobbingMod extends Mod {
	public static final NoBobbingMod I = new NoBobbingMod();
	public final Setting.Bool camera = bool("camera", "Camera stays still", true);
	public final Setting.Bool hand = bool("hand", "Hand stays still", false);

	private NoBobbingMod() {
		super("no_bobbing", "No Bobbing", "Stops the camera, the hand or both from bobbing while you walk");
	}
}
