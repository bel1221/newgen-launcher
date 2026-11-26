package net.newgen.client.mods.visual;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/**
 * "Legacy animations": a curated set of Animatium's player-animation options (Animatium by Mixces and lowercasebtw,
 * bundled unmodified as its own jar; see CREDITS.md). Only options that change how the player and the held item move
 * are offered here; anything touching hit detection, eye height, client movement or other visuals is left alone.
 * <p>
 * No Animatium code is used: its public config object (AnimatiumConfig.instance(), one field per option) is read and
 * written by reflection, and its own save() persists it. Each tick our values are copied over when they differ, so a
 * change applies at once. With this card off, every option here is off (vanilla). On the first run with Animatium
 * present, all of Animatium's switches (not just ours) are turned off and its enums set to vanilla, once.
 * Without Animatium installed this card is not registered.
 */
public class AnimatiumMod extends Mod {
	private static final String CONFIG = "org.visuals.legacy.animatium.config.AnimatiumConfig";

	/** One Animatium option: its category field, its own field, and our setting. */
	private record Link(String category, String field, Setting setting, String[] enumNames) {
	}

	private final List<Link> links = new ArrayList<>();
	private final Setting.Hidden resetDone = new Setting.Hidden("defaults_off", "");
	private Object config;            // AnimatiumConfig.instance()
	private boolean broken;           // Animatium's config could not be reached: do nothing more

	public AnimatiumMod() {
		super("animatium", "Legacy Animations", "1.7 / 1.8 player and item animations (Animatium)");
		// Held item and hand
		bool("items", "itemPositions", "item_positions", "1.7 item positions (eating, drinking, blocking)");
		bool("items", "itemPositionsInThirdPerson", "item_positions_third", "1.7 item positions in third person");
		bool("items", "thinBlockPositions", "thin_blocks", "1.7 held thin-block positions");
		bool("items", "itemUsageSwinging", "use_swing", "Swing while using items (block-hitting)");
		bool("extras", "alwaysUsageSwing", "always_use_swing", "Swing while using any item");
		bool("extras", "offhandUsageSwinging", "offhand_use_swing", "Swing the offhand while using it");
		bool("extras", "legacySwingAnimation", "legacy_swing", "1.7 arm swing");
		choice("items", "equipAnimationVersion", "equip", "Item switch animation",
				new String[]{"Vanilla", "1.7", "1.8", "Off"}, new String[]{"VANILLA", "V1_7", "V1_8", "DISABLED"});
		choice("items", "fishingRodVersion", "rod", "Fishing rod pose",
				new String[]{"Vanilla", "1.7", "1.8", "1.13"}, new String[]{"VANILLA", "V1_7", "V1_8", "V1_13"});
		// Sword blocking
		bool("other", "thirdPersonSwordBlockingPosition", "block_third", "1.7 sword blocking in third person");
		bool("other", "lockBlockingArmRotation", "block_arm_lock", "Lock the arm while blocking");
		bool("movement", "uncapBlockingHeadRotation", "block_head", "Free head turn while blocking");
		bool("other", "heldItemArmLogic", "held_arm", "1.7 arm pose when holding items");
		// Body and movement
		choice("movement", "sneakAnimation", "sneak", "Sneak animation",
				new String[]{"Vanilla", "1.7", "1.8", "1.13"}, new String[]{"VANILLA", "V1_7", "V1_8", "V1_13"});
		bool("movement", "longUnsneak", "long_unsneak", "Slow stand-up after sneaking");
		choice("screen", "cameraVersion", "camera", "Sneak camera",
				new String[]{"Vanilla", "1.8", "1.9-1.13", "1.14"}, new String[]{"VANILLA", "V1_8", "V1_9_TO_V1_13_2", "V1_14_TO_V1_14_3"});
		bool("movement", "rotateBackwardsWalking", "backwards", "Turn the body when walking backwards");
		bool("movement", "handViewBobbingMovement", "hand_bob", "Hand bobs with walking");
		bool("movement", "bowArmMovement", "bow_arms", "1.7 arms when drawing a bow");
		bool("movement", "deathLimbs", "death_limbs", "1.7 limbs when dying");
		bool("movement", "oldCapeMovement", "cape", "1.7 cape movement");
		bool("movement", "capeSneakPosition", "cape_sneak", "Cape follows sneaking");
		settings.add(resetDone);
	}

	public static boolean available() {
		return FabricLoader.getInstance().isModLoaded("animatium");
	}

	private void bool(String category, String field, String id, String name) {
		links.add(new Link(category, field, bool(id, name, false), null));
	}

	private void choice(String category, String field, String id, String name, String[] labels, String[] enumNames) {
		links.add(new Link(category, field, choice(id, name, labels[0], labels), enumNames));
	}

	/** Copies our values into Animatium when they differ; saves Animatium's config when something changed. */
	public void tick() {
		if (broken) return;
		try {
			if (config == null) config = Class.forName(CONFIG).getMethod("instance").invoke(null);
			boolean changed = false;
			if (resetDone.value.isEmpty()) {
				changed = allOff();
				resetDone.value = "yes";
				net.newgen.client.core.Mods.save();
			}
			for (Link link : links) changed |= apply(link);
			if (changed) {
				Object handler = Class.forName(CONFIG).getField("HANDLER").get(null);
				handler.getClass().getMethod("save").invoke(handler);
			}
		} catch (Throwable e) {
			broken = true;
			org.slf4j.LoggerFactory.getLogger("newgen_client").warn("Animatium settings could not be applied", e);
		}
	}

	private boolean apply(Link link) throws ReflectiveOperationException {
		Object category = config.getClass().getField(link.category).get(config);
		Field f = category.getClass().getField(link.field);
		Object want;
		if (link.setting instanceof Setting.Bool b) {
			want = enabled && b.value;
		} else {
			Setting.Choice c = (Setting.Choice) link.setting;
			int at = java.util.Arrays.asList(c.options).indexOf(c.value);
			String name = link.enumNames[enabled && at >= 0 ? at : 0];
			want = enumConstant(f.getType(), name);
		}
		if (want == null || want.equals(f.get(category))) return false;
		f.set(category, want);
		return true;
	}

	/** Every boolean in every Animatium category off and every enum to VANILLA (or OFF): vanilla Minecraft. */
	private boolean allOff() throws ReflectiveOperationException {
		boolean changed = false;
		for (Field cf : config.getClass().getFields()) {
			if (java.lang.reflect.Modifier.isStatic(cf.getModifiers())) continue;
			Object category = cf.get(config);
			if (category == null) continue;
			for (Field f : category.getClass().getFields()) {
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
				Object off = f.getType() == boolean.class ? Boolean.FALSE
						: f.getType().isEnum() ? enumConstant(f.getType(), "VANILLA", "OFF") : null;
				if (off != null && !off.equals(f.get(category))) {
					f.set(category, off);
					changed = true;
				}
			}
		}
		return changed;
	}

	private static Object enumConstant(Class<?> type, String... names) {
		for (String name : names) {
			for (Object c : type.getEnumConstants()) if (((Enum<?>) c).name().equals(name)) return c;
		}
		return null;
	}
}
