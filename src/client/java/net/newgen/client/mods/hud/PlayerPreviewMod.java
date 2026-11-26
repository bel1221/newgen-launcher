package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Pose;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

/** Player Preview: your own player model on the HUD, drawn the way the inventory screen draws it. */
public class PlayerPreviewMod extends HudMod {
	private final Setting.Choice rotation = radio("preview_rotation", "Rotation", "Follow", "Follow", "Fixed", "Spin");
	private final Setting.Num yaw = num("preview_yaw", "Turn (degrees)", 25, -180, 180, 5);
	private final Setting.Num spinSpeed = num("spin_speed", "Spin speed (degrees/s)", 45, 5, 360, 5);
	private final Setting.Num modelSize = num("model_size", "Model size", 30, 10, 100, 1);

	public PlayerPreviewMod() {
		super("player_preview", "Player Preview", "Your own player model on the screen", 0.02f, 0.6f);
		background.value = false;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		if (mc.player == null) return null;
		double s = modelSize.get();
		return new int[] {(int) Math.round(s * 1.4), (int) Math.round(s * 2.3)};
	}

	/** The body's turn in degrees (0 = facing the viewer) for the mode at time ms. */
	static float bodyTurn(String mode, double yaw, double spinPerSecond, long ms) {
		if (mode.equals("Spin")) return (float) ((ms % 3_600_000L) / 1000.0 * spinPerSecond % 360);
		return (float) yaw;
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) return;
		int[] box = size(mc);
		// The model is drawn into its own picture by screen coordinates, so turn the element's corners into those.
		Vector2f a = g.pose().transformPosition(0, 0, new Vector2f());
		Vector2f b = g.pose().transformPosition(box[0], box[1], new Vector2f());
		float hudScale = (b.x - a.x) / box[0];

		EntityRenderer<? super LocalPlayer, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
		EntityRenderState state = renderer.createRenderState(player, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
		state.lightCoords = 0xF000F0;
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180f + bodyTurn(rotation.value, yaw.get(), spinSpeed.get(), System.currentTimeMillis());
			if (!rotation.value.equals("Follow") || living.pose == Pose.FALL_FLYING) {
				living.yRot = 0;
				living.xRot = 0;
			}
			living.boundingBoxWidth /= living.scale;
			living.boundingBoxHeight /= living.scale;
			living.scale = 1f;
		}
		Quaternionf flip = new Quaternionf().rotateZ((float) Math.PI);
		Vector3f offset = new Vector3f(0, state.boundingBoxHeight / 2f + 0.0625f, 0);
		g.submitEntityRenderState(state, (float) modelSize.get() * hudScale, offset, flip, new Quaternionf(),
				Math.round(a.x), Math.round(a.y), Math.round(b.x), Math.round(b.y));
	}
}
