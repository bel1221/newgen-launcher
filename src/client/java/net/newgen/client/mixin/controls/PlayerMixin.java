package net.newgen.client.mixin.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.newgen.client.mods.controls.ToggleSneakMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Toggle Sneak/Sprint fly boost: multiplies the own player's flying speed (the mod only allows it in creative or singleplayer). */
@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
	private void newgen$flyBoost(CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		if (ToggleSneakMod.flyBoost != 1f && self.getAbilities().flying && self == Minecraft.getInstance().player) {
			cir.setReturnValue(cir.getReturnValue() * ToggleSneakMod.flyBoost);
		}
	}
}
