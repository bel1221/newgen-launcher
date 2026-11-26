package net.newgen.client.mods.hud;

import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

/**
 * Media Player: the song or video playing on the computer (Spotify, a browser, any app in the Windows volume flyout),
 * with its artist and progress, plus keys for play/pause, next and previous. Off by default; Windows only.
 * The reading happens in {@link SystemMedia}, on its own thread, only while this HUD is on.
 */
public class MediaPlayerMod extends HudMod {
	private final Setting.Num boxWidth = num("width", "Width", 160, 100, 300, 10);
	private final Setting.Bool showArtist = bool("show_artist", "Show artist", true);
	private final Setting.Bool showProgress = bool("show_progress", "Show progress bar", true);
	private final Setting.Bool showTime = bool("show_time", "Show time", true);
	private final Setting.Color barColor = color("bar_color", "Progress bar colour", 0xFF9CCB8F);
	private final Setting.Key playPause = key("play_pause_key", "Play/pause key", -1);
	private final Setting.Key nextKey = key("next_key", "Next track key", -1);
	private final Setting.Key previousKey = key("previous_key", "Previous track key", -1);
	private final Setting.Bool scrollTitle = bool("scroll_title", "Scroll long titles", true);
	private final Setting.Color accentColor = color("accent_color", "Accent colour (title)", 0xFFFFFFFF);
	private final boolean[] wasDown = new boolean[3];
	// Wave 3 vanilla HUD: album art and on-screen buttons.
	private final Setting.Bool showArt = bool("show_art", "Album art", true);
	private final Setting.Bool showButtons = bool("show_buttons", "Buttons while the mouse is free", true);
	private static final Identifier ART_TEXTURE = Identifier.fromNamespaceAndPath("newgen_client", "media_art");
	private static final int BUTTON_W = 16, BUTTON_H = 11;
	private SystemMedia.Art uploaded;
	/** Screen rectangles {x1, y1, x2, y2} of previous, play/pause, next, as last drawn, and when. */
	private final float[][] buttonRects = new float[3][];
	private long buttonsDrawnAt;

