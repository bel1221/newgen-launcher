package net.newgen.client.mods.utility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/**
 * Plays a sound when a player or mob you hit recently dies. Your hits are remembered on this computer only; a death is
 * seen from the death event the server sends (or the entity's health reaching 0). Hook: LivingEntityKillSoundMixin.
 */
public class KillSoundsMod extends Mod {
	public static KillSoundsMod I;

	private final Setting.Choice sound = choice("kill_sound", "Sound", "Experience",
			"Experience", "Level up", "Pling", "Bell", "Arrow hit", "Critical hit");
	private final Setting.Num volume = num("kill_volume", "Kill sound volume (%)", 100, 0, 100, 5);
	private final Setting.Num pitch = num("kill_pitch", "Pitch", 1.0, 0.5, 2.0, 0.1);
	private final Setting.Bool players = bool("kill_players", "On player kills", true);
	private final Setting.Bool mobs = bool("kill_mobs", "On mob kills", true);
	private final Setting.Num window = num("kill_window", "Count a kill up to (seconds after your hit)", 10, 1, 60, 1);

	/** Entity id to the time of your last hit on it (ms). */
	private final Map<Integer, Long> hits = new HashMap<>();
	private ClientLevel level;

	public KillSoundsMod() {
		super("kill_sounds", "Kill Sounds", "A sound when a player or mob you hit dies");
		I = this;
		action("kill_test", "Test sound", "newgen_client.kill_sounds.test", this::play);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (enabled && world.isClientSide() && entity instanceof LivingEntity) hits.put(entity.getId(), System.currentTimeMillis());
			return InteractionResult.PASS;
		});
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		if (mc.level != level) {
			hits.clear();
			level = mc.level;
		}
		if (!enabled || level == null || hits.isEmpty()) return;
		long now = System.currentTimeMillis();
		for (Iterator<Map.Entry<Integer, Long>> it = hits.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, Long> e = it.next();
			if (!recent(e.getValue(), now, (long) (window.get() * 1000))) {
				it.remove();
				continue;
			}
			// Fallback for servers that never send the death event: the synced health reached 0.
			if (level.getEntity(e.getKey()) instanceof LivingEntity living && living.isDeadOrDying()) {
				it.remove();
				killed(living);
			}
		}
	}

	/** Whether a hit at hitTime still counts at now. */
	static boolean recent(long hitTime, long now, long windowMs) {
		return now - hitTime <= windowMs && now >= hitTime;
	}

	/** Called when the client learns an entity died (entity event 3). */
	public static void onDeath(LivingEntity entity) {
		KillSoundsMod m = I;
		if (m == null || !m.enabled) return;
		Long at = m.hits.remove(entity.getId());
		if (at != null && recent(at, System.currentTimeMillis(), (long) (m.window.get() * 1000))) m.killed(entity);
	}

	private void killed(Entity entity) {
		boolean isPlayer = entity instanceof Player;
		if (isPlayer ? players.value : mobs.value) play();
	}

	private void play() {
		SoundEvent event = switch (sound.value) {
			case "Level up" -> SoundEvents.PLAYER_LEVELUP;
			case "Pling" -> SoundEvents.NOTE_BLOCK_PLING.value();
			case "Bell" -> SoundEvents.BELL_BLOCK;
			case "Arrow hit" -> SoundEvents.ARROW_HIT_PLAYER;
			case "Critical hit" -> SoundEvents.PLAYER_ATTACK_CRIT;
			default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
		};
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, (float) pitch.get(), (float) volume.get() / 100f));
	}
}
