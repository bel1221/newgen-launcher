package net.newgen.client.mixin.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.newgen.client.mods.visual.ItemCustomizerMod;
import net.newgen.client.mods.visual.ShieldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item Customizer and Shields: moves the first person hand. The push/pop pair keeps the move inside this one hand. */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"))
	private void newgen$moveHand(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		pose.pushPose();
		float x = 0, y = 0, z = 0;
		if (ItemCustomizerMod.I.enabled) {
			x = (float) ItemCustomizerMod.I.x.get();
			y = (float) ItemCustomizerMod.I.y.get();
			z = (float) ItemCustomizerMod.I.z.get();
		}
		if (ShieldsMod.I.enabled && stack.is(Items.SHIELD)) y -= (float) ShieldsMod.I.lower.get();
		boolean right = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
		// "Sideways" moves the item outwards for either hand; in view space -Z is away from the eye.
		pose.translate(right ? x : -x, y, -z);
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void newgen$restore(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		pose.popPose();
	}
}
