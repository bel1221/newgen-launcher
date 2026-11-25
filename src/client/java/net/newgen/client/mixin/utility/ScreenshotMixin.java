package net.newgen.client.mixin.utility;

import java.util.function.Consumer;

import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.newgen.client.mods.utility.ScreenshotMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Screenshot.class)
public class ScreenshotMixin {
	@ModifyVariable(
			method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
			at = @At("HEAD"), argsOnly = true)
	private static Consumer<Component> newgen$wrapCallback(Consumer<Component> callback) {
		return ScreenshotMod.wrap(callback);
	}
}
