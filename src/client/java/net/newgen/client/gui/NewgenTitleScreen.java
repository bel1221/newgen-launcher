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
 * NewGen's title screen: the panorama, the NewGen logo, one blue "Play NewGen" button and the secondary actions.
 * Shown instead of the vanilla title screen (see NewgenClient). The copyright line and version text stay, as on the vanilla one.
 * The logo and the buttons enter once a game session (about 0.9 s, ease-out), then stand.
 */
public class NewgenTitleScreen extends Screen {
	private static final String SERVER = "newgenmc.net:25565";
	/** The main action is taller than the rest; every other button is 20 high. */
	private static final int PLAY_H = 24, BUTTON_H = 20, GAP = 4, ROW_GAP = 6;
	/** Buttons here lie on the bright panorama, not on a panel, so they are darker than Ui.DARK. */
	private static final int GLASS = 0x99000000;
	/** When the title screen was first shown this session; the entrance plays once from here. */
	private static long enteredAt = -1;

	private int logoW, logoY;

	public NewgenTitleScreen() {
		super(Component.translatable("narrator.screen.title"));
	}

	@Override
	protected void init() {
		if (enteredAt < 0) enteredAt = System.currentTimeMillis();
		int w = Math.min(220, width - 40), stack = PLAY_H + ROW_GAP + BUTTON_H;
		// The logo shrinks with the window; the buttons always fit above the bottom row and the copyright line.
		logoW = Math.max(0, Math.min(w, (height - stack - BUTTON_H - 64) * 4));
		int logoH = logoW >= 40 ? Ui.logoHeight(logoW) + 14 : 0;
		logoY = Math.max(4, (height - 40 - logoH - stack) / 2);
		int x = (width - w) / 2, y = logoY + logoH, half = (w - GAP) / 2;

		addRenderableWidget(new Ui.Btn(x, y, w, PLAY_H, Ui.tr("title.play"), Ui.BLUE, Ui.TITLE, () -> ConnectScreen.startConnecting(
				this, minecraft, ServerAddress.parseString(SERVER), new ServerData("NewGen", SERVER, ServerData.Type.OTHER), false, null)));
		int row = y + PLAY_H + ROW_GAP;
		addRenderableWidget(new Ui.Btn(x, row, half, BUTTON_H, I18n.get("menu.singleplayer"), GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new SelectWorldScreen(this))));
		addRenderableWidget(new Ui.Btn(x + w - half, row, half, BUTTON_H, I18n.get("menu.multiplayer"), GLASS, Ui.CAPS,
				() -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this))));
		// The bottom row sits at the foot of the screen, or right under the stack when the window is low.
		int third = (w - GAP * 2) / 3, rowY = Math.max(row + BUTTON_H + ROW_GAP, height - 38);
		addRenderableWidget(new Ui.Btn(x, rowY, third, BUTTON_H, I18n.get("menu.options").replace("...", ""), GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
		addRenderableWidget(new Ui.Btn(x + third + GAP, rowY, w - (third + GAP) * 2, BUTTON_H, Ui.tr("mods"), GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new ModsScreen(this))));
		addRenderableWidget(new Ui.Btn(x + w - third, rowY, third, BUTTON_H, I18n.get("menu.quit"), GLASS, Ui.CAPS, minecraft::stop));

		Component copyright = Component.translatable("title.credits").withStyle(Ui.style(Ui.UI));
		int copyrightW = font.width(copyright);
		addRenderableWidget(new PlainTextButton(width - copyrightW - 3, height - 11, copyrightW, 10, copyright,
				button -> minecraft.setScreen(new CreditsAndAttributionScreen(this)), font));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		renderPanorama(g, delta);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Entrance: the logo rises 8 units and fades in over 700 ms; the buttons follow 12 units up over 520 ms from 300 ms.
		float logo = Ui.ease(enteredAt, 700), rest = Ui.ease(enteredAt + 300, 520);
		g.pose().pushMatrix();
		g.pose().translate(0, (1 - logo) * 8);
		Ui.logo(g, width / 2, logoY, logoW, Math.max(4, (int) (logo * 255)) << 24 | 0xFFFFFF);
		g.pose().popMatrix();
		g.pose().pushMatrix();
		g.pose().translate(0, (1 - rest) * 12);
		super.render(g, mouseX, mouseY, delta);
		g.pose().popMatrix();
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
