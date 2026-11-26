package net.newgen.client.emotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.HumanoidAnimationController;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plays an emote and checks the pose against the numbers written in the file itself: the arm's pitch and bend at
 * each of its keyframe ticks. The file is walked here byte by byte (legacy v2 layout: per bone and axis an "enabled"
 * byte, a count, then tick / value / easing records), independently of the library's decoder.
 */
class EmoteKeyframeTest {
	private static final Path DIR = Path.of("src/main/resources/assets/newgen_client/emotes");

	/** bone → axis lists in file order: x, y, z, pitch, yaw, roll, (bend axis, bend) ; each list = {tick, value} pairs. */
	static Map<String, List<List<float[]>>> rawKeyframes(byte[] bytes) {
		ByteBuffer b = ByteBuffer.wrap(bytes);
		b.getInt();
		b.get();
		int sections = b.get() & 0xFF;
		for (int s = 0; s < sections; s++) {
			int id = b.get() & 0xFF, version = b.get(), size = b.getInt(), start = b.position();
			if (id != 0x00) {
				b.position(start + size);
				continue;
			}
			b.getInt();
			b.getInt(); // begin
			b.getInt(); // end
			b.getInt(); // stop
			b.get(); // looped
			b.getInt(); // return tick
			b.get(); // ease before
			b.get(); // nsfw
			int keyframeSize = b.get();
			if (version < 2) throw new IllegalStateException("fixture is not v2");
			int bones = b.getInt();
			Map<String, List<List<float[]>>> out = new LinkedHashMap<>();
			for (int i = 0; i < bones; i++) {
				byte[] name = new byte[b.getInt()];
				b.get(name);
				String bone = new String(name, StandardCharsets.UTF_8);
				boolean bends = !bone.contains("Item") && !bone.equals("head");
				int axes = bends ? 8 : 6;
				List<List<float[]>> lists = new ArrayList<>();
				for (int a = 0; a < axes; a++) {
					boolean enabled = b.get() != 0;
					int count = b.getInt();
					List<float[]> frames = new ArrayList<>();
					for (int k = 0; k < count; k++) {
						int at = b.position();
						frames.add(new float[] {b.getInt(), b.getFloat()});
						b.position(at + keyframeSize);
					}
					lists.add(enabled ? frames : List.of());
				}
				out.put(bone, lists);
			}
			return out;
		}
		throw new IllegalStateException("no legacy section");
	}

	/** Plays an emote tick by tick; the first tick already advances it to tick 1, so step s shows tick s + 1. */
	private static void play(byte[] bytes, int lastTick, java.util.function.BiConsumer<Integer, HumanoidAnimationController> check) throws Exception {
		HumanoidAnimationController c = new HumanoidAnimationController((ctl, data, set) -> PlayState.CONTINUE, MolangLoader::createNewEngine);
		c.triggerAnimation(EmoteFile.read(bytes));
		for (int step = 0; step < lastTick; step++) {
			AnimationData d = new AnimationData(0, 0, false);
			c.tick(d);
			c.setupAnim(d);
			check.accept(step + 1, c);
		}
	}

	@SafeVarargs
	private static int lastTick(List<float[]>... lists) {
		int last = 0;
		for (List<float[]> list : lists) for (float[] f : list) last = Math.max(last, (int) f[0]);
		return last;
	}

	/** The arm's pitch and elbow bend equal the file's numbers at every keyframe (no torso bend in these two). */
	@ParameterizedTest
	@ValueSource(strings = {"hello", "floss"})
	void armPitchAndBendMatchTheFile(String id) throws Exception {
		byte[] bytes = Files.readAllBytes(DIR.resolve(id + ".emotecraft"));
		Map<String, List<List<float[]>>> raw = rawKeyframes(bytes);
		List<float[]> pitch = raw.get("rightArm").get(3), bend = raw.get("rightArm").get(7);
		assertFalse(pitch.isEmpty() && bend.isEmpty(), id + ": the right arm moves");
		play(bytes, lastTick(pitch, bend), (t, c) -> {
			PlayerAnimBone arm = c.get3DTransform(new PlayerAnimBone("right_arm"));
			for (float[] f : pitch) if ((int) f[0] == t) assertEquals(f[1], arm.rotX, 1e-3, id + " right arm pitch at tick " + t);
			for (float[] f : bend) if ((int) f[0] == t) assertEquals(f[1], arm.bend, 1e-3, id + " right arm bend at tick " + t);
		});
	}

	/**
	 * A torso bend (Gangnam Style): the file's body bend reaches the torso, and the animation asks for the head and
	 * arms to follow it, which Bendable Cuboids draws, so they stay attached to the bent torso.
	 */
	@Test
	void torsoBendMatchesTheFileAndCarriesTheHeadAndArms() throws Exception {
		byte[] bytes = Files.readAllBytes(DIR.resolve("gangnam_style.emotecraft"));
		List<float[]> bend = rawKeyframes(bytes).get("body").get(7);
		assertFalse(bend.isEmpty(), "Gangnam Style bends its torso");
		assertEquals(Boolean.TRUE, EmoteFile.read(bytes).data().get(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY).orElse(null),
				"head and arms follow the torso bend");
		play(bytes, lastTick(bend), (t, c) -> {
			for (float[] f : bend) if ((int) f[0] == t) assertEquals(f[1], c.get3DTransform(new PlayerAnimBone("torso")).bend, 1e-3, "torso bend at tick " + t);
		});
	}
}
