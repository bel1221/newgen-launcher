import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/**
 * Build-time import of Emotecraft emote files into the mod's resources. Not part of the mod jar.
 * <pre>
 *   java tools/EmoteImport.java &lt;folder of .emotecraft files&gt; &lt;path to ffmpeg&gt;
 * </pre>
 * Run from the project folder. For each file it writes, under src/main/resources/assets/newgen_client/:
 * <ul>
 *   <li>emotes/&lt;id&gt;.emotecraft: the same container, keeping only the animation and the text header
 *       (the icon and sound sections move to their own files);</li>
 *   <li>emotes/&lt;id&gt;.json: the author from the file header, and the sound id if it has one;</li>
 *   <li>textures/emotes/&lt;id&gt;.png: the icon, scaled to 64x64;</li>
 *   <li>sounds/emotes/&lt;id&gt;.ogg: the Opus sound (section 3, version 3) muxed into Ogg and converted by ffmpeg
 *       to mono 44.1 kHz Ogg Vorbis, loudness-normalised (Minecraft cannot play Opus);</li>
 * </ul>
 * and rewrites sounds.json with an "emote.&lt;id&gt;" entry per sound. Display names go into lang/*.json by hand.
 * Source files are only read.
 */
public class EmoteImport {
	static final Path ASSETS = Path.of("src/main/resources/assets/newgen_client");

	public static void main(String[] args) throws Exception {
		Path in = Path.of(args[0]);
		String ffmpeg = args[1];
		for (String dir : List.of("emotes", "textures/emotes", "sounds/emotes")) Files.createDirectories(ASSETS.resolve(dir));
		TreeMap<String, String> sounds = new TreeMap<>();
		java.util.Set<String> ids = new java.util.HashSet<>();
		List<Path> files = new ArrayList<>();
		try (var list = Files.list(in)) {
			list.filter(p -> p.toString().endsWith(".emotecraft")).sorted().forEach(files::add);
		}
		for (Path file : files) {
			String id = slug(file.getFileName().toString().replace(".emotecraft", ""));
			if (id.isEmpty() || !ids.add(id)) throw new IOException(file.getFileName() + " gives the id \"" + id + "\", which is empty or already taken; rename the file");
			ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(file));
			int version = b.getInt();
			byte purpose = b.get();
			int count = b.get() & 0xFF;
			ByteArrayOutputStream kept = new ByteArrayOutputStream();
			int keptCount = 0;
			String name = "", author = "";
			boolean sound = false;
			for (int i = 0; i < count; i++) {
				int sid = b.get() & 0xFF, ver = b.get(), size = b.getInt(), pos = b.position();
				ByteBuffer sub = b.slice(pos, size);
				switch (sid) {
					case 0x00, 0x99, 0x11 -> {
						if (sid == 0x11) {
							name = string(sub);
							string(sub); // description
							author = string(sub);
							// Some files store the author as a JSON string ("\"Lualt\""), others bare.
							if (author.length() > 1 && author.startsWith("\"") && author.endsWith("\"")) author = author.substring(1, author.length() - 1);
						}
						kept.write(sid);
						kept.write(ver);
						kept.write(ByteBuffer.allocate(4).putInt(size).array());
						kept.write(b.array(), pos, size);
						keptCount++;
					}
					case 0x12 -> {
						byte[] png = new byte[sub.getInt()];
						sub.get(png);
						icon(png, ASSETS.resolve("textures/emotes/" + id + ".png"));
					}
					case 0x03 -> {
						if (ver == 3) {
							Path opus = Files.createTempFile(id, ".opus");
							try {
								Files.write(opus, oggOpus(sub));
								Path ogg = ASSETS.resolve("sounds/emotes/" + id + ".ogg");
								Process p = new ProcessBuilder(ffmpeg, "-v", "error", "-y", "-i", opus.toString(), "-ac", "1", "-ar", "44100",
										"-af", "loudnorm=I=-18:TP=-2", "-c:a", "libvorbis", "-q:a", "4", ogg.toString()).inheritIO().start();
								if (p.waitFor() != 0) throw new IOException("ffmpeg failed for " + id);
							} finally {
								Files.deleteIfExists(opus);
							}
							sounds.put("emote." + id, "newgen_client:emotes/" + id);
							sound = true;
						} else {
							System.out.println(id + ": sound section v" + ver + " is a note-block song, not imported");
						}
					}
					default -> System.out.println(id + ": skipped section 0x" + Integer.toHexString(sid));
				}
				b.position(pos + size);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(ByteBuffer.allocate(4).putInt(version).array());
			out.write(purpose);
			out.write(keptCount);
			kept.writeTo(out);
			Files.write(ASSETS.resolve("emotes/" + id + ".emotecraft"), out.toByteArray());
			String json = "{\n\t\"author\": " + quote(author) + (sound ? ",\n\t\"sound\": \"newgen_client:emote." + id + "\"" : "") + "\n}\n";
			Files.writeString(ASSETS.resolve("emotes/" + id + ".json"), json);
			System.out.printf("%-28s name=%s author=%s sound=%s%n", id, name, author, sound);
		}
		StringBuilder json = new StringBuilder("{\n");
		int i = 0;
		for (var e : sounds.entrySet()) {
			json.append("\t\"").append(e.getKey()).append("\": {\"sounds\": [{\"name\": \"").append(e.getValue()).append("\"}]}")
					.append(++i < sounds.size() ? ",\n" : "\n");
		}
		Files.writeString(ASSETS.resolve("sounds.json"), json.append("}\n").toString());
	}

