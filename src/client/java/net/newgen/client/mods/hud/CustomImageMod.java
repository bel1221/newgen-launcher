package net.newgen.client.mods.hud;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Custom Image: a PNG file from the computer on the HUD, sized and faded to taste. */
public class CustomImageMod extends HudMod {
	static final long MAX_BYTES = 8L * 1024 * 1024;
	static final int MAX_SIDE = 2048;
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("newgen_client", "hud/custom_image");

	private final Setting.FilePath file = file("image_file", "Image", "", "*.png");
	private final Setting.Num maxSize = num("image_size", "Image size", 64, 8, 512, 4);
	private final Setting.Num opacity = num("opacity", "Opacity", 1, 0.1, 1, 0.05);

	private String loadedPath;
	private int imageW, imageH;
	private boolean loaded;
	private String error;

	public CustomImageMod() {
		super("custom_image", "Custom Image", "A PNG picture of your choice on the screen", 0.5f, 0.3f);
		background.value = false;
	}

	/** True when the bytes start with the PNG signature. */
	static boolean isPng(byte[] head) {
		byte[] sig = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
		if (head.length < sig.length) return false;
		for (int i = 0; i < sig.length; i++) if (head[i] != sig[i]) return false;
		return true;
	}

	/** The drawn size: the longest side becomes max, keeping the shape. */
	static int[] fit(int w, int h, int max) {
		if (w <= 0 || h <= 0) return new int[] {0, 0};
		double f = (double) max / Math.max(w, h);
		return new int[] {Math.max(1, (int) Math.round(w * f)), Math.max(1, (int) Math.round(h * f))};
	}

	/** Loads the file when the path changed (render thread). */
	private void refresh(Minecraft mc) {
		String path = file.value.trim();
		if (path.equals(loadedPath)) return;
		loadedPath = path;
		if (loaded) {
			mc.getTextureManager().release(TEXTURE);
			loaded = false;
		}
		error = null;
		if (path.isEmpty()) return;
		try {
			Path p = Path.of(path);
			if (!Files.isRegularFile(p)) throw new IllegalArgumentException("newgen_client.custom_image.missing");
			if (Files.size(p) > MAX_BYTES) throw new IllegalArgumentException("newgen_client.custom_image.too_big");
			byte[] bytes = Files.readAllBytes(p);
			if (!isPng(bytes)) throw new IllegalArgumentException("newgen_client.custom_image.not_png");
			NativeImage image = NativeImage.read(bytes);
			if (image.getWidth() > MAX_SIDE || image.getHeight() > MAX_SIDE) {
				image.close();
				throw new IllegalArgumentException("newgen_client.custom_image.too_big");
			}
			imageW = image.getWidth();
			imageH = image.getHeight();
			mc.getTextureManager().register(TEXTURE, new DynamicTexture(() -> "newgen custom image", image));
			loaded = true;
		} catch (IllegalArgumentException e) {
			error = e.getMessage();
		} catch (Exception e) {
			error = "newgen_client.custom_image.unreadable";
		}
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		refresh(mc);
		return error != null && editing ? List.of(I18n.get(error)) : List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		refresh(mc);
		return loaded ? fit(imageW, imageH, (int) maxSize.get()) : null;
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		if (!loaded) return;
		int[] s = fit(imageW, imageH, (int) maxSize.get());
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0f, 0f, s[0], s[1], imageW, imageH, imageW, imageH, ARGB.white((float) opacity.get()));
	}
}
