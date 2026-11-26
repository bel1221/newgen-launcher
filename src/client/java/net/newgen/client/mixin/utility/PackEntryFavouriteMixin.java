package net.newgen.client.mixin.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.newgen.client.mods.utility.PackOrganizerMod;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pack Organizer: a star on favourite packs; right-click a pack to make it a favourite or not. */
@Mixin(TransferableSelectionList.PackEntry.class)
public class PackEntryFavouriteMixin {
	@Inject(method = "renderContent(Lnet/minecraft/client/gui/GuiGraphics;IIZF)V", at = @At("TAIL"))
	private void newgen$star(GuiGraphics g, int mouseX, int mouseY, boolean hovered, float delta, CallbackInfo ci) {
		if (!PackOrganizerMod.active()) return;
		TransferableSelectionList.PackEntry self = (TransferableSelectionList.PackEntry) (Object) this;
		if (!PackOrganizerMod.I.isFavourite(self.getPackId())) return;
		g.drawString(Minecraft.getInstance().font, "★", self.getContentRight() - 8, self.getContentY() + 1, 0xFFFFD75E, true);
	}

	@Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
	private void newgen$rightClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (!PackOrganizerMod.active() || event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
		TransferableSelectionList.PackEntry self = (TransferableSelectionList.PackEntry) (Object) this;
		PackOrganizerMod.I.toggleFavourite(self.getPackId());
		PackOrganizerMod.I.dirty = true; // re-sorted on the next screen tick, not while the list handles this click
		cir.setReturnValue(true);
	}
}
