package net.newgen.client.emotes;

import java.io.IOException;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.network.AnimationBinary;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Reads the animation out of an Emotecraft ".emotecraft" file with Player Animation Library's decoder (MIT);
 * no Emotecraft code is used.
 * <p>
 * The file is a container: an int version, a purpose byte and a section count, then per section an id byte,
 * a version byte, an int size and the data. Section 0x00 is the legacy animation binary (after an int),
 * 0x99 the newer one. Our import tool (tools/EmoteImport.java) has already moved the icon and sound out.
 */
public final class EmoteFile {
	private EmoteFile() {
	}

	public static Animation read(byte[] bytes) throws IOException {
		ByteBuf buf = Unpooled.wrappedBuffer(bytes);
		buf.readInt();
		buf.readByte();
		int count = buf.readUnsignedByte();
		Animation animation = null;
		for (int i = 0; i < count; i++) {
			int id = buf.readByte() & 0xFF, version = buf.readByte(), size = buf.readInt(), pos = buf.readerIndex();
			if (size < 0 || size > buf.readableBytes()) throw new IOException("emote file section " + id + " claims " + size + " bytes, " + buf.readableBytes() + " left");
			ByteBuf section = buf.slice(pos, size);
			if (id == 0x00) {
				section.readInt();
				animation = LegacyAnimationBinary.read(section, version);
			} else if (id == 0x99) {
				animation = AnimationBinary.read(section, version);
			}
			buf.readerIndex(pos + size);
		}
		if (animation == null) throw new IOException("no animation in emote file");
		return withoutBends(animation);
	}

	/**
	 * Removes the elbow, knee and waist bends. Player Animation Library decodes them but does not draw a bent limb
	 * itself (that takes a separate renderer we do not ship), while it still moves the head and arms as if the torso
	 * bent ("apply bend to other bones"). The result was a torso and legs drawn straight with the head and arms
	 * placed for a bend that never showed: the head came off the body and squats sank between rigid legs. Without
	 * bends every part stays joined.
	 */
	static Animation withoutBends(Animation animation) {
		animation.data().put(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, false);
		for (BoneAnimation bone : animation.boneAnimations().values()) bone.bendKeyFrames().clear();
		animation.boneAnimations().values().removeIf(bone -> !bone.hasKeyframes());
		return animation;
	}
}
