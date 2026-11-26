package net.newgen.client.mods.utility;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.newgen.client.NewgenClient;

/**
 * Chat image link preview: hovering a link in the open chat loads that one image (only then), from http/https only,
 * at most 4 MB and 5 seconds, and shows it scaled to 256 px. Nothing is loaded for links that are never hovered.
 */
public final class ImagePreview {
	public static final int MAX_BYTES = 4 * 1024 * 1024;
	public static final int MAX_SIDE = 256;
	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final int CACHE = 16;
	private static final int MAX_REDIRECTS = 3;

	/** A loaded preview: its texture and size. */
	public record Image(Identifier texture, int width, int height) {
	}

	/** Per URL: null while loading, an Image when ready, FAILED when not an image or not reachable. */
	private static final Map<String, Object> CACHE_MAP = new LinkedHashMap<>(CACHE, 0.75f, true);
	private static final Object FAILED = new Object();
	private static HttpClient client;
	private static int counter;

	private ImagePreview() {
	}

	/** Whether this is a link we would try: http or https. */
	public static boolean previewable(String url) {
		try {
			String scheme = URI.create(url).getScheme();
			return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/** The preview for a hovered link: ready, or null (starts loading it the first time). Game thread only. */
	public static Image get(String url) {
		if (!previewable(url)) return null;
		if (CACHE_MAP.containsKey(url)) {
			Object v = CACHE_MAP.get(url);
			return v instanceof Image image ? image : null;
		}
		CACHE_MAP.put(url, null);
		while (CACHE_MAP.size() > CACHE) evict();
		CompletableFuture.supplyAsync(() -> download(url)).completeOnTimeout(null, TIMEOUT.toMillis() + 500, java.util.concurrent.TimeUnit.MILLISECONDS).thenAccept(pixels -> Minecraft.getInstance().execute(() -> {
			if (!CACHE_MAP.containsKey(url)) return;
			if (pixels == null) {
				CACHE_MAP.put(url, FAILED);
				return;
			}
			Identifier id = Identifier.fromNamespaceAndPath(NewgenClient.ID, "chat_preview/" + counter++);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "chat preview", pixels));
			CACHE_MAP.put(url, new Image(id, pixels.getWidth(), pixels.getHeight()));
		}));
		return null;
	}

	private static void evict() {
		String oldest = CACHE_MAP.keySet().iterator().next();
		if (CACHE_MAP.remove(oldest) instanceof Image image) Minecraft.getInstance().getTextureManager().release(image.texture());
	}

	/** Downloads and decodes the image, scaled to fit MAX_SIDE; null on any failure. Off the game thread. */
	/**
	 * Downloads and decodes the image, scaled to fit MAX_SIDE; null on any failure. Off the game thread. Only public
	 * addresses: every hop (at most 3 redirects, followed by hand) is resolved and refused if it points into this computer
	 * or the local network. TIMEOUT is the whole budget, reading included.
	 */
	private static NativeImage download(String url) {
		long deadline = System.nanoTime() + TIMEOUT.toNanos();
		try {
			if (client == null) client = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
			URI uri = URI.create(url);
			for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
				if (!previewable(uri.toString()) || !publicHost(uri.getHost())) return null;
				Duration left = Duration.ofNanos(deadline - System.nanoTime());
				if (left.isNegative() || left.isZero()) return null;
				HttpRequest request = HttpRequest.newBuilder(uri).timeout(left).header("User-Agent", "NewgenClient").GET().build();
				HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
				try (InputStream in = response.body()) {
					int status = response.statusCode();
					if (status >= 300 && status < 400) {
						String location = response.headers().firstValue("Location").orElse(null);
						if (location == null) return null;
						uri = uri.resolve(location);
						continue;
					}
					String type = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
					if (status != 200 || !type.startsWith("image/")) return null;
					byte[] bytes = readCapped(in, MAX_BYTES, deadline);
					return bytes == null ? null : decode(bytes);
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/** Whether every address the host resolves to is a public one (not this computer, the LAN, link-local or multicast). */
	static boolean publicHost(String host) {
		if (host == null || host.isBlank()) return false;
		try {
			String h = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
			InetAddress[] addresses = InetAddress.getAllByName(h);
			if (addresses.length == 0) return false;
			for (InetAddress a : addresses) if (!publicAddress(a)) return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static boolean publicAddress(InetAddress a) {
		if (a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isMulticastAddress()) return false;
		byte[] b = a.getAddress();
		if (a instanceof Inet6Address && (b[0] & 0xFE) == 0xFC) return false; // unique-local fc00::/7
		if (a instanceof Inet4Address && ((b[0] & 0xFF) == 0 || (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64)) return false; // 0/8, carrier-grade NAT 100.64/10
		return true;
	}

	/** At most cap bytes, or null when the stream is longer. */
	static byte[] readCapped(InputStream in, int cap) throws java.io.IOException {
		return readCapped(in, cap, Long.MAX_VALUE);
	}

	/** At most cap bytes before the deadline (System.nanoTime), or null. */
	static byte[] readCapped(InputStream in, int cap, long deadline) throws java.io.IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = in.read(buf)) > 0) {
			if (out.size() + n > cap || System.nanoTime() > deadline) return null;
			out.write(buf, 0, n);
		}
		return out.toByteArray();
	}

	/** PNG, JPEG or GIF through ImageIO; when the runtime has no AWT (it may not), PNG through the game's own decoder. */
	private static NativeImage decode(byte[] bytes) throws java.io.IOException {
		BufferedImage src;
		try {
			src = ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (LinkageError noAwt) {
			try (NativeImage png = NativeImage.read(bytes)) {
				int[] size = fit(png.getWidth(), png.getHeight(), MAX_SIDE);
				NativeImage image = new NativeImage(size[0], size[1], false);
				for (int y = 0; y < size[1]; y++)
					for (int x = 0; x < size[0]; x++)
						image.setPixel(x, y, png.getPixel(x * png.getWidth() / size[0], y * png.getHeight() / size[1]));
				return image;
			}
		}
		if (src == null) return null;
		int[] size = fit(src.getWidth(), src.getHeight(), MAX_SIDE);
		NativeImage image = new NativeImage(size[0], size[1], false);
		for (int y = 0; y < size[1]; y++)
			for (int x = 0; x < size[0]; x++)
				image.setPixel(x, y, src.getRGB(x * src.getWidth() / size[0], y * src.getHeight() / size[1]));
		return image;
	}

	/** A width and height that fit inside max × max, keeping the aspect ratio (never larger than the original). */
	static int[] fit(int w, int h, int max) {
		double s = Math.min(1.0, (double) max / Math.max(w, h));
		return new int[] {Math.max(1, (int) Math.round(w * s)), Math.max(1, (int) Math.round(h * s))};
	}
}
