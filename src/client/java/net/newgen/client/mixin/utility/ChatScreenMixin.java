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
	@org.spongepowered.asm.mixin.Shadow protected net.minecraft.client.gui.components.EditBox input;
	/** While searching: what was typed before Ctrl+F, put back when the search ends. */
	@org.spongepowered.asm.mixin.Unique private String newgen$draft;

	/** Enter while searching only ends the search; nothing is sent. */
	@Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
	private void newgen$searchEnter(String typed, boolean addToHistory, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if (ChatMod.query() == null) return;
		ChatMod.search(null, (ChatMod.Lines) Minecraft.getInstance().gui.getChat());
		ci.cancel();
	}

	@ModifyVariable(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true)
	private String newgen$outgoing(String typed) {
		return ChatMod.outgoing(typed);
	}

	/** Ctrl+F starts or ends searching the chat; the input field then holds the search text. */
	@Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
	private void newgen$searchKey(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (event.key() != GLFW.GLFW_KEY_F || !event.hasControlDown() || !ChatMod.searchable()) return;
		ChatMod.Lines chat = (ChatMod.Lines) Minecraft.getInstance().gui.getChat();
		if (ChatMod.query() == null) {
			newgen$draft = input.getValue();
			input.setValue("");
			ChatMod.search("", chat);
		} else {
			ChatMod.search(null, chat);
			input.setValue(newgen$draft == null ? "" : newgen$draft);
		}
		cir.setReturnValue(true);
	}

	/** Closing the chat ends a search, so the full history is back next time. */
	@Inject(method = "removed()V", at = @At("HEAD"))
	private void newgen$endSearch(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if (ChatMod.query() != null) ChatMod.search(null, (ChatMod.Lines) Minecraft.getInstance().gui.getChat());
	}

	/** While searching: the list follows what is typed, and a line above the input says so. */
	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
	private void newgen$searchFollow(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if (ChatMod.query() == null) return;
		ChatMod.search(input.getValue(), (ChatMod.Lines) Minecraft.getInstance().gui.getChat());
		g.drawString(Minecraft.getInstance().font, Component.translatable("newgen_client.chat.searching"), 4, g.guiHeight() - 26, 0xFFFFD25A, true);
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
