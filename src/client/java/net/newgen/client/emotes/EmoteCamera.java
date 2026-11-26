package net.newgen.client.emotes;

import net.minecraft.client.CameraType;

/**
 * Which view an emote switches to: only from first person (you would not see yourself), to the Camera setting's
 * third-person view. A player already in third person (behind or front) keeps their view, and it is not touched
 * on stop either. null = leave the camera alone.
 */
public final class EmoteCamera {
	private EmoteCamera() {
	}

	public static CameraType viewFor(CameraType current, String setting) {
		if (current != CameraType.FIRST_PERSON || setting.equals("Don't change")) return null;
		return setting.equals("Front") ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
	}
}
