package net.newgen.client.emotes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.newgen.client.gui.Ui;

/**
 * The emote wheel, open while the wheel key is held: 8 round slots on a ring around the middle of the screen.
 * The slot in the direction of the mouse lights up in blue; letting go of the key (or a click) plays it.
 * The middle shows the lit emote's name and author.
 * <pre>
 *              ( 1 )
 *        ( 8 )       ( 2 )
 *     ( 7 )   [ name  ]   ( 3 )
 *        ( 6 )  by x  ( 4 )
 *              ( 5 )
 * </pre>
 */
public class EmoteWheelScreen extends Screen {
	private static final int RADIUS = 62, SLOT = 34, ICON = 24, CENTER = 46;
	private final EmotesMod mod;
	private int hovered = -1;
	private double lastMouseX = Double.NaN, lastMouseY = Double.NaN; // the mouse only takes over the selection when it moves
	/** Per slot, 0..1: how far it has grown toward the hovered size (1.08), in 120 ms. */
	private final float[] grow = new float[EmotesMod.SLOTS];
	private long lastFrame = System.currentTimeMillis(), hoverSince;
	private int lastHovered = -1;
	/** The hovered emote's sound, played after 400 ms of hover at half volume; stopped when the hover leaves. */
	private net.minecraft.client.resources.sounds.SoundInstance preview;
	private boolean opened;
	/**
	 * Live previews: per filled slot a stand-in player (a RemotePlayer with your own profile, so your skin, never added
	 * to the world) that loops the slot's emote on its own animation layer, so the real player is not touched. Ticked
	 * here at 20 per second like any player; drawn every frame like the inventory's player figure. With Reduce motion the
	 * figure holds one pose. Without a world (no level or player) the slots show the emote's icon instead.
	 */
	private final RemotePlayer[] figures = new RemotePlayer[EmotesMod.SLOTS];
	private final int[] idle = new int[EmotesMod.SLOTS]; // ticks a one-shot emote has rested before it plays again
	private static final int REST_TICKS = 12, KEY_POSE_TICKS = 10;

	public EmoteWheelScreen(EmotesMod mod) {
		super(Component.literal("Emotes"));
		this.mod = mod;
	}

