package net.newgen.client.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** QA7-2: Try again may only ever rejoin a remote multiplayer server. */
class ErrorCardTest {
	@Test
	void onlyRemoteMultiplayerServersAreRetryable() {
		assertTrue(ErrorCard.retryable("newgenmc.net:25565", false, false));
		assertFalse(ErrorCard.retryable("newgenmc.net:25565", false, true)); // the integrated (single-player) server
		assertFalse(ErrorCard.retryable("192.168.1.5:51000", true, false)); // a LAN game
		assertFalse(ErrorCard.retryable("", false, false));
		assertFalse(ErrorCard.retryable(null, false, false));
	}
}
