package net.newgen.client.mods.visual;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import net.newgen.client.mixin.visual.OverlayTextureAccessor;

public class HitColorMod extends Mod {
	public static final HitColorMod I = new HitColorMod();
	private static final int VANILLA = 0xB2FF0000;
	private final Setting.Color color = color("color", "Colour", 0xFFFF0000);
	private final Setting.Num strength = num("strength", "Strength", 0.3, 0.1, 1, 0.1);

	private int applied = VANILLA;

	private HitColorMod() {
		super("hit_color", "Hit Color", "Colour of mobs and players when they are hit");
	}

	/** Repaints the red rows of the vanilla hurt overlay texture when the wanted colour changes. */
	public void tick(Minecraft mc) {
		// The alpha of the texture is how much of the normal entity colour is kept.
		int want = enabled ? Math.round((1 - (float) strength.get()) * 255) << 24 | color.value & 0xFFFFFF : VANILLA;
		if (want == applied) return;
		applied = want;
		DynamicTexture texture = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).newgen$getTexture();
		NativeImage pixels = texture.getPixels();
		if (pixels == null) return;
		for (int v = 0; v < 8; v++) {
			for (int u = 0; u < 16; u++) pixels.setPixel(u, v, want);
		}
		texture.upload();
	}
}
