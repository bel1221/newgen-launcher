package net.newgen.client.mods.hud;

import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

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

	public MediaPlayerMod() {
		super("media_player", "Media Player", "What is playing on your computer (Spotify, YouTube…), with its progress", 1f, 0.7f);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
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
		int h = 9;
		if (showArtist.value) h += 11;
		if (showProgress.value) h += 6;
		if (showTime.value) h += 11;
		return new int[] {(int) boxWidth.get(), h};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		SystemMedia.Track track = SystemMedia.current();
		if (track == null) return;
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
