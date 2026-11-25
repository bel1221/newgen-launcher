package net.newgen.client.gui;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * NewGen's title screen: the vanilla panorama, the NewGen logo and a stack of buttons.
 * Shown instead of the vanilla title screen (see NewgenClient). The copyright line and version text stay, as on the vanilla one.
 */
public class NewgenTitleScreen extends Screen {
	private static final String SERVER = "newgenmc.net:25565";
	private static final int BUTTON_H = 20, SMALL_H = 16, GAP = 4;
	/** Buttons here lie on the bright panorama, not on a panel, so they are darker than Ui.DARK. */
	private static final int GLASS = 0x99000000;

	private int logoW, logoY;

	public NewgenTitleScreen() {
		super(Component.translatable("narrator.screen.title"));
	}

	@Override
	protected void init() {
		int w = Math.min(200, width - 40), stack = 3 * BUTTON_H + 2 * GAP;
		// The logo shrinks with the window; the buttons always fit above the small row and the copyright line.
		logoW = Math.max(0, Math.min(w, (height - stack - 64) * 4));
		int logoH = logoW >= 40 ? Ui.logoHeight(logoW) + 10 : 0;
		logoY = Math.max(4, (height - 40 - logoH - stack) / 2);
		int x = (width - w) / 2, y = logoY + logoH, step = BUTTON_H + GAP;

		addRenderableWidget(new Ui.Btn(x, y, w, BUTTON_H, "Play NewGen", Ui.BLUE, Ui.UI, () -> ConnectScreen.startConnecting(
				this, minecraft, ServerAddress.parseString(SERVER), new ServerData("NewGen", SERVER, ServerData.Type.OTHER), false, null)));
		addRenderableWidget(new Ui.Btn(x, y + step, w, BUTTON_H, I18n.get("menu.singleplayer"), GLASS, Ui.UI,
				() -> minecraft.setScreen(new SelectWorldScreen(this))));
		addRenderableWidget(new Ui.Btn(x, y + step * 2, w, BUTTON_H, I18n.get("menu.multiplayer"), GLASS, Ui.UI,
				() -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this))));
		// The small row sits at the bottom of the screen, or right under the stack when the window is low.
		int third = (w - GAP * 2) / 3, rowY = Math.max(y + stack + GAP, height - 34);
		addRenderableWidget(new Ui.Btn(x, rowY, third, SMALL_H, I18n.get("menu.options").replace("...", ""), GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
		addRenderableWidget(new Ui.Btn(x + third + GAP, rowY, w - (third + GAP) * 2, SMALL_H, "Mods", GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new ModsScreen(this))));
		addRenderableWidget(new Ui.Btn(x + w - third, rowY, third, SMALL_H, "Quit", GLASS, Ui.CAPS, minecraft::stop));

		Component copyright = Component.translatable("title.credits").withStyle(Ui.style(Ui.UI));
		int copyrightW = font.width(copyright);
		addRenderableWidget(new PlainTextButton(width - copyrightW - 2, height - 10, copyrightW, 10, copyright,
				button -> minecraft.setScreen(new CreditsAndAttributionScreen(this)), font));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		renderPanorama(g, delta);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Ui.logo(g, width / 2, logoY, logoW);
		super.render(g, mouseX, mouseY, delta);
		String version = "Minecraft " + SharedConstants.getCurrentVersion().name();
		if (Minecraft.checkModStatus().shouldReportAsModified()) version += I18n.get("menu.modded");
		Ui.text(g, version, 3, height - 10, Ui.SOFT, Ui.UI);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
