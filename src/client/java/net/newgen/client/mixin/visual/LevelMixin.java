package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.newgen.client.mods.visual.TimeChangerMod;
import net.newgen.client.mods.visual.WeatherChangerMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Time Changer and Weather Changer. Only the client's level is touched, never the singleplayer server's. */
@Mixin(Level.class)
public class LevelMixin {
	@ModifyReturnValue(method = "getDayTime", at = @At("RETURN"))
	private long newgen$time(long original) {
		return TimeChangerMod.I.enabled && (Object) this instanceof ClientLevel ? (long) TimeChangerMod.I.time.get() : original;
	}

	@ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
	private float newgen$rain(float original) {
		return WeatherChangerMod.I.enabled && (Object) this instanceof ClientLevel ? WeatherChangerMod.I.rain() : original;
	}

	@ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
	private float newgen$thunder(float original) {
		return WeatherChangerMod.I.enabled && (Object) this instanceof ClientLevel ? WeatherChangerMod.I.thunder() : original;
	}
}
