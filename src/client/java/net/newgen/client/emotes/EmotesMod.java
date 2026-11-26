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
 * The camera goes to third person (behind, by default) and back to the player's own view afterwards. Walking,
 * sprinting, jumping and strafing keep the emote going; standing still, the drawn body holds its yaw while the mouse
 * turns the real one, so the camera can orbit the emote. It stops when it ends, when the wheel key is pressed again,
 * or on attack, use, damage, sneaking, crawling or swimming, gliding, sleeping, riding, death, an inventory screen,
 * a dimension change or a disconnect (see EmoteCancel).
 */
public class EmotesMod extends Mod {
	/** The animation layer on the player model. */
	public static final Identifier LAYER = Identifier.fromNamespaceAndPath(NewgenClient.ID, "emote");
	public static final int SLOTS = 8;
	/** G: B is Waypoints' "add waypoint" key, and Simple Voice Chat uses V, M, N, H and Caps Lock. */
	private final Setting.Key key = key("key", "Wheel key", GLFW.GLFW_KEY_G);
	/** Third person back by default (one F5 press); a new id, so configs saved with the old "Front" default move over. */
	private final Setting.Choice camera = choice("camera_view", "Camera", "Back", "Back", "Front", "Don't change");
	private final Setting.Bool sound = bool("sound", "Emote sound", true);
	private final Setting.Num volume = num("volume", "Emote volume (%)", 100, 0, 100, 5);
	/** The 8 slots as emote ids, comma separated; an empty entry is an empty slot. */
	private final Setting.Hidden wheel = new Setting.Hidden("wheel",
			"hello,hug,blow_kiss,floss,take_the_l,get_the_griddy,macarena,rat_dance_1_2");

	/** The local player's entity id while an emote plays, else -1; the cape, armor and elytra layers skip that player. */
	private static volatile int emotingEntity = -1;

	private EmoteLibrary.Emote playing;
	private int playedTicks;
	private Level level;
	private SoundInstance clip;
	private CameraType before;
	/** The view the emote switched to; the old one comes back only if the player kept this one (no F5 meanwhile). */
	private CameraType setView;
	/** False until attack and use have both been released after the emote started (the wheel click may still be held). */
	private boolean clicksArmed;
	private boolean cameraChanged, wasDown;

