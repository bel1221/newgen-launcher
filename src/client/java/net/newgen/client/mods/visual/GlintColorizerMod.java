package net.newgen.client.mods.visual;

import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/**
 * Glint Colorizer: the enchantment shimmer in any colour. The game's glint textures are repainted in place on the GPU
 * (their brightness kept, the hue replaced), so every item and armour glint follows; turning it off writes the
 * original pixels back.
 */
public class GlintColorizerMod extends Mod {
	public static final GlintColorizerMod I = new GlintColorizerMod();
	private static final Identifier[] GLINTS = {
			Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png"),
			Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png")};
	private final Setting.Color color = color("color", "Glint colour", 0xFFFF5555);
	private final Setting.Num strength = num("strength", "Brightness (%)", 100, 20, 200, 10);
	private int applied;
	private boolean painted;

	private GlintColorizerMod() {
		super("glint_colorizer", "Glint Colorizer", "Changes the colour of the enchantment glint on items and armour");
	}

	/** Each tick: repaint when the colour changed (chroma moves it), or restore once when turned off. */
	public void tick(Minecraft mc) {
		int want = color.current() * 31 + (int) strength.get();
		if (enabled ? want == applied && painted : !painted) return;
		for (Identifier id : GLINTS) paint(mc, id, enabled);
		painted = enabled;
		applied = want;
	}

	private void paint(Minecraft mc, Identifier id, boolean tint) {
		AbstractTexture texture = mc.getTextureManager().getTexture(id);
		if (texture == null || texture.getTexture() == null) return;
		try (InputStream in = mc.getResourceManager().open(id); NativeImage image = NativeImage.read(in)) {
			if (tint) {
				int c = color.current();
				float r = (c >> 16 & 0xFF) / 255f, g = (c >> 8 & 0xFF) / 255f, b = (c & 0xFF) / 255f, k = (float) (strength.get() / 100);
				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < image.getWidth(); x++) {
						int p = image.getPixel(x, y);
						// The brightness of the original shimmer, in the chosen colour.
						float l = Math.min(1f, ((p >> 16 & 0xFF) * 0.3f + (p >> 8 & 0xFF) * 0.59f + (p & 0xFF) * 0.11f) / 255f * 1.6f * k);
						image.setPixel(x, y, p & 0xFF000000 | Math.round(Math.min(1, r * l) * 255) << 16
								| Math.round(Math.min(1, g * l) * 255) << 8 | Math.round(Math.min(1, b * l) * 255));
					}
				}
			}
			if (image.getWidth() == texture.getTexture().getWidth(0) && image.getHeight() == texture.getTexture().getHeight(0))
				RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture.getTexture(), image);
		} catch (Exception e) {
			// a resource pack without the texture, or an unexpected format: the glint stays as it is
		}
	}
}
