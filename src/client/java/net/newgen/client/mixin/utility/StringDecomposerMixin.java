package net.newgen.client.mixin.utility;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.newgen.client.mods.utility.NickHiderMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public class StringDecomposerMixin {
	/** All styled text the game draws passes through here as plain strings. */
	@ModifyVariable(
			method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
			at = @At("HEAD"), argsOnly = true)
	private static String newgen$nickHider(String text, String sameText, int start, Style style, Style plain, FormattedCharSink sink) {
		// A start offset points into the original string, so only whole strings are rewritten.
		return start == 0 ? NickHiderMod.apply(text) : text;
	}
}
