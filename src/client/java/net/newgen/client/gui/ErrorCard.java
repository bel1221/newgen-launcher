package net.newgen.client.gui;

import java.lang.reflect.Field;
import java.util.List;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.DisconnectionDetails;

/**
 * A failed join or a disconnect, in the same card as the loading screens: what happened, why (wrapped), and under it
 * Try again (back into the server that failed, when there was one) next to the screen's own Back button.
 */
public final class ErrorCard {
	private static final int CW = 260;
	/** The server last joined or tried (kept from the connect screen), for Try again. */
	private static ServerData last;

	private ErrorCard() {
	}

	/** The connect screen calls this every frame: it knows the server being tried. Only real multiplayer servers. */
	public static void remember(Minecraft mc) {
		ServerData s = mc.getCurrentServer();
		if (s != null && retryable(s.ip, s.isLan(), mc.isLocalServer())) last = s;
	}

	/** Try again may only ever rejoin a remote multiplayer server: never a LAN game or the integrated (single-player) one. */
	static boolean retryable(String ip, boolean lan, boolean local) {
		return ip != null && !ip.isBlank() && !lan && !local;
	}

	public static void attach(Minecraft mc, Screen screen, int width, int height) {
		if (!(screen instanceof DisconnectedScreen)) return;
		List<String> all = Overlay.wrap(reason(screen), CW - 24), lines = all.subList(0, Math.min(8, all.size())); // at most 8 lines
		int h = cardH(lines), y = (height - h) / 2 + h + 8;
		List<AbstractWidget> buttons = Screens.getButtons(screen);
		if (last != null && retryable(last.ip, last.isLan(), false)) {
			ServerData server = last;
			// Mouse click only: keyboard focus starts on the first widget, so a stray Enter must never rejoin.
			// Added after the screen's own Back button, so the initial focus stays on Back.
			buttons.add(new Ui.Btn(0, 0, 150, 20, Ui.tr("error.retry"), Ui.I_PLAY, Ui.PRIMARY, Ui.CAPS, () -> ConnectScreen.startConnecting(
					new NewgenTitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null)) {
				@Override
				public void onPress(net.minecraft.client.input.InputWithModifiers input) {
					if (input instanceof net.minecraft.client.input.MouseButtonEvent) super.onPress(input);
				}
			});
		}
		for (AbstractWidget w : buttons) {
			if (!(w instanceof net.minecraft.client.gui.components.AbstractButton)) continue; // text stays under the card
			w.setPosition((width - w.getWidth()) / 2, y);
			y += w.getHeight() + 4;
		}
		ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> draw(s, g, mx, my, delta, lines));
	}

	private static int cardH(List<String> lines) {
		return 34 + lines.size() * 10 + 8;
	}

	/** Why the connection ended: the disconnect details' reason, read by type (the screen has no getter). */
	private static String reason(Screen s) {
		for (Field f : DisconnectedScreen.class.getDeclaredFields()) {
			if (f.getType() != DisconnectionDetails.class) continue;
			try {
				f.setAccessible(true);
				return ((DisconnectionDetails) f.get(s)).reason().getString();
			} catch (Exception e) {
				break;
			}
		}
		return "";
	}

	private static void draw(Screen s, GuiGraphics g, int mouseX, int mouseY, float delta, List<String> lines) {
		int width = s.width, height = s.height, h = cardH(lines);
		g.nextStratum();
		if (!VideoBackground.render(g, width, height)) g.fill(0, 0, width, height, 0xFF0E1216);
		g.fill(0, 0, width, height, 0x40000000);
		int x = (width - CW) / 2, y = (height - h) / 2;
		Ui.panel(g, x, y, CW, h);
		Ui.icon(g, x + 12, y + 12, 14);
		Ui.text(g, Ui.fit(s.getTitle().getString(), CW - 44, Ui.TITLE, false), x + 32, y + 15, Ui.TEXT, Ui.TITLE);
		for (int i = 0; i < lines.size(); i++) Ui.text(g, lines.get(i), x + 12, y + 34 + i * 10, Ui.SOFT, Ui.UI);
		// Only the buttons come back on top: the screen's own text widgets stay under the card (it says the same).
		for (AbstractWidget w : Screens.getButtons(s)) if (w instanceof net.minecraft.client.gui.components.AbstractButton) w.render(g, mouseX, mouseY, delta);
	}
}
