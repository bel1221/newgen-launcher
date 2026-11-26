package net.newgen.client.mixin.utility;

import java.util.List;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.newgen.client.mods.utility.ChatMod;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The Chat mod's hooks: history limit, message processing, size, visibility, background, offset, copy and delete. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatMod.Lines {
	@Shadow @Final private List<GuiMessage> allMessages;
	@Shadow @Final private List<GuiMessage.Line> trimmedMessages;
	@Shadow private int chatScrollbarPos;
	@Shadow @Final Minecraft minecraft;

	@Shadow
	private void refreshTrimmedMessages() {
		throw new AssertionError();
	}

	@Shadow
	private double getScale() {
		throw new AssertionError();
	}

	@Shadow public abstract int getLinesPerPage();

	@Shadow public abstract boolean isChatFocused();

	/** Vanilla drops messages past 100 lines; the Chat mod raises that limit (or removes it). */
	@ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
	private int newgen$historyLimit(int vanilla) {
		return ChatMod.historyLimit(vanilla);
	}

	@ModifyVariable(
			method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("HEAD"), argsOnly = true)
	private Component newgen$process(Component message) {
		return ChatMod.process(message, this);
	}

	@Override
	public void newgen$removeNewest() {
		if (allMessages.isEmpty()) return;
		allMessages.removeFirst();
		refreshTrimmedMessages();
	}

	@ModifyReturnValue(method = "getWidth()I", at = @At("RETURN"))
	private int newgen$width(int vanilla) {
		return ChatMod.width(vanilla);
	}

	@ModifyReturnValue(method = "getHeight()I", at = @At("RETURN"))
	private int newgen$height(int vanilla) {
		return ChatMod.height(vanilla, isChatFocused());
	}

	@ModifyReturnValue(method = "isChatHidden()Z", at = @At("RETURN"))
	private boolean newgen$hidden(boolean vanilla) {
		return vanilla || ChatMod.hidden();
	}

	@ModifyReturnValue(method = "isChatFocused()Z", at = @At("RETURN"))
	private boolean newgen$peek(boolean vanilla) {
		return vanilla || ChatMod.peeking();
	}

	/** Moves the chat up (and slides it in for a moment after a new message), for drawing and clicking alike. */
	@ModifyVariable(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int newgen$offset(int screenHeight) {
		return screenHeight - ChatMod.verticalOffset();
	}

	/** Background colour and the scrollbar: vanilla draws the background black (with its opacity) and the bar in colour. */
	@ModifyVariable(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V", at = @At("HEAD"), argsOnly = true)
	private ChatComponent.ChatGraphicsAccess newgen$styled(ChatComponent.ChatGraphicsAccess access) {
		if (!ChatMod.restyles()) return access;
		return new ChatComponent.ChatGraphicsAccess() {
			@Override
			public void updatePose(Consumer<Matrix3x2f> consumer) {
				access.updatePose(consumer);
			}

			@Override
			public void fill(int x1, int y1, int x2, int y2, int color) {
				if ((color & 0xFFFFFF) == 0) access.fill(x1, y1, x2, y2, ChatMod.background(color >>> 24));
				else if (!ChatMod.hideScrollbar()) access.fill(x1, y1, x2, y2, color);
			}

			@Override
			public boolean handleMessage(int y, float alpha, FormattedCharSequence text) {
				return access.handleMessage(y, alpha, text);
			}

			@Override
			public void handleTag(int x1, int y1, int x2, int y2, float alpha, GuiMessageTag tag) {
				access.handleTag(x1, y1, x2, y2, alpha, tag);
			}

			@Override
			public void handleTagIcon(int x, int y, boolean hovered, GuiMessageTag tag, GuiMessageTag.Icon icon) {
				access.handleTagIcon(x, y, hovered, tag, icon);
			}
		};
	}

	/** The message whose line is at screen height y in the open chat, or null (same layout as vanilla's render). */
	@Override
	public GuiMessage newgen$messageAt(double y) {
		double scale = getScale();
		double spacing = minecraft.options.chatLineSpacing().get();
		int lineHeight = (int) (9 * (spacing + 1.0));
		int bottom = (int) Math.floor((minecraft.getWindow().getGuiScaledHeight() - ChatMod.verticalOffset() - 40) / scale);
		double fromBottom = bottom - y / scale;
		if (fromBottom < 0) return null;
		int k = (int) (fromBottom / lineHeight);
		int index = k + chatScrollbarPos;
		if (k >= getLinesPerPage() || index >= trimmedMessages.size()) return null;
		GuiMessage.Line line = trimmedMessages.get(index);
		for (GuiMessage message : allMessages) if (message.addedTime() == line.addedTime()) return message;
		return null;
	}

	@Override
	public void newgen$refresh() {
		refreshTrimmedMessages();
	}

	/** Chat search: while searching only the matching messages are listed. */
	@com.llamalad7.mixinextras.injector.WrapWithCondition(method = "refreshTrimmedMessages()V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V"))
	private boolean newgen$searchFilter(ChatComponent chat, GuiMessage message) {
		return ChatMod.shows(message);
	}

	@Override
	public void newgen$delete(GuiMessage message) {
		if (allMessages.remove(message)) refreshTrimmedMessages();
	}
}
