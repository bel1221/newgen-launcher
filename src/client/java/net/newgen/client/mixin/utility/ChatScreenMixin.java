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

	/** Image link preview: the link under the pointer, if it is one, shows its image beside the pointer. */
	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
	private void newgen$imagePreview(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if (!ChatMod.imagePreview()) return;
		Minecraft mc = Minecraft.getInstance();
		var finder = new net.minecraft.client.gui.ActiveTextCollector.ClickableStyleFinder(mc.font, mouseX, mouseY);
		mc.gui.getChat().captureClickableText(finder, mc.getWindow().getGuiScaledHeight(), mc.gui.getGuiTicks(), true);
		net.minecraft.network.chat.Style style = finder.result();
		if (style == null || !(style.getClickEvent() instanceof net.minecraft.network.chat.ClickEvent.OpenUrl open)) return;
		net.newgen.client.mods.utility.ImagePreview.Image image = net.newgen.client.mods.utility.ImagePreview.get(open.uri().toString());
		if (image == null) return;
		int x = Math.min(mouseX + 8, g.guiWidth() - image.width() - 2), y = Math.max(2, mouseY - image.height() - 8);
		g.fill(x - 2, y - 2, x + image.width() + 2, y + image.height() + 2, 0xC0000000);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, image.texture(), x, y, 0, 0, image.width(), image.height(), image.width(), image.height(), image.width(), image.height());
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
