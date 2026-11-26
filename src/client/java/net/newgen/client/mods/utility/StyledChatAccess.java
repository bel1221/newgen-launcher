package net.newgen.client.mods.utility;

import java.util.function.Consumer;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

/**
 * The chat's drawing with Chat's background colour, rounded panel and scrollbar setting. One instance is reused:
 * {@link #wrap} points it at this frame's vanilla access, so nothing is allocated per frame.
 */
public final class StyledChatAccess implements ChatComponent.ChatGraphicsAccess {
	private ChatComponent.ChatGraphicsAccess target;
	/** The panel draws straight to the vanilla access (made once, not per frame). */
	private final RoundedChatBackground.Fill direct = (x1, y1, x2, y2, color) -> target.fill(x1, y1, x2, y2, color);
	private final RoundedChatBackground panel = new RoundedChatBackground(direct, 0);
	private boolean rounded;

	/** This frame's wrapper around the vanilla access, with the chosen corner radius (0 = square). */
	public StyledChatAccess wrap(ChatComponent.ChatGraphicsAccess access, int radius) {
		target = access;
		rounded = radius > 0;
		panel.reset(direct, radius);
		return this;
	}

	/** The rounded panel of this frame, or null when the corners are square. */
	public RoundedChatBackground panel() {
		return rounded ? panel : null;
	}

	/** Background strips go to the panel (or get the chosen colour); the scrollbar is kept or dropped. */
	@Override
	public void fill(int x1, int y1, int x2, int y2, int color) {
		if ((color & 0xFFFFFF) == 0) {
			if (rounded) panel.add(x1, y1, x2, y2, ChatMod.background(color >>> 24));
			else target.fill(x1, y1, x2, y2, ChatMod.background(color >>> 24));
		} else if (!ChatMod.hideScrollbar()) target.fill(x1, y1, x2, y2, color);
	}

	@Override
	public void updatePose(Consumer<Matrix3x2f> consumer) {
		target.updatePose(consumer);
	}

	@Override
	public boolean handleMessage(int y, float alpha, FormattedCharSequence text) {
		if (rounded) panel.flush();
		return target.handleMessage(y, alpha, text);
	}

	@Override
	public void handleTag(int x1, int y1, int x2, int y2, float alpha, GuiMessageTag tag) {
		target.handleTag(x1, y1, x2, y2, alpha, tag);
	}

	@Override
	public void handleTagIcon(int x, int y, boolean hovered, GuiMessageTag tag, GuiMessageTag.Icon icon) {
		target.handleTagIcon(x, y, hovered, tag, icon);
	}
}
