package net.newgen.client.emotes;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.newgen.client.NewgenClient;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import net.newgen.client.mods.controls.FreelookMod;
import org.lwjgl.glfw.GLFW;

/**
 * Emotes, local only: hold the wheel key, release on an emote, and your own player plays it (through Player
 * Animation Library) with its sound, if it has one. Nothing is sent to the server and nobody else sees or hears it.
 * <p>
 * The camera goes to third person (front by default) and back afterwards. An emote stops when it ends, when
 * the wheel key is pressed again, or on movement, jump, sneak, attack, use, damage, riding, swimming, gliding,
 * sleeping, death, an inventory screen, a dimension change or a disconnect.
 */
public class EmotesMod extends Mod {
	/** The animation layer on the player model. */
	public static final Identifier LAYER = Identifier.fromNamespaceAndPath(NewgenClient.ID, "emote");
	public static final int SLOTS = 8;
	/** G: B is Waypoints' "add waypoint" key, and Simple Voice Chat uses V, M, N, H and Caps Lock. */
	private final Setting.Key key = key("key", "Wheel key", GLFW.GLFW_KEY_G);
	private final Setting.Choice camera = choice("camera", "Camera", "Front", "Front", "Back", "Don't change");
	private final Setting.Bool sound = bool("sound", "Emote sound", true);
	private final Setting.Num volume = num("volume", "Emote volume (%)", 100, 0, 100, 5);
	/** The 8 slots as emote ids, comma separated; an empty entry is an empty slot. */
	private final Setting.Hidden wheel = new Setting.Hidden("wheel",
			"hello,hug,blow_kiss,floss,take_the_l,get_the_griddy,macarena,rat_dance_1_2");

	private EmoteLibrary.Emote playing;
	private int playedTicks;
	private Level level;
	private SoundInstance clip;
	private CameraType before;
	private boolean cameraChanged, wasDown, preloaded, overlayWas;

	public EmotesMod() {
		super("emotes", "Emotes", "Hold G for the emote wheel; only you see and hear them");
		settings.add(new Setting.Action("edit_wheel", "Wheel", "newgen_client.emotes.edit",
				() -> Minecraft.getInstance().setScreen(new EmoteWheelEditScreen(Minecraft.getInstance().screen, this))));
		settings.add(wheel);
		enabled = true;
		// Every player model gets the layer; only the local player's is ever triggered.
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000,
				avatar -> new PlayerAnimationController(avatar, (controller, data, setter) -> PlayState.CONTINUE));
	}

	// ---- the wheel's slots ----

	public String slot(int i) {
		String[] ids = wheel.value.split(",", -1);
		return i < ids.length ? ids[i] : "";
	}

	public void setSlot(int i, String id) {
		String[] ids = new String[SLOTS];
		for (int k = 0; k < SLOTS; k++) ids[k] = k == i ? id : slot(k);
		wheel.value = String.join(",", ids);
	}

	/** Whether the wheel key (keyboard or mouse) is held, also while a screen is open (the wheel itself). */
	boolean keyHeld(Minecraft mc) {
		if (key.isMouse()) return GLFW.glfwGetMouseButton(mc.getWindow().handle(), key.value - Setting.Key.MOUSE) == GLFW.GLFW_PRESS;
		return key.value > 0 && com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), key.value);
	}

	// ---- playback ----

	public void tick(Minecraft mc) {
		// Emotes (re)load whenever the resources finish loading: at start-up, after F3+T and after a pack change
		// (each shows the loading overlay).
		boolean overlay = mc.getOverlay() != null;
		if (!overlay && (overlayWas || !preloaded)) {
			preloaded = true;
			EmoteLibrary.reload();
		}
		overlayWas = overlay;
		if (playing != null && (mc.player == null || mc.level != level || !enabled)) stop(mc);
		if (!enabled || mc.player == null) {
			wasDown = false;
			return;
		}
		boolean down = key.isDown(mc);
		if (down && !wasDown) {
			if (playing != null) stop(mc);
			else if (!interrupted(mc)) mc.setScreen(new EmoteWheelScreen(this));
		}
		wasDown = down;
		if (playing == null) return;
		playedTicks++;
		PlayerAnimationController c = controller(mc.player);
		// The controller picks the animation up on its next tick, so "ended" is only read after a moment.
		if (interrupted(mc) || c == null || playedTicks > 2 && !c.isActive()) stop(mc);
	}

	public void play(Minecraft mc, EmoteLibrary.Emote emote) {
		if (emote == null || mc.player == null || interrupted(mc)) return;
		stop(mc);
		PlayerAnimationController c = controller(mc.player);
		if (c == null) return;
		c.triggerAnimation(emote.animation());
		playing = emote;
		playedTicks = 0;
		level = mc.level;
		if (!FreelookMod.active && !camera.value.equals("Don't change")) {
			before = mc.options.getCameraType();
			mc.options.setCameraType(camera.value.equals("Back") ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT);
			cameraChanged = true;
		}
		if (sound.value && emote.sound() != null && volume.get() > 0) {
			// Not positional, category Players (the vanilla "Players" slider applies too), only on this client.
			clip = new SimpleSoundInstance(emote.sound(), SoundSource.PLAYERS, (float) (volume.get() / 100), 1f,
					SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
			mc.getSoundManager().play(clip);
		}
	}

	public void stop(Minecraft mc) {
		if (playing == null) return;
		playing = null;
		if (mc.player != null) {
			PlayerAnimationController c = controller(mc.player);
			if (c != null) c.stop();
		}
		if (clip != null) mc.getSoundManager().stop(clip);
		clip = null;
		if (cameraChanged) mc.options.setCameraType(before);
		cameraChanged = false;
	}

	private static PlayerAnimationController controller(LocalPlayer player) {
		return PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER) instanceof PlayerAnimationController c ? c : null;
	}

	/** Anything that cancels an emote, or keeps one from starting. */
	private static boolean interrupted(Minecraft mc) {
		LocalPlayer p = mc.player;
		Options o = mc.options;
		return o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown()
				|| o.keyShift.isDown() || o.keyAttack.isDown() || o.keyUse.isDown()
				|| p.hurtTime > 0 || p.isPassenger() || p.isSwimming() || p.isFallFlying() || p.isSleeping() || p.isDeadOrDying()
				|| mc.screen instanceof AbstractContainerScreen<?>;
	}
}
