package net.newgen.client.mods.utility;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
		CompletableFuture.supplyAsync(() -> download(url)).thenAccept(pixels -> Minecraft.getInstance().execute(() -> {
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
	private static NativeImage download(String url) {
		try {
			if (client == null) client = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();
			HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).header("User-Agent", "NewgenClient").GET().build();
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			String type = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
			try (InputStream in = response.body()) {
				if (response.statusCode() != 200 || !type.startsWith("image/")) return null;
				byte[] bytes = readCapped(in, MAX_BYTES);
				return bytes == null ? null : decode(bytes);
			}
		} catch (Exception e) {
			return null;
		}
	}

	/** At most cap bytes, or null when the stream is longer. */
	static byte[] readCapped(InputStream in, int cap) throws java.io.IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = in.read(buf)) > 0) {
			if (out.size() + n > cap) return null;
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
