package net.newgen.client.mods.utility;

import java.io.File;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Adds clickable actions under the "Saved screenshot" chat line. The hook is in ScreenshotMixin. */
public class ScreenshotMod extends Mod {
	public static ScreenshotMod INSTANCE;

	private final Setting.Bool actions = bool("actions", "Show actions in chat", true);
	private final Setting.Bool autoCopy = bool("auto_copy", "Copy file path automatically", false);

	public ScreenshotMod() {
		super("screenshot", "Screenshot", "Open folder / copy path buttons after a screenshot");
		INSTANCE = this;
	}

	/** Wraps the callback that reports the result of a screenshot. May run on a worker thread. */
	public static Consumer<Component> wrap(Consumer<Component> vanilla) {
		return message -> {
			vanilla.accept(message);
			ScreenshotMod m = INSTANCE;
			if (m == null || !m.enabled) return;
			File file = savedFile(message);
			if (file == null) return; // the screenshot failed; vanilla already said so
			String path = file.getAbsolutePath();
			if (m.actions.value) {
				vanilla.accept(Component.empty()
						.append(button("[Open folder]", "Open the screenshots folder", new ClickEvent.OpenFile(file.getAbsoluteFile().getParentFile())))
						.append(" ")
						.append(button("[Copy path]", "Copy the file path", new ClickEvent.CopyToClipboard(path))));
			}
			if (m.autoCopy.value) {
				Minecraft mc = Minecraft.getInstance();
				mc.execute(() -> mc.keyboardHandler.setClipboard(path));
			}
		};
	}

	private static MutableComponent button(String label, String hint, ClickEvent click) {
		return Component.literal(label).withStyle(s -> s.withColor(ChatFormatting.AQUA)
				.withClickEvent(click).withHoverEvent(new HoverEvent.ShowText(Component.literal(hint))));
	}

	/** The success message carries the file as a click event on its file-name argument. */
	private static File savedFile(Component message) {
		if (!(message.getContents() instanceof TranslatableContents contents)) return null;
		for (Object arg : contents.getArgs()) {
			if (arg instanceof Component c && c.getStyle().getClickEvent() instanceof ClickEvent.OpenFile open) return open.file();
		}
		return null;
	}
}
