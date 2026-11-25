package net.newgen.client.mixin.visual;

import net.minecraft.client.Minecraft;
import net.newgen.client.mods.visual.GuiScaleMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** GUI Scale: swaps the scale the window is asked for. Vanilla still caps it to what fits the window. */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@ModifyArg(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;calculateScale(IZ)I"), index = 0)
	private int newgen$guiScale(int scale) {
		return GuiScaleMod.I.enabled ? (int) GuiScaleMod.I.scale.get() : scale;
	}
}
