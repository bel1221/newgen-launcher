package net.newgen.client.mixin.controls;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.newgen.client.mods.controls.ToggleSneakMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toggle sneak/sprint: holds the sneak or sprint input on while toggled. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void newgen$toggleSneak(CallbackInfo ci) {
		if (!ToggleSneakMod.sneak && !ToggleSneakMod.sprint) return;
		Input k = keyPresses;
		keyPresses = new Input(k.forward(), k.backward(), k.left(), k.right(), k.jump(),
				k.shift() || ToggleSneakMod.sneak, k.sprint() || ToggleSneakMod.sprint);
	}
}
