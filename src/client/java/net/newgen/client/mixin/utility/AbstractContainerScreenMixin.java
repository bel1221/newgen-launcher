package net.newgen.client.mixin.utility;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import net.newgen.client.mods.utility.InventoryGuard;
import net.newgen.client.mods.utility.InventoryTweaksMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Wave 3 utility 1
/** Container screens: lock/bind keys over a slot, slot overlays (padlock, link, enchant labels), click outside to close. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected Slot hoveredSlot;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;

	@Shadow
	protected abstract boolean hasClickedOutside(double x, double y, int left, int top);

	@Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
	private void newgen$slotKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
		if (self.getFocused() instanceof EditBox) return; // typing in a search or name field
		if (InventoryGuard.onKey(hoveredSlot, event.key())) cir.setReturnValue(true);
	}

	@Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;II)V", at = @At("TAIL"))
	private void newgen$slotOverlay(GuiGraphics g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		InventoryGuard.drawSlot(g, slot);
	}

	@Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasClickedOutside(DDII)Z"),
			cancellable = true)
	private void newgen$clickOutside(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
		if (!InventoryTweaksMod.clickOutside() || !self.getMenu().getCarried().isEmpty()) return;
		if (hasClickedOutside(event.x(), event.y(), leftPos, topPos)) {
			self.onClose();
			cir.setReturnValue(true);
		}
	}
}