	public MediaPlayerMod() {
		super("media_player", "Media Player", "What is playing on your computer (Spotify, YouTube…), with its progress", 1f, 0.7f);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		// Clicks on the buttons while a screen frees the mouse (chat, the HUD editor); other clicks go on as usual.
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) ->
				ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !click(event.x(), event.y(), event.button())));
	}

	/** Runs the button under the mouse; true if one was hit. */
	private boolean click(double mx, double my, int button) {
		if (!enabled || !showButtons.value || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		if (System.currentTimeMillis() - buttonsDrawnAt > 250 || SystemMedia.current() == null) return false;
		SystemMedia.Command[] commands = {SystemMedia.Command.PREVIOUS, SystemMedia.Command.TOGGLE, SystemMedia.Command.NEXT};
		for (int i = 0; i < 3; i++) {
			float[] r = buttonRects[i];
			if (r != null && mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3]) {
				SystemMedia.command(commands[i]);
				return true;
			}
		}
		return false;
	}

	private boolean buttonsVisible(Minecraft mc) {
		return showButtons.value && (mc.screen != null || editing);
	}

	/** Height of the text lines (title, artist, progress, time). */
	private int textHeight() {
		int h = 9;
		if (showArtist.value) h += 11;
		if (showProgress.value) h += 6;
		if (showTime.value) h += 11;
		return h;
	}

	/** Side of the album art square, or 0 when there is none to show. */
	private int artSide() {
		return showArt.value && SystemMedia.art() != null ? Math.max(textHeight(), 20) : 0;
	}

	/** Uploads a new picture to the texture once (game thread). */
	private void uploadArt(Minecraft mc, SystemMedia.Art art) {
		if (art == uploaded) return;
		uploaded = art;
		NativeImage image = new NativeImage(art.width(), art.height(), false);
		for (int y = 0; y < art.height(); y++)
			for (int x = 0; x < art.width(); x++) image.setPixel(x, y, art.argb()[y * art.width() + x]);
		mc.getTextureManager().register(ART_TEXTURE, new DynamicTexture(() -> "newgen media art", image));
	}

	/** Before the accent colour existed the title used the text colour. */
	@Override
	protected void migrate(com.google.gson.JsonObject saved) {
		super.migrate(saved);
		if (!saved.has(accentColor.id)) accentColor.value = textColor.value;
	}

	private void tick(Minecraft mc) {
		SystemMedia.setActive(enabled);
		if (!enabled) return;
		Setting.Key[] keys = {playPause, nextKey, previousKey};
		SystemMedia.Command[] commands = {SystemMedia.Command.TOGGLE, SystemMedia.Command.NEXT, SystemMedia.Command.PREVIOUS};
		for (int i = 0; i < keys.length; i++) {
			boolean down = keys[i].isDown(mc);
			if (down && !wasDown[i]) SystemMedia.command(commands[i]);
			wasDown[i] = down;
		}
	}

	/** Nothing playing: no box (the HUD editor shows the name instead). */
	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		if (SystemMedia.current() == null) return null;
		int art = artSide();
		int w = (int) boxWidth.get() + (art > 0 ? art + 4 : 0);
		int h = Math.max(textHeight(), art);
		if (buttonsVisible(mc)) h += BUTTON_H + 3;
		return new int[] {w, h};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		SystemMedia.Track track = SystemMedia.current();
		if (track == null) return;
		int art = artSide();
		if (art > 0) {
			SystemMedia.Art picture = SystemMedia.art();
			uploadArt(mc, picture);
			// Fit the picture in the square, keeping its shape.
			int dw = picture.width() >= picture.height() ? art : Math.max(1, art * picture.width() / picture.height());
			int dh = picture.height() >= picture.width() ? art : Math.max(1, art * picture.height() / picture.width());
			g.blit(RenderPipelines.GUI_TEXTURED, ART_TEXTURE, (art - dw) / 2, (art - dh) / 2, 0f, 0f, dw, dh,
					picture.width(), picture.height(), picture.width(), picture.height());
			g.pose().pushMatrix();
			g.pose().translate(art + 4, 0);
		}
		drawText(g, mc, track);
		if (art > 0) g.pose().popMatrix();
		if (buttonsVisible(mc)) drawButtons(g, track, (int) boxWidth.get() + (art > 0 ? art + 4 : 0), Math.max(textHeight(), art) + 3);
	}

	/** Previous, play/pause and next, centred in a row; remembers where they landed on screen for clicks. */
	private void drawButtons(GuiGraphics g, SystemMedia.Track track, int width, int y) {
		int x0 = (width - 3 * BUTTON_W - 8) / 2;
		int color = textColor.current();
		for (int i = 0; i < 3; i++) {
			int x = x0 + i * (BUTTON_W + 4);
			g.fill(x, y, x + BUTTON_W, y + BUTTON_H, 0x40FFFFFF);
			int cx = x + BUTTON_W / 2, cy = y + BUTTON_H / 2;
			switch (i) {
				case 0 -> { // |<
					g.fill(cx - 4, cy - 3, cx - 3, cy + 4, color);
					triangle(g, cx + 3, cy, -1, color);
				}
				case 1 -> {
					if (track.playing()) { // ||
						g.fill(cx - 3, cy - 3, cx - 1, cy + 4, color);
						g.fill(cx + 1, cy - 3, cx + 3, cy + 4, color);
					} else {
						triangle(g, cx - 2, cy, 1, color); // >
					}
				}
				default -> { // >|
					triangle(g, cx - 3, cy, 1, color);
					g.fill(cx + 3, cy - 3, cx + 4, cy + 4, color);
				}
			}
			Vector2f a = g.pose().transformPosition(new Vector2f(x, y));
			Vector2f b = g.pose().transformPosition(new Vector2f(x + BUTTON_W, y + BUTTON_H));
			buttonRects[i] = new float[] {Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y)};
		}
		buttonsDrawnAt = System.currentTimeMillis();
	}

	/** A small triangle, 4 px wide and 7 tall, pointing right (dir 1, starting at x) or left (dir -1, ending at x). */
	private static void triangle(GuiGraphics g, int x, int cy, int dir, int color) {
		for (int i = 0; i < 4; i++) {
			int h = 3 - i; // column i from the flat side
			int cx = dir > 0 ? x + i : x - i;
			g.fill(cx, cy - h, cx + 1, cy + h + 1, color);
		}
	}

	private void drawText(GuiGraphics g, Minecraft mc, SystemMedia.Track track) {
		int w = (int) boxWidth.get();
		int y = 0;
		title(g, mc, track.title(), w);
		y += 11;
		if (showArtist.value) {
			g.drawString(mc.font, fit(mc, track.artist(), w), 0, y, dim(textColor.current()), textShadow.value);
			y += 11;
		}
		long now = System.currentTimeMillis();
		long position = track.position(now);
		if (showProgress.value) {
			g.fill(0, y, w, y + 2, 0x40FFFFFF);
			if (track.durationMs() > 0) g.fill(0, y, (int) (w * Math.min(1.0, (double) position / track.durationMs())), y + 2, barColor.current());
			y += 6;
		}
		if (showTime.value) {
			String time = track.durationMs() > 0 ? clock(position) + " / " + clock(track.durationMs()) : clock(position);
			if (!track.playing()) time += " · " + I18n.get("newgen_client.media.paused");
			g.drawString(mc.font, time, 0, y, dim(textColor.current()), textShadow.value);
		}
	}

	/** The title in the accent colour; too long for the box, it scrolls (or is cut with an ellipsis). */
	private void title(GuiGraphics g, Minecraft mc, String title, int w) {
		int color = accentColor.current();
		int tw = mc.font.width(title);
		if (tw <= w || !scrollTitle.value) {
			g.drawString(mc.font, fit(mc, title, w), 0, 0, color, textShadow.value);
			return;
		}
		int gap = 24, offset = HudValues.marquee(System.currentTimeMillis(), tw + gap, 25, 1500);
		g.enableScissor(0, 0, w, 10);
		g.drawString(mc.font, title, -offset, 0, color, textShadow.value);
		g.drawString(mc.font, title, -offset + tw + gap, 0, color, textShadow.value);
		g.disableScissor();
	}

	/** The text cut to the box width with an ellipsis. */
	private static String fit(Minecraft mc, String text, int width) {
		if (mc.font.width(text) <= width) return text;
		return mc.font.plainSubstrByWidth(text, width - mc.font.width("…")) + "…";
	}

	/** The text colour at two-thirds opacity, for the second lines. */
	private static int dim(int color) {
		return ((color >>> 24) * 2 / 3) << 24 | color & 0xFFFFFF;
	}

	/** 75 000 ms → "1:15"; an hour or more → "1:02:03". */
	static String clock(long ms) {
		long s = Math.max(0, ms / 1000);
		return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
	}
}
