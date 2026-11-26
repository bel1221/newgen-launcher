package net.newgen.client.emotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.client.CameraType;
import org.junit.jupiter.api.Test;

/** The emote camera rule: first person goes to third person; third person is left as it is. */
class EmoteCameraTest {
	@Test
	void firstPersonGoesBehind() {
		assertEquals(CameraType.THIRD_PERSON_BACK, EmoteCamera.viewFor(CameraType.FIRST_PERSON, "Back"));
		assertEquals(CameraType.THIRD_PERSON_FRONT, EmoteCamera.viewFor(CameraType.FIRST_PERSON, "Front"));
	}

	@Test
	void thirdPersonIsLeftAlone() {
		assertNull(EmoteCamera.viewFor(CameraType.THIRD_PERSON_BACK, "Back"));
		assertNull(EmoteCamera.viewFor(CameraType.THIRD_PERSON_FRONT, "Back"));
		assertNull(EmoteCamera.viewFor(CameraType.THIRD_PERSON_BACK, "Front"));
	}

	@Test
	void dontChangeNeverChanges() {
		assertNull(EmoteCamera.viewFor(CameraType.FIRST_PERSON, "Don't change"));
	}
}
