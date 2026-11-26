package net.newgen.client.mixin.utility;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.newgen.client.mods.utility.UnfocusedLimitsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Unfocused Limits: a lower frame limit while the window is in the background. */
@Mixin(FramerateLimitTracker.class)
public class FramerateLimitMixin {
	@ModifyReturnValue(method = "getFramerateLimit()I", at = @At("RETURN"))
	private int newgen$unfocusedFps(int original) {
		return UnfocusedLimitsMod.fps(original);
	}
}
