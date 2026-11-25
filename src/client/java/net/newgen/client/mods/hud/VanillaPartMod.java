package net.newgen.client.mods.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Hides, scales or moves one part of the vanilla HUD. A mixin calls begin/end around the vanilla draw method. */
public class VanillaPartMod extends Mod {
	public final Setting.Bool hide = bool("hide", "Hide", false);
	public final Setting.Num scale = num("scale", "Scale", 1.0, 0.5, 2.0, 0.1);
	public final Setting.Num offsetX = num("offset_x", "Move right", 0, -400, 400, 1);
	public final Setting.Num offsetY = num("offset_y", "Move down", 0, -400, 400, 1);

	// Point the part scales around, 0..1 of the screen, so it stays attached to its usual edge.
	private final float pivotX, pivotY;
	private boolean pushed;

	public VanillaPartMod(String id, String name, String description, float pivotX, float pivotY) {
		super(id, name, description);
		this.pivotX = pivotX;
		this.pivotY = pivotY;
	}

	/** Call before the vanilla part is drawn. Returns true if it must not be drawn at all. */
	public boolean begin(GuiGraphics g) {
		if (!enabled) return false;
		if (hide.value) return true;
		float s = (float) scale.get();
		float px = pivotX * g.guiWidth(), py = pivotY * g.guiHeight();
		g.pose().pushMatrix();
		g.pose().translate(px + (float) offsetX.get(), py + (float) offsetY.get());
		g.pose().scale(s, s);
		g.pose().translate(-px, -py);
		pushed = true;
		return false;
	}

	/** Call after the vanilla part is drawn. */
	public void end(GuiGraphics g) {
		if (pushed) g.pose().popMatrix();
		pushed = false;
	}
}
