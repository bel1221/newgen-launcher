package net.newgen.client.mods.utility;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

/**
 * Chat screenshot: Ctrl + right-click a message to mark it; Ctrl + right-click it again for an image of that message,
 * or another one for the range between them (the part of it on screen). The image is the chat as drawn, cut out of a
 * screenshot, saved to screenshots/ as PNG; its path goes to the clipboard (the game's clipboard holds text only).
 * No AWT is used: the runtime the game ships with may not have it.
 */
public final class ChatScreenshot {
	private static GuiMessage marked;

	private ChatScreenshot() {
	}

	/** A Ctrl + right-click on a message. */
	public static void click(GuiMessage message, ChatMod.Lines chat) {
		Minecraft mc = Minecraft.getInstance();
		List<GuiMessage> messages = chat.newgen$messages();
		if (marked == null || !messages.contains(marked)) {
			marked = message;
			mc.gui.setOverlayMessage(Component.translatable("newgen_client.chat.shot.marked"), false);
			return;
		}
		int a = messages.indexOf(marked), b = messages.indexOf(message);
		marked = null;
		Set<Integer> times = new HashSet<>();
		for (GuiMessage m : messages.subList(Math.min(a, b), Math.max(a, b) + 1)) times.add(m.addedTime());
		int[] rows = chat.newgen$rows(times);
		if (rows == null) {
			mc.gui.setOverlayMessage(Component.translatable("newgen_client.chat.shot.failed"), false);
			return;
		}
		File file = new File(new File(mc.gameDirectory, "screenshots"),
				"chat-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")) + ".png");
		int guiWidth = mc.getWindow().getGuiScaledWidth();
		Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
			try (image) {
				int[] crop = crop(rows, image.getWidth(), image.getHeight(), guiWidth);
				try (NativeImage out = new NativeImage(crop[2], crop[3], false)) {
					image.copyRect(out, crop[0], crop[1], 0, 0, crop[2], crop[3], false, false);
					file.getParentFile().mkdirs();
					out.writeToFile(file);
				}
				mc.execute(() -> {
					mc.keyboardHandler.setClipboard(file.getAbsolutePath());
					mc.gui.setOverlayMessage(Component.translatable("newgen_client.chat.shot.saved", file.getName()), false);
				});
			} catch (Exception e) {
				mc.execute(() -> mc.gui.setOverlayMessage(Component.translatable("newgen_client.chat.shot.failed"), false));
			}
		});
	}

	/**
	 * The rectangle to cut from a screenshot, {x, y, width, height} in its pixels, for chat rows {left, top, right,
	 * bottom} in GUI pixels, clamped to the image.
	 */
	static int[] crop(int[] rows, int imageWidth, int imageHeight, int guiWidth) {
		double s = (double) imageWidth / guiWidth;
		int x = Math.max(0, (int) Math.floor(rows[0] * s)), y = Math.max(0, (int) Math.floor(rows[1] * s));
		int x2 = Math.min(imageWidth, (int) Math.ceil(rows[2] * s)), y2 = Math.min(imageHeight, (int) Math.ceil(rows[3] * s));
		return new int[] {x, y, Math.max(1, x2 - x), Math.max(1, y2 - y)};
	}
}
