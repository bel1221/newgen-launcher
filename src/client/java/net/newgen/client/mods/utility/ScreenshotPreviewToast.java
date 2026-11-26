package net.newgen.client.mods.utility;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;

/** Toast with a small picture of the screenshot just taken. */
class ScreenshotPreviewToast implements Toast {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("newgen_client", "screenshot_preview");
	private static final int PREVIEW_W = 96, SHOW_MS = 4000;

	private final int imageW, imageH;
	private long shownAt = -1;
	private Toast.Visibility visibility = Toast.Visibility.SHOW;

	private ScreenshotPreviewToast(int imageW, int imageH) {
		this.imageW = imageW;
		this.imageH = imageH;
	}

	/** Reads and shrinks the file on the calling thread, then shows the toast on the game thread. */
	static void show(File file) {
		NativeImage small;
		try (InputStream in = Files.newInputStream(file.toPath()); NativeImage full = NativeImage.read(in)) {
			int h = Math.max(1, Math.min(PREVIEW_W, PREVIEW_W * full.getHeight() / Math.max(1, full.getWidth())));
			small = new NativeImage(full.format(), PREVIEW_W, h, false);
			full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), small);
		} catch (Exception e) {
			LoggerFactory.getLogger("newgen_client").warn("Could not read screenshot {} for the preview", file, e);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			int w = small.getWidth(), h = small.getHeight();
			mc.getTextureManager().register(TEXTURE, new DynamicTexture(() -> "newgen screenshot preview", small));
			mc.getToastManager().addToast(new ScreenshotPreviewToast(w, h));
		});
	}

	@Override
	public Toast.Visibility getWantedVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long time) {
		if (shownAt < 0) shownAt = time;
		visibility = time - shownAt < SHOW_MS ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
	}

	@Override
	public int width() {
		return PREVIEW_W + 8;
	}

	@Override
	public int height() {
		return imageH + 20;
	}

	@Override
	public void render(GuiGraphics g, Font font, long time) {
		g.fill(0, 0, width(), height(), 0xE0101010);
		g.fill(0, 0, width(), 1, 0xFF55AAFF);
		String title = font.plainSubstrByWidth(Component.translatable("newgen_client.screenshot.preview").getString(), width() - 8);
		g.drawString(font, title, 4, 4, 0xFFFFFFFF, false);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 4, 15, 0f, 0f, imageW, imageH, imageW, imageH, imageW, imageH);
	}
}
