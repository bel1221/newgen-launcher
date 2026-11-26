package net.newgen.client.mods.utility;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/** The Screenshot Manager: file list on the left (newest first), the selected picture large on the right, actions below. */
class ScreenshotManagerScreen extends Screen {
	private static final int ROW = 12, LIST_W = 170, TOP = 30, MAX_SIDE = 1280;
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("newgen_client", "screenshot_manager");

	private final ScreenshotManagerMod mod;
	private final Screen parent;
	private List<File> files = new ArrayList<>();
	private File selected;
	private boolean onlyFavourites;
	private int scroll;
	/** Size of the loaded preview texture; 0 while none. */
	private int texW, texH;
	/** Bumped on every load, so a slow load for an older selection is dropped. */
	private int loadToken;
	private boolean loading, failed;
	private Button deleteButton, copyButton, favButton;

	ScreenshotManagerScreen(ScreenshotManagerMod mod, Screen parent) {
		super(Component.translatable("newgen_client.screenshot_manager.title"));
		this.mod = mod;
		this.parent = parent;
	}

	@Override
	protected void init() {
		refresh();
		int by = height - 26, w = 74, gap = 4;
		int x = width / 2 - (w * 5 + gap * 4) / 2;
		addRenderableWidget(Button.builder(Component.translatable("newgen_client.screenshot_manager.folder"), b -> {
			File dir = ScreenshotManagerMod.folder();
			dir.mkdirs();
			Util.getPlatform().openFile(dir);
		}).bounds(x, by, w, 20).build());
		deleteButton = addRenderableWidget(Button.builder(Component.translatable("newgen_client.screenshot_manager.delete"), b -> askDelete())
				.bounds(x + (w + gap), by, w, 20).build());
		copyButton = addRenderableWidget(Button.builder(Component.translatable("newgen_client.screenshot_manager.copy"), b -> {
			if (selected != null) minecraft.keyboardHandler.setClipboard(selected.getAbsolutePath());
		}).bounds(x + 2 * (w + gap), by, w, 20).build());
		favButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			if (selected != null) mod.toggleFavourite(selected.getName());
			updateButtons();
			if (onlyFavourites) refresh();
		}).bounds(x + 3 * (w + gap), by, w, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x + 4 * (w + gap), by, w, 20).build());
		addRenderableWidget(Button.builder(Component.empty(), b -> {
			onlyFavourites = !onlyFavourites;
			scroll = 0;
			refresh();
			b.setMessage(filterLabel());
		}).bounds(8, 6, LIST_W, 18).build()).setMessage(filterLabel());
		updateButtons();
	}

	private Component filterLabel() {
		return Component.translatable(onlyFavourites ? "newgen_client.screenshot_manager.only_favourites" : "newgen_client.screenshot_manager.all");
	}

	private void refresh() {
		List<File> all = ScreenshotManagerMod.list();
		if (onlyFavourites) all.removeIf(f -> !mod.isFavourite(f.getName()));
		files = all;
		File want = selected != null && files.contains(selected) ? selected : files.isEmpty() ? null : files.get(0);
		if (want != selected || texW == 0 && !loading) select(want);
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
	}

	private void updateButtons() {
		boolean has = selected != null;
		if (deleteButton != null) deleteButton.active = has;
		if (copyButton != null) copyButton.active = has;
		if (favButton != null) {
			favButton.active = has;
			favButton.setMessage(Component.translatable(has && mod.isFavourite(selected.getName())
					? "newgen_client.screenshot_manager.unfavourite" : "newgen_client.screenshot_manager.favourite"));
		}
	}

	private void select(File file) {
		selected = file;
		updateButtons();
		releaseTexture();
		failed = false;
		loading = false;
		if (file == null) return;
		int token = ++loadToken;
		loading = true;
		CompletableFuture.supplyAsync(() -> read(file)).thenAccept(image -> Minecraft.getInstance().execute(() -> {
			if (token != loadToken || minecraft.screen != this) {
				if (image != null) image.close();
				return;
			}
			loading = false;
			if (image == null) {
				failed = true;
				return;
			}
			texW = image.getWidth();
			texH = image.getHeight();
			minecraft.getTextureManager().register(TEXTURE, new DynamicTexture(() -> "newgen screenshot manager", image));
		}));
	}

	/** The picture, shrunk to at most MAX_SIDE; null when it cannot be read. Off the game thread. */
	private static NativeImage read(File file) {
		try (InputStream in = Files.newInputStream(file.toPath()); NativeImage full = NativeImage.read(in)) {
			int[] size = ScreenshotManagerMod.fit(full.getWidth(), full.getHeight(), Math.min(MAX_SIDE, full.getWidth()), Math.min(MAX_SIDE, full.getHeight()));
			NativeImage small = new NativeImage(full.format(), size[0], size[1], false);
			full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), small);
			return small;
		} catch (Exception | OutOfMemoryError e) {
			return null;
		}
	}

	private void releaseTexture() {
		if (texW > 0) minecraft.getTextureManager().release(TEXTURE);
		texW = texH = 0;
	}

	private void askDelete() {
		File file = selected;
		if (file == null) return;
		minecraft.setScreen(new ConfirmScreen(yes -> {
			if (yes) {
				try {
					Files.deleteIfExists(file.toPath());
					mod.forget(file.getName());
				} catch (Exception ignored) {
				}
				int index = files.indexOf(file);
				files.remove(file);
				selected = files.isEmpty() ? null : files.get(Math.min(Math.max(0, index), files.size() - 1));
			}
			minecraft.setScreen(this); // init runs again: the list is read again and the preview reloaded
		}, Component.translatable("newgen_client.screenshot_manager.delete_title"),
				Component.translatable("newgen_client.screenshot_manager.delete_message", file.getName())));
	}

	private int listBottom() {
		return height - 34;
	}

	private int visibleRows() {
		return Math.max(1, (listBottom() - TOP) / ROW);
	}

	private int maxScroll() {
		return Math.max(0, files.size() - visibleRows());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (mx >= 8 && mx < 8 + LIST_W && my >= TOP && my < listBottom()) {
			int i = scroll + (int) ((my - TOP) / ROW);
			if (i >= 0 && i < files.size()) {
				if (files.get(i) != selected) select(files.get(i));
				else if (doubleClick) Util.getPlatform().openFile(selected);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(v) * 3));
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		int right = width - 8, previewX = 8 + LIST_W + 10;
		g.drawCenteredString(font, title, (previewX + right) / 2, 11, 0xFFFFFFFF);

		// List
		g.fill(8, TOP - 2, 8 + LIST_W, listBottom() + 2, 0x80000000);
		if (files.isEmpty()) {
			g.drawCenteredString(font, Component.translatable("newgen_client.screenshot_manager.empty"), 8 + LIST_W / 2, TOP + 6, 0xFFAAAAAA);
		}
		int rows = visibleRows();
		for (int r = 0; r < rows && scroll + r < files.size(); r++) {
			File f = files.get(scroll + r);
			int y = TOP + r * ROW;
			boolean hover = mouseX >= 8 && mouseX < 8 + LIST_W && mouseY >= y && mouseY < y + ROW;
			if (f.equals(selected)) g.fill(8, y, 8 + LIST_W, y + ROW, 0x80FFFFFF);
			else if (hover) g.fill(8, y, 8 + LIST_W, y + ROW, 0x40FFFFFF);
			boolean fav = mod.isFavourite(f.getName());
			String name = font.plainSubstrByWidth(f.getName(), LIST_W - (fav ? 16 : 6));
			g.drawString(font, name, 11, y + 2, 0xFFFFFFFF, false);
			if (fav) g.drawString(font, "★", 8 + LIST_W - 10, y + 2, 0xFFFFD75E, false);
		}
		if (maxScroll() > 0) {
			int track = listBottom() - TOP, barH = Math.max(10, track * rows / files.size());
			int barY = TOP + (track - barH) * scroll / maxScroll();
			g.fill(8 + LIST_W - 2, barY, 8 + LIST_W, barY + barH, 0xC0FFFFFF);
		}

		// Preview
		int areaW = right - previewX, areaH = listBottom() - TOP - 12;
		g.fill(previewX, TOP - 2, right, listBottom() + 2, 0x80000000);
		if (texW > 0) {
			int[] s = ScreenshotManagerMod.fit(texW, texH, areaW, areaH);
			int px = previewX + (areaW - s[0]) / 2, py = TOP + (areaH - s[1]) / 2;
			g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, px, py, 0f, 0f, s[0], s[1], texW, texH, texW, texH);
		} else if (selected != null) {
			String key = failed ? "newgen_client.screenshot_manager.failed" : "newgen_client.screenshot_manager.loading";
			g.drawCenteredString(font, Component.translatable(key), previewX + areaW / 2, TOP + areaH / 2, 0xFFAAAAAA);
		}
		if (selected != null) {
			String info = selected.getName() + "  ·  " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(selected.lastModified()))
					+ "  ·  " + (selected.length() / 1024) + " KB";
			g.drawCenteredString(font, font.plainSubstrByWidth(info, areaW - 6), previewX + areaW / 2, listBottom() - 9, 0xFFCCCCCC);
		}
	}

	@Override
	public void removed() {
		loadToken++;
		loading = false;
		releaseTexture();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
