package net.newgen.client.emotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.HumanoidAnimationController;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import org.junit.jupiter.api.Test;

/** Loads every bundled emote and plays it through PAL's controller tick by tick, as the game would. */
class EmoteFileTest {
	private static final Path DIR = Path.of("src/main/resources/assets/newgen_client/emotes");
	private static final String[] BONES = {"body", "torso", "head", "right_arm", "left_arm", "right_leg", "left_leg", "right_item", "left_item"};

	@Test
	void everyBundledEmotePlays() throws Exception {
		List<Path> files;
		try (var list = Files.list(DIR)) {
			files = list.filter(p -> p.toString().endsWith(".emotecraft")).sorted().toList();
		}
		assertEquals(14, files.size(), "bundled emotes");
		for (Path file : files) {
			String id = file.getFileName().toString().replace(".emotecraft", "");
			Animation animation = EmoteFile.read(Files.readAllBytes(file));
			assertTrue(animation.length() > 0, id + " has a length");
			assertTrue(Files.exists(DIR.resolve(id + ".json")), id + " has its sidecar");
			// Icons are optional (Get the Griddy has none); the menus draw a plain glyph instead.

			HumanoidAnimationController c = new HumanoidAnimationController((ctl, data, set) -> PlayState.CONTINUE, MolangLoader::createNewEngine);
			c.triggerAnimation(animation);
			int ticks = (int) Math.min(animation.length() + 20, 20 * 120), active = 0;
			float moved = 0;
			for (int t = 0; t <= ticks; t++) {
				AnimationData d = new AnimationData(0, 0, false);
				c.tick(d);
				c.setupAnim(d);
				if (c.isActive()) active++;
				for (String bone : BONES) {
					PlayerAnimBone b = c.get3DTransform(new PlayerAnimBone(bone));
					for (float v : new float[]{b.rotX, b.rotY, b.rotZ, b.positionX, b.positionY, b.positionZ})
						assertFalse(Float.isNaN(v) || Float.isInfinite(v), id + " " + bone + " at tick " + t);
					moved = Math.max(moved, Math.abs(b.rotX) + Math.abs(b.rotY) + Math.abs(b.rotZ));
				}
			}
			assertTrue(active > 0, id + " plays");
			assertTrue(moved > 0.01f, id + " moves a bone");
		}
	}
}