	public EmotesMod() {
		super("emotes", "Emotes", "Hold G for the emote wheel; only you see and hear them");
		settings.add(new Setting.Action("edit_wheel", "Wheel", "newgen_client.emotes.edit",
				() -> Minecraft.getInstance().setScreen(new EmoteWheelEditScreen(Minecraft.getInstance().screen, this))));
		settings.add(wheel);
		enabled = true;
		EmoteLibrary.register();
		// Every player model gets the layer; only the local player's is ever triggered.
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000,
				avatar -> new PlayerAnimationController(avatar, (controller, data, setter) -> PlayState.CONTINUE));
	}

	/** Volume for the wheel's sound preview: half the emote volume, 0 when emote sound is off. */
	float previewVolume() {
		return sound.value ? (float) (volume.get() / 200) : 0;
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
		if (playing != null && (mc.player == null || mc.level != level || !enabled)) stop(mc);
		if (!enabled || mc.player == null) {
			wasDown = false;
			return;
		}
		boolean down = key.isDown(mc);
		if (down && !wasDown) {
			if (playing != null) stop(mc);
			else if (!interrupted(mc, true)) mc.setScreen(new EmoteWheelScreen(this));
		}
		wasDown = down;
		if (playing == null) return;
		playedTicks++;
		PlayerAnimationController c = controller(mc.player);
		// The controller picks the animation up on its next tick, so "ended" is only read after a moment.
		if (!clicksArmed && !mc.options.keyAttack.isDown() && !mc.options.keyUse.isDown()) clicksArmed = true;
		if (interrupted(mc, clicksArmed) || c == null || playedTicks > 2 && !c.isActive()) stop(mc);
	}

	public void play(Minecraft mc, EmoteLibrary.Emote emote) {
		if (emote == null || mc.player == null || interrupted(mc, false)) return;
		stop(mc);
		PlayerAnimationController c = controller(mc.player);
		if (c == null) return;
		c.triggerAnimation(emote.animation());
		playing = emote;
		emotingEntity = mc.player.getId();
		// The wheel key may still be held (an emote picked with 1-8 or Enter): that is not a new press, so the next tick
		// must not read it as "press again to stop".
		wasDown = keyHeld(mc);
		playedTicks = 0;
		clicksArmed = !mc.options.keyAttack.isDown() && !mc.options.keyUse.isDown();
		level = mc.level;
		// Only from first person: the view goes to third person and comes back on stop (if still the one we set).
		// A player already in third person keeps their view, before and after.
		CameraType view = FreelookMod.active ? null : EmoteCamera.viewFor(mc.options.getCameraType(), camera.value);
		if (view != null) {
			before = mc.options.getCameraType();
			setView = view;
			mc.options.setCameraType(setView);
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
		emotingEntity = -1;
		locked = false;
		blending = false;
		if (mc.player != null) {
			PlayerAnimationController c = controller(mc.player);
			if (c != null) c.stop();
		}
		if (clip != null) mc.getSoundManager().stop(clip);
		clip = null;
		if (cameraChanged && mc.options.getCameraType() == setView) mc.options.setCameraType(before);
		cameraChanged = false;
	}

	/** Whether this render state is the local player playing an emote: its cape, armor and elytra are not drawn. */
	public static boolean hidesGear(net.minecraft.client.renderer.entity.state.EntityRenderState state) {
		return emotingEntity != -1 && state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar && avatar.id == emotingEntity;
	}

	private static PlayerAnimationController controller(LocalPlayer player) {
		return PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER) instanceof PlayerAnimationController c ? c : null;
	}

	/** Anything that cancels an emote, or keeps one from starting (see EmoteCancel; moving does not). */
	private static boolean interrupted(Minecraft mc, boolean clicksArmed) {
		LocalPlayer p = mc.player;
		Options o = mc.options;
		return EmoteCancel.reason(new EmoteCancel.Signals(o.keyAttack.isDown(), o.keyUse.isDown(), p.hurtTime > 0,
				o.keyShift.isDown() || p.isShiftKeyDown(), p.getPose() == net.minecraft.world.entity.Pose.SWIMMING || p.isSwimming(),
				p.isFallFlying(), p.isSleeping(), p.isPassenger(), p.isDeadOrDying(), mc.screen instanceof AbstractContainerScreen<?>), clicksArmed) != null;
	}

	// ---- body yaw while standing still (render side only; the real rotation is never touched) ----

	private static final long REALIGN_NANOS = 150_000_000L;
	private static boolean locked, blending;
	private static float lockedYaw, blendFrom;
	private static long blendStart;

	/** Whether the local player is emoting. */
	public static boolean emoting(net.minecraft.world.entity.Entity entity) {
		return emotingEntity != -1 && entity.getId() == emotingEntity;
	}

	/** True while the rendered body is held still (standing): the head then follows the emote, not the mouse. */
	public static boolean bodyLocked() {
		return locked;
	}

	/**
	 * The body yaw to draw for the emoting local player, or NaN for the normal one. Standing still: held at the yaw it
	 * had when the player stopped (or the emote started), while the mouse keeps turning the real yaw, so the camera
	 * orbits the emote. Moving again: back to the real body yaw over 150 ms.
	 */
	public static float renderBodyYaw(LocalPlayer p, float realBodyYaw) {
		Options o = Minecraft.getInstance().options;
		boolean moving = o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown()
				|| p.getDeltaMovement().horizontalDistance() > 0.01;
		float blended = Float.NaN;
		if (blending) {
			float t = (System.nanoTime() - blendStart) / (float) REALIGN_NANOS;
			if (t >= 1) blending = false;
			else blended = net.minecraft.util.Mth.rotLerp(t * t * (3 - 2 * t), blendFrom, realBodyYaw);
		}
		if (!moving) {
			if (!locked) {
				locked = true;
				blending = false;
				lockedYaw = Float.isNaN(blended) ? realBodyYaw : blended;
			}
			return lockedYaw;
		}
		if (locked) {
			locked = false;
			blending = true;
			blendFrom = lockedYaw;
			blendStart = System.nanoTime();
			return lockedYaw;
		}
		return blended;
	}
}
