package net.newgen.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.junit.jupiter.api.Test;

/** The title-screen clips decode with JCodec the way VideoBackground reads them (a re-encode must keep that). */
class TitleClipTest {
	private static final Path DIR = Path.of("src/main/resources/assets/newgen_client/backgrounds");

	@Test
	void everyClipDecodesAtFullSize() throws Exception {
		List<Path> clips;
		try (Stream<Path> files = Files.list(DIR)) {
			clips = files.filter(p -> p.toString().endsWith(".mp4")).sorted().toList();
		}
		assertTrue(clips.size() >= 8, "clips found: " + clips.size());
		for (Path clip : clips) {
			FrameGrab grab = FrameGrab.createFrameGrab(ByteBufferSeekableByteChannel.readFromByteBuffer(ByteBuffer.wrap(Files.readAllBytes(clip))));
			// Two seconds: the first key frame and the predicted frames after it.
			for (int i = 0; i < 48; i++) {
				Picture p = grab.getNativeFrame();
				assertNotNull(p, clip + " frame " + i);
				assertEquals(1920, p.getWidth(), clip.toString());
				// 540 rows, coded as 34 macroblocks (544) and cropped on display
				assertEquals(540, p.getCrop() == null ? p.getHeight() : p.getCrop().getHeight(), clip.toString());
			}
		}
	}
}
