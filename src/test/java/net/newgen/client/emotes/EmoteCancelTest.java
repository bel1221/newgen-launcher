package net.newgen.client.emotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.newgen.client.emotes.EmoteCancel.Reason;
import net.newgen.client.emotes.EmoteCancel.Signals;
import org.junit.jupiter.api.Test;

/** The emote cancel rules: moving keeps the emote; attacking, using, getting hit and pose changes end it. */
class EmoteCancelTest {
	private static Signals only(int which) {
		boolean[] b = new boolean[10];
		if (which >= 0) b[which] = true;
		return new Signals(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9]);
	}

	@Test
	void nothingHappeningKeepsPlaying() {
		// Movement (walk, sprint, jump, strafe) is not an input at all, so it can never cancel.
		assertNull(EmoteCancel.reason(only(-1)));
	}

	@Test
	void eachTriggerCancelsWithItsReason() {
		assertEquals(Reason.ATTACK, EmoteCancel.reason(only(0)));
		assertEquals(Reason.USE, EmoteCancel.reason(only(1)));
		assertEquals(Reason.HURT, EmoteCancel.reason(only(2)));
		for (int pose = 3; pose <= 7; pose++) assertEquals(Reason.POSE, EmoteCancel.reason(only(pose)), "pose signal " + pose);
		assertEquals(Reason.DEATH, EmoteCancel.reason(only(8)));
		assertEquals(Reason.SCREEN, EmoteCancel.reason(only(9)));
	}

	@Test
	void theWheelClickStillHeldDoesNotCancel() {
		assertNull(EmoteCancel.reason(only(0), false), "attack held from the wheel click");
		assertNull(EmoteCancel.reason(only(1), false), "use held from the wheel click");
		assertEquals(Reason.HURT, EmoteCancel.reason(only(2), false), "other triggers still count");
		assertEquals(Reason.ATTACK, EmoteCancel.reason(only(0), true), "after a release, attack cancels again");
	}

	@Test
	void deathWinsOverEverythingElse() {
		assertEquals(Reason.DEATH, EmoteCancel.reason(new Signals(true, true, true, true, true, true, true, true, true, true)));
	}
}
