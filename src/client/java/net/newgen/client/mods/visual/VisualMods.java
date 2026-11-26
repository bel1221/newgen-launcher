package net.newgen.client.mods.visual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.newgen.client.core.Mods;

/** Registers every mod of the "Visual" group. Add one line per mod: Mods.register(CATEGORY, XMod.I); */
public final class VisualMods {
	public static final String CATEGORY = "Visual";

	private VisualMods() {
	}

	public static void register() {
		// Each mod keeps its one instance in an "I" field so the mixins can read its settings.
		Mods.register(CATEGORY, BlockOutlineMod.I);
		Mods.register(CATEGORY, ChunkBordersMod.I);
		Mods.register(CATEGORY, CrosshairMod.I);
		Mods.register(CATEGORY, DamageTintMod.I);
		Mods.register(CATEGORY, FogMod.I);
		Mods.register(CATEGORY, FovMod.I);
		Mods.register(CATEGORY, GuiScaleMod.I);
		Mods.register(CATEGORY, HeightLimitMod.I);
		Mods.register(CATEGORY, HitboxMod.I);
		Mods.register(CATEGORY, HitColorMod.I);
		Mods.register(CATEGORY, HurtCamMod.I);
		Mods.register(CATEGORY, ItemCustomizerMod.I);
		Mods.register(CATEGORY, LightingMod.I);
		Mods.register(CATEGORY, LightOverlayMod.I);
		Mods.register(CATEGORY, MenuBlurMod.I);
		Mods.register(CATEGORY, NametagMod.I);
		Mods.register(CATEGORY, OverlayMod.I);
		Mods.register(CATEGORY, ParticleChangerMod.I);
		Mods.register(CATEGORY, ShieldsMod.I);
		Mods.register(CATEGORY, ShinyPotsMod.I);
		Mods.register(CATEGORY, TimeChangerMod.I);
		Mods.register(CATEGORY, TntCountdownMod.I);
		Mods.register(CATEGORY, WeatherChangerMod.I);
		Mods.register(CATEGORY, ZoomMod.I);

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			ZoomMod.I.tick(mc);
			GuiScaleMod.I.tick(mc);
			HitColorMod.I.tick(mc);
		});
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("newgen_client", "visual"),
				(graphics, delta) -> DamageTintMod.I.render(graphics, Minecraft.getInstance()));
	}

	/** Called by DebugRendererMixin once per frame, at the point where vanilla draws its own debug shapes. */
	public static void emitGizmos(float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Entity camera = mc.getCameraEntity();
		if (mc.level == null || camera == null) return;
		ChunkBordersMod.I.emit(mc, camera);
		HeightLimitMod.I.emit(mc, camera);
		HitboxMod.I.emit(mc, camera, partialTick);
		LightOverlayMod.I.emit(mc, camera);
		TntCountdownMod.I.emit(mc, partialTick);
	}
}
