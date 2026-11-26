package net.newgen.client.mixin.utility;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.mods.controls.HotbarTweaksMod;
import net.newgen.client.mods.utility.EnchantLabelsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Wave 3 utility 1
/** Hotbar: Enchant Labels on its slots, Hotbar Tweaks' sliding selection box. */
@Mixin(Gui.class)
public class GuiHotbarMixin {
	@Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
			at = @At("TAIL"))
	private void newgen$enchantLabels(GuiGraphics g, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed, CallbackInfo ci) {
		EnchantLabelsMod.draw(g, stack, x, y, true);
	}

	/** The second sprite of renderItemHotbar is the selection box. */
	@ModifyArg(method = "renderItemHotbar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
			at = @At(value = "INVOKE", ordinal = 1,
					target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 2)
	private int newgen$smoothSelection(int x) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null ? x : HotbarTweaksMod.selectionX(x, mc.player.getInventory().getSelectedSlot());
	}
}
