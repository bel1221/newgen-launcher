package net.newgen.client.mixin.utility;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.newgen.client.mods.controls.HotbarTweaksMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Wave 3 utility 1
/** Hotbar Tweaks: the slot the mouse wheel moves to. */
@Mixin(MouseHandler.class)
public class HotbarScrollMixin {
	@WrapOperation(method = "onScroll(JDD)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"))
	private int newgen$hotbarScroll(double scroll, int current, int size, Operation<Integer> original) {
		int next = HotbarTweaksMod.next(scroll, current, size);
		return next >= 0 ? next : original.call(scroll, current, size);
	}
}
