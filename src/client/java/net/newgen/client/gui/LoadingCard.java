package net.newgen.client.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The joining and loading screens in our system: the title video (or a dark fill), one glass card with what is
 * happening and three breathing dots, and the screen's own buttons (Cancel) under the card. Vanilla still does the
 * work; we only draw over it, so nothing changes in how joining behaves.
 */
public final class LoadingCard {
	private static final int CW = 240, CH = 62;

	private LoadingCard() {
	}

	private static boolean covered(Screen s) {
		return s instanceof ConnectScreen || s instanceof LevelLoadingScreen || s instanceof ProgressScreen || s instanceof GenericMessageScreen;
	}

	/** Called from ScreenEvents.AFTER_INIT for every screen. */
	public static void attach(Minecraft mc, Screen screen, int width, int height) {
		if (!covered(screen)) return;
		int y = (height - CH) / 2 + CH + 8;
		// The screen's buttons go under the card, centred.
		for (AbstractWidget w : Screens.getButtons(screen)) {
			w.setPosition((width - w.getWidth()) / 2, y);
			y += w.getHeight() + 4;
		}
		long start = System.currentTimeMillis();
		ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> draw(mc, s, g, mx, my, delta, start));
	}

	/** What the screen is doing: the connect screen's status line (read by type, it has no getter), else its title. */
	private static Component stage(Screen s) {
		if (s instanceof ConnectScreen) {
			for (Field f : ConnectScreen.class.getDeclaredFields()) {
				// The status line is the one Component that changes (not final; the failure title is final).
				if (f.getType() != Component.class || Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;
				try {
					f.setAccessible(true);
					Object v = f.get(s);
					if (v instanceof Component c) return c;
				} catch (Exception e) {
					break;
				}
			}
			return Component.translatable("connect.connecting");
		}
		return s.getTitle();
	}

	private static void draw(Minecraft mc, Screen s, GuiGraphics g, int mouseX, int mouseY, float delta, long start) {
		if (s instanceof ConnectScreen) ErrorCard.remember(mc);
		int width = s.width, height = s.height;
		g.nextStratum();
		if (!VideoBackground.render(g, width, height)) g.fill(0, 0, width, height, 0xFF0E1216);
		g.fill(0, 0, width, height, 0x40000000);
		int x = (width - CW) / 2, y = (height - CH) / 2;
		Ui.panel(g, x, y, CW, CH);
		Ui.icon(g, x + 12, y + 12, 14);
		String title = mc.getCurrentServer() != null ? mc.getCurrentServer().name : Ui.tr("loading.world");
		Ui.text(g, Ui.fit(title, CW - 44, Ui.TITLE, false), x + 32, y + 15, Ui.TEXT, Ui.TITLE);
		String text = stage(s).getString();
		if (text.isBlank()) text = Ui.tr("loading.wait");
		Ui.text(g, Ui.fit(text, CW - 24, Ui.UI, false), x + 12, y + 36, Ui.SOFT, Ui.UI);
		// Three dots that brighten in turn (still with Reduce motion).
		long t = System.currentTimeMillis() - start;
		for (int i = 0; i < 3; i++) {
			float k = Ui.reduceMotion ? 0.6f : (float) (0.5 + 0.5 * Math.sin(t / 220.0 - i * 0.9));
			int a = Math.round(80 + 175 * k);
			Ui.round(g, x + CW - 30 + i * 7, y + 18, 4, 4, 2, a << 24 | Ui.ACCENT & 0xFFFFFF);
		}
		for (AbstractWidget w : Screens.getButtons(s)) w.render(g, mouseX, mouseY, delta);
	}
}