	@Override
	protected void init() {
		Ui.hideTutorialToasts();
		if (!opened) {
			opened = true;
			Ui.sound("open");
			makeFigures();
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0x30000000); // the game stays visible
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		int cx = width / 2, cy = height / 2;
		double dx = mouseX - cx, dy = mouseY - cy;
		// Slot 0 is at the top, then clockwise every 45 degrees; the middle (under 22 units) chooses nothing.
		if (mouseX != lastMouseX || mouseY != lastMouseY) {
			if (!Double.isNaN(lastMouseX))
				hovered = dx * dx + dy * dy < 22 * 22 ? -1 : Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dx, -dy)) / 45), EmotesMod.SLOTS);
			lastMouseX = mouseX;
			lastMouseY = mouseY;
		}

		long now = System.currentTimeMillis();
		float dt = Math.min(100, now - lastFrame) / 120f;
		lastFrame = now;
		if (hovered != lastHovered) {
			lastHovered = hovered;
			hoverSince = now;
			stopPreview();
			if (hovered >= 0 && EmoteLibrary.get(mod.slot(hovered)) != null) Ui.sound("hover");
		}
		EmoteLibrary.Emote held = hovered < 0 ? null : EmoteLibrary.get(mod.slot(hovered));
		if (held != null && held.sound() != null && preview == null && now - hoverSince >= 400 && mod.previewVolume() > 0) {
			preview = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
					net.minecraft.sounds.SoundEvent.createVariableRangeEvent(held.sound()), 1f, mod.previewVolume());
			minecraft.getSoundManager().play(preview);
		}

		for (int i = 0; i < EmotesMod.SLOTS; i++) {
			double a = Math.toRadians(i * 45);
			int x = cx + (int) Math.round(Math.sin(a) * RADIUS) - SLOT / 2, y = cy - (int) Math.round(Math.cos(a) * RADIUS) - SLOT / 2;
			EmoteLibrary.Emote emote = EmoteLibrary.get(mod.slot(i));
			boolean on = i == hovered && emote != null;
			grow[i] = Math.max(0, Math.min(1, grow[i] + (on && !Ui.reduceMotion ? dt : -dt)));
			// The hovered slot grows to 108 % around its own centre (not with Reduce motion).
			float scale = 1 + 0.08f * (1 - (1 - grow[i]) * (1 - grow[i]));
			g.pose().pushMatrix();
			g.pose().translate(x + SLOT / 2f, y + SLOT / 2f);
			g.pose().scale(scale, scale);
			g.pose().translate(-(x + SLOT / 2f), -(y + SLOT / 2f));
			Ui.round(g, x, y, SLOT, SLOT, SLOT / 2, on ? Ui.SELECTED : Ui.DARK);
			Ui.ring(g, x, y, SLOT, SLOT, SLOT / 2, on ? 0x80FFFFFF : Ui.LINE);
			if (emote != null) {
				int o = (SLOT - ICON) / 2;
				if (figures[i] != null) {
					// The figure stands in the slot, feet near the rim, facing you (it looks at the slot's own centre).
					int fx = x + 4, fy = y + 3, fw = SLOT - 8, fh = SLOT - 5;
					InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx, fy, fx + fw, fy + fh, 13, 0.0625f,
							fx + fw / 2f, fy + fh / 3f, figures[i]);
				} else {
					emote.drawIcon(g, x + o, y + o, ICON); // no world yet: the icon (a circle, see tools/EmoteImport)
				}
			} else {
				Ui.centered(g, "+", x + SLOT / 2, y + (SLOT - 7) / 2, Ui.DIM, Ui.TITLE); // an empty slot, to fill in Edit wheel
			}
			g.pose().popMatrix();
			// The slot's number key, just outside the ring on the same spoke.
			int nx = cx + (int) Math.round(Math.sin(a) * (RADIUS + SLOT / 2 + 7)), ny = cy - (int) Math.round(Math.cos(a) * (RADIUS + SLOT / 2 + 7));
			Ui.centered(g, String.valueOf(i + 1), nx, ny - 3, on ? Ui.TEXT : Ui.DIM, Ui.CAPS);
		}

		Ui.round(g, cx - CENTER / 2, cy - CENTER / 2, CENTER, CENTER, CENTER / 2, Ui.DARK);
		Ui.ring(g, cx - CENTER / 2, cy - CENTER / 2, CENTER, CENTER, CENTER / 2, Ui.LINE);
		EmoteLibrary.Emote lit = hovered < 0 ? null : EmoteLibrary.get(mod.slot(hovered));
		int textY = cy + RADIUS + SLOT / 2 + 18;
		if (lit != null) {
			Ui.centered(g, Ui.fit(lit.name(), width - 16, Ui.TITLE, false), cx, textY, Ui.TEXT, Ui.TITLE);
			if (!lit.author().isEmpty()) Ui.centered(g, I18n.get("newgen_client.emotes.by", lit.author()), cx, textY + 13, Ui.DIM, Ui.UI);
		} else {
			// On a glass strip, like the hub's hint, so it reads over any scenery.
			String hint = Ui.fit(Ui.tr("emotes.hint"), width - 16, Ui.UI, false);
			int w = Ui.width(hint, Ui.UI) + 12;
			Ui.round(g, cx - w / 2, textY - 3, w, 13, Ui.R_SM, Ui.GLASS);
			Ui.centered(g, hint, cx, textY, Ui.SOFT, Ui.UI);
		}
	}

	@Override
	public void tick() {
		if (!mod.keyHeld(minecraft)) {
			choose();
			return;
		}
		if (Ui.reduceMotion) return; // figures hold their key pose
		for (int i = 0; i < figures.length; i++) {
			if (figures[i] == null) continue;
			step(figures[i]);
			PlayerAnimationController c = controller(figures[i]);
			if (c != null && !c.isActive() && ++idle[i] > REST_TICKS) { // a one-shot emote restarts after a short rest
				idle[i] = 0;
				EmoteLibrary.Emote emote = EmoteLibrary.get(mod.slot(i));
				if (emote != null) c.triggerAnimation(emote.animation());
			}
		}
	}

	private void makeFigures() {
		if (minecraft.level == null || minecraft.player == null) return;
		for (int i = 0; i < figures.length; i++) {
			EmoteLibrary.Emote emote = EmoteLibrary.get(mod.slot(i));
			if (emote == null) continue;
			try {
				RemotePlayer figure = new RemotePlayer(minecraft.level, minecraft.player.getGameProfile());
				PlayerAnimationController c = controller(figure);
				if (c == null) continue;
				c.triggerAnimation(emote.animation());
				figures[i] = figure;
				// The key pose for Reduce motion: a short way into the emote, then frozen.
				if (Ui.reduceMotion) for (int t = 0; t < KEY_POSE_TICKS; t++) step(figure);
			} catch (Throwable e) {
				figures[i] = null; // that slot shows its icon
			}
		}
	}

	/** One animation tick of a stand-in, as Player Animation Library does for players in the world. */
	private static void step(RemotePlayer figure) {
		figure.tickCount++;
		PlayerAnimationAccess.getPlayerAnimManager(figure).handleAnimations(0f, true, false);
	}

	private static PlayerAnimationController controller(RemotePlayer figure) {
		return PlayerAnimationAccess.getPlayerAnimationLayer(figure, EmotesMod.LAYER) instanceof PlayerAnimationController c ? c : null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) choose();
		return true;
	}

	private void stopPreview() {
		if (preview != null) minecraft.getSoundManager().stop(preview);
		preview = null;
	}

	@Override
	public void removed() {
		stopPreview();
	}

	private void choose() {
		stopPreview();
		EmoteLibrary.Emote emote = hovered < 0 ? null : EmoteLibrary.get(mod.slot(hovered));
		onClose();
		if (emote != null) mod.play(minecraft, emote);
	}

	/** Keyboard: 1-8 play that slot at once; arrows move round the wheel (left/up back, right/down on); Enter plays. */
	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		int key = event.key();
		if (key >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && key <= org.lwjgl.glfw.GLFW.GLFW_KEY_8) {
			hovered = key - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
			choose();
			return true;
		}
		int step = Ui.arrow(key);
		if (step != 0) {
			hovered = Math.floorMod((hovered < 0 ? (step > 0 ? -1 : 0) : hovered) + step, EmotesMod.SLOTS);
			lastMouseX = Double.NaN; // the mouse takes over again only once it moves
			return true;
		}
		if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
			choose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