	/** "Rat Dance 1.2" becomes "rat_dance_1_2"; parts in brackets are dropped ("Floss (Dance)" becomes "floss"). */
	static String slug(String s) {
		return s.replaceAll("[(][^)]*[)]", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
	}

	/** A header string: an int length, then UTF-8 bytes. */
	static String string(ByteBuffer b) {
		byte[] s = new byte[b.getInt()];
		b.get(s);
		return new String(s, StandardCharsets.UTF_8);
	}

	static String quote(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	static void icon(byte[] png, Path out) throws IOException {
		BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
		BufferedImage dst = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		var g = dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		float scale = Math.min(64f / src.getWidth(), 64f / src.getHeight());
		int w = Math.round(src.getWidth() * scale), h = Math.round(src.getHeight() * scale);
		g.drawImage(src, (64 - w) / 2, (64 - h) / 2, w, h, null);
		g.dispose();
		ImageIO.write(dst, "png", out.toFile());
	}

	// ---- Ogg Opus muxing (RFC 7845), one packet per page ----

	/** The sound section: varints preSkip|gain<<16, endTrim, loopStart+1, packet count, then length-prefixed Opus packets. */
	static byte[] oggOpus(ByteBuffer s) throws IOException {
		int skipGain = varInt(s), endTrim = varInt(s);
		varInt(s); // loop start
		int n = varInt(s), preSkip = skipGain & 0xFFFF;
		short gain = (short) (skipGain >> 16);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int serial = 0x4E47454E, seq = 0;
		ByteBuffer head = ByteBuffer.allocate(19).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		head.put("OpusHead".getBytes(StandardCharsets.US_ASCII)).put((byte) 1).put((byte) 1).putShort((short) preSkip).putInt(48000).putShort(gain).put((byte) 0);
		page(out, head.array(), 0x02, 0, serial, seq++);
		byte[] vendor = "newgen".getBytes(StandardCharsets.US_ASCII);
		ByteBuffer tags = ByteBuffer.allocate(8 + 4 + vendor.length + 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		tags.put("OpusTags".getBytes(StandardCharsets.US_ASCII)).putInt(vendor.length).put(vendor).putInt(0);
		page(out, tags.array(), 0, 0, serial, seq++);
		long granule = 0;
		List<byte[]> packets = new ArrayList<>();
		for (int k = 0; k < n; k++) {
			byte[] pkt = new byte[varInt(s)];
			s.get(pkt);
			packets.add(pkt);
		}
		long total = 0;
		for (byte[] p : packets) total += samples(p);
		for (int k = 0; k < packets.size(); k++) {
			granule += samples(packets.get(k));
			boolean last = k == packets.size() - 1;
			page(out, packets.get(k), last ? 0x04 : 0, last ? Math.max(0, total - endTrim) : granule, serial, seq++);
		}
		return out.toByteArray();
	}

	/** Samples at 48 kHz in an Opus packet, from its TOC byte (RFC 6716 section 3.1). */
	static int samples(byte[] p) {
		int config = (p[0] & 0xFF) >> 3;
		int tenths; // frame length in 0.1 ms
		if (config < 12) tenths = new int[]{100, 200, 400, 600}[config % 4];
		else if (config < 16) tenths = new int[]{100, 200}[config % 2];
		else tenths = new int[]{25, 50, 100, 200}[config % 4];
		int c = p[0] & 3, frames = c == 0 ? 1 : c < 3 ? 2 : p[1] & 0x3F;
		return frames * tenths * 48 / 10;
	}

	static void page(ByteArrayOutputStream out, byte[] packet, int flags, long granule, int serial, int seq) {
		int segments = packet.length / 255 + 1;
		ByteBuffer h = ByteBuffer.allocate(27 + segments).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		h.put("OggS".getBytes(StandardCharsets.US_ASCII)).put((byte) 0).put((byte) flags).putLong(granule).putInt(serial).putInt(seq).putInt(0).put((byte) segments);
		for (int i = 0; i < segments - 1; i++) h.put((byte) 255);
		h.put((byte) (packet.length % 255));
		byte[] header = h.array();
		int crc = crc(crc(0, header), packet);
		ByteBuffer.wrap(header, 22, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(crc);
		out.writeBytes(header);
		out.writeBytes(packet);
	}

	static int crc(int crc, byte[] data) {
		for (byte d : data) {
			crc ^= (d & 0xFF) << 24;
			for (int i = 0; i < 8; i++) crc = (crc & 0x80000000) != 0 ? crc << 1 ^ 0x04C11DB7 : crc << 1;
		}
		return crc;
	}

	static int varInt(ByteBuffer b) {
		int v = 0, shift = 0, x;
		do {
			x = b.get() & 0xFF;
			v |= (x & 0x7F) << shift;
			shift += 7;
		} while ((x & 0x80) != 0);
		return v;
	}
}
