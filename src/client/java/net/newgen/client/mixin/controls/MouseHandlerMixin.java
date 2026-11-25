package net.newgen.client.mixin.controls;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.newgen.client.mods.controls.FreelookMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Freelook: the mouse turns the camera instead of the player. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void newgen$freelook(LocalPlayer player, double dx, double dy) {
		if (FreelookMod.active) FreelookMod.turn(dx, dy);
		else player.turn(dx, dy);
	}
}
