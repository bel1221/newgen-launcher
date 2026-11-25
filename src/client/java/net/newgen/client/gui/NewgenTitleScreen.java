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
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * NewGen's title screen, laid out like a minimal client menu: the panorama, the logo in the middle, a 200 wide stack
 * of translucent buttons (Play NewGen in blue, Singleplayer, Multiplayer, then Options | Mods), the player's name
 * top left, language and quit top right, version and copyright in the bottom corners.
 * The logo and the stack enter once a game session (about 0.8 s, ease-out), then stand.
 */
public class NewgenTitleScreen extends Screen {
	private static final String SERVER = "newgenmc.net:25565";
	private static final int STACK_W = 200, BUTTON_H = 20, GAP = 4, CORNER = 20, EDGE = 6;
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
		int w = Math.min(STACK_W, width - 40), step = BUTTON_H + GAP, stack = 4 * step - GAP;
		// The logo shrinks with the window; the stack always fits above the bottom line.
		logoW = Math.max(0, Math.min(160, (height - stack - 60) * 4));
		int logoH = logoW >= 40 ? Ui.logoHeight(logoW) + 12 : 0;
		logoY = Math.max(CORNER + EDGE, (height - logoH - stack) / 2);
		int x = (width - w) / 2, y = logoY + logoH, half = (w - GAP) / 2;

		addRenderableWidget(new Ui.Btn(x, y, w, BUTTON_H, Ui.tr("title.play"), Ui.I_PLAY, Ui.BLUE, Ui.CAPS, () -> ConnectScreen.startConnecting(
				this, minecraft, ServerAddress.parseString(SERVER), new ServerData("NewGen", SERVER, ServerData.Type.OTHER), false, null)));
		addRenderableWidget(new Ui.Btn(x, y + step, w, BUTTON_H, I18n.get("menu.singleplayer"), Ui.I_USER, GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new SelectWorldScreen(this))));
		addRenderableWidget(new Ui.Btn(x, y + step * 2, w, BUTTON_H, I18n.get("menu.multiplayer"), Ui.I_USERS, GLASS, Ui.CAPS,
				() -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this))));
		addRenderableWidget(new Ui.Btn(x, y + step * 3, half, BUTTON_H, I18n.get("menu.options").replace("...", ""), Ui.I_GEAR, GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
		addRenderableWidget(new Ui.Btn(x + w - half, y + step * 3, half, BUTTON_H, Ui.tr("mods"), Ui.I_GRID, GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new ModsScreen(this))));

		// Top right: language, then quit (the window's own corner).
		addRenderableWidget(new Ui.Btn(width - EDGE - CORNER, EDGE, CORNER, CORNER, "", Ui.I_CLOSE, GLASS, Ui.CAPS, minecraft::stop)
				.narration(I18n.get("menu.quit")));
		addRenderableWidget(new Ui.Btn(width - EDGE - CORNER * 2 - GAP, EDGE, CORNER, CORNER, "", Ui.I_GLOBE, GLASS, Ui.CAPS,
				() -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())))
				.narration(I18n.get("options.language")));

		Component copyright = Component.translatable("title.credits").withStyle(Ui.style(Ui.UI));
		int copyrightW = font.width(copyright);
		addRenderableWidget(new PlainTextButton(width - copyrightW - EDGE, height - 14, copyrightW, 10, copyright,
				button -> minecraft.setScreen(new CreditsAndAttributionScreen(this)), font));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (!VideoBackground.render(g, width, height)) renderPanorama(g, delta);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Entrance: the logo rises 8 units and fades in over 700 ms; the widgets follow 12 units up over 520 ms from 300 ms.
		float logo = Ui.ease(enteredAt, 700), rest = Ui.ease(enteredAt + 300, 520);
		g.pose().pushMatrix();
		g.pose().translate(0, (1 - logo) * 8);
		Ui.logo(g, width / 2, logoY, logoW, Math.max(4, (int) (logo * 255)) << 24 | 0xFFFFFF);
		g.pose().popMatrix();

		// Top left: the player's name on a glass chip (who will join), not a button.
		String name = minecraft.getUser().getName();
		int chipW = Ui.width(name, Ui.CAPS) + 10 + 14;
		Ui.round(g, EDGE, EDGE, chipW, CORNER, Ui.R_SM, GLASS);
		Ui.glyph(g, Ui.I_USER, EDGE + 7, EDGE + 5, 10, Ui.SOFT);
		Ui.text(g, name, EDGE + 21, EDGE + 7, Ui.TEXT, Ui.CAPS);

		g.pose().pushMatrix();
		g.pose().translate(0, (1 - rest) * 12);
		super.render(g, mouseX, mouseY, delta);
		g.pose().popMatrix();
		String version = "Minecraft " + SharedConstants.getCurrentVersion().name();
		if (Minecraft.checkModStatus().shouldReportAsModified()) version += I18n.get("menu.modded");
		Ui.text(g, version, EDGE, height - 13, Ui.DIM, Ui.UI);
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
