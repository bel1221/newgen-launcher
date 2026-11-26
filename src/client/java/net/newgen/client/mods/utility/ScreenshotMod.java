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
	private final Setting.Bool compact = bool("compact", "Compact chat message", false);
	private final Setting.Bool preview = bool("preview", "Show preview toast", true);

	public ScreenshotMod() {
		super("screenshot", "Screenshot", "Open folder / copy path buttons after a screenshot");
		INSTANCE = this;
	}

	/** Wraps the callback that reports the result of a screenshot. May run on a worker thread. */
	public static Consumer<Component> wrap(Consumer<Component> vanilla) {
		return message -> {
			ScreenshotMod m = INSTANCE;
			File file = m != null && m.enabled ? savedFile(message) : null;
			if (file == null) { // mod off, or the screenshot failed: vanilla says so
				vanilla.accept(message);
				return;
			}
			String path = file.getAbsolutePath();
			MutableComponent buttons = Component.empty()
					.append(button("open", new ClickEvent.OpenFile(file.getAbsoluteFile().getParentFile())))
					.append(" ")
					.append(button("copy", new ClickEvent.CopyToClipboard(path)));
			if (m.compact.value) {
				// One short line: "Screenshot: <name>" with the name clickable, then the buttons.
				MutableComponent line = Component.translatable("newgen_client.screenshot.compact", Component.literal(file.getName())
						.withStyle(s -> s.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(file))));
				if (m.actions.value) line.append(" ").append(buttons);
				vanilla.accept(line);
			} else {
				vanilla.accept(message);
				if (m.actions.value) vanilla.accept(buttons);
			}
			if (m.autoCopy.value) {
				Minecraft mc = Minecraft.getInstance();
				mc.execute(() -> mc.keyboardHandler.setClipboard(path));
			}
			if (m.preview.value) ScreenshotPreviewToast.show(file);
		};
	}

	/** A chat button, "[Open folder]" or "[Copy path]" in the game's language, with its hover hint. */
	private static MutableComponent button(String which, ClickEvent click) {
		return Component.translatable("newgen_client.screenshot." + which).withStyle(s -> s.withColor(ChatFormatting.AQUA)
				.withClickEvent(click).withHoverEvent(new HoverEvent.ShowText(Component.translatable("newgen_client.screenshot." + which + ".hint"))));
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
