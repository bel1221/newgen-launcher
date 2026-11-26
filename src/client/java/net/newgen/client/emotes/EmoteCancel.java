package net.newgen.client.emotes;

/**
 * What ends an emote (or keeps one from starting), as a pure decision on what the player is doing this tick.
 * Walking, sprinting, jumping and strafing are deliberately not inputs: the emote keeps playing while moving.
 * Death, a dimension change, a disconnect and pressing the wheel key again are handled by EmotesMod itself.
 */
public final class EmoteCancel {
	/**
	 * This tick's state. attack: attack key held (punching air, a block or an entity, or breaking a block).
	 * use: use key held (items, blocks, entities, placing, eating, bows, shields). hurt: took damage or its knockback.
	 * sneak, crawlOrSwim (the swimming pose, in or out of water), gliding, sleeping, riding: pose changes.
	 * containerOpen: an inventory or container screen is open.
	 */
	public record Signals(boolean attack, boolean use, boolean hurt, boolean sneak, boolean crawlOrSwim, boolean gliding,
			boolean sleeping, boolean riding, boolean dead, boolean containerOpen) {
	}

	public enum Reason {
		ATTACK, USE, HURT, POSE, DEATH, SCREEN
	}

	private EmoteCancel() {
	}

	/** The reason to cancel, or null to keep playing. */
	public static Reason reason(Signals s) {
		if (s.dead()) return Reason.DEATH;
		if (s.hurt()) return Reason.HURT;
		if (s.attack()) return Reason.ATTACK;
		if (s.use()) return Reason.USE;
		if (s.sneak() || s.crawlOrSwim() || s.gliding() || s.sleeping() || s.riding()) return Reason.POSE;
		if (s.containerOpen()) return Reason.SCREEN;
		return null;
	}
}
