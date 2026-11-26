package net.newgen.client.mixin.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.newgen.client.mods.utility.ChatMod;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chat mod: shortcuts and :emoji: on what is sent; right-click a message to copy it, Shift + right-click to delete it. */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	@ModifyVariable(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true)
	private String newgen$outgoing(String typed) {
		return ChatMod.outgoing(typed);
	}

	@Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
	private void newgen$copyOrDelete(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !ChatMod.copyAndDelete()) return;
		Minecraft mc = Minecraft.getInstance();
		ChatMod.Lines chat = (ChatMod.Lines) mc.gui.getChat();
		GuiMessage message = chat.newgen$messageAt(event.y());
		if (message == null) return;
		boolean shift = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
		if (shift) {
			chat.newgen$delete(message);
		} else {
			mc.keyboardHandler.setClipboard(message.content().getString());
			mc.gui.setOverlayMessage(Component.translatable("newgen_client.chat.copied"), false);
		}
		cir.setReturnValue(true);
	}
}
