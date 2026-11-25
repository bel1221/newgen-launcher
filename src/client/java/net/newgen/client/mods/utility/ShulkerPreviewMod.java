package net.newgen.client.mods.utility;

import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/** Shows what is inside a shulker box (or any item holding a container) in its tooltip. Hook: ItemStackMixin. */
public class ShulkerPreviewMod extends Mod {
	public static ShulkerPreviewMod INSTANCE;
	private static final int COLUMNS = 9, ROWS = 3, SLOT = 18;

	private final Setting.Bool needShift = bool("need_shift", "Only while holding Shift", false);
	private final Setting.Color slotColor = color("slot_color", "Slot colour", 0x60FFFFFF);

	/** The 27 slots of the box, in order. */
	public record Preview(NonNullList<ItemStack> items) implements TooltipComponent {
	}

	public ShulkerPreviewMod() {
		super("shulker_preview", "Shulker Preview", "See the contents of shulker boxes in their tooltip");
		INSTANCE = this;
		TooltipComponentCallback.EVENT.register(data -> data instanceof Preview p ? new Grid(p, this) : null);
	}

	public static Optional<TooltipComponent> preview(ItemStack stack) {
		ShulkerPreviewMod m = INSTANCE;
		if (m == null || !m.enabled) return Optional.empty();
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null || !contents.nonEmptyItems().iterator().hasNext()) return Optional.empty();
		if (m.needShift.value) {
			Minecraft mc = Minecraft.getInstance();
			if (!InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
					&& !InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) return Optional.empty();
		}
		NonNullList<ItemStack> items = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
		contents.copyInto(items);
		return Optional.of(new Preview(items));
	}

	private record Grid(Preview preview, ShulkerPreviewMod mod) implements ClientTooltipComponent {
		@Override
		public int getHeight(Font font) {
			return ROWS * SLOT + 4;
		}

		@Override
		public int getWidth(Font font) {
			return COLUMNS * SLOT;
		}

		@Override
		public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics g) {
			for (int i = 0; i < preview.items().size(); i++) {
				int sx = x + (i % COLUMNS) * SLOT, sy = y + (i / COLUMNS) * SLOT;
				g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, mod.slotColor.value);
				ItemStack stack = preview.items().get(i);
				if (stack.isEmpty()) continue;
				g.renderItem(stack, sx + 1, sy + 1);
				g.renderItemDecorations(font, stack, sx + 1, sy + 1);
			}
		}
	}
}
