package net.newgen.client.mixin.visual;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Nametag: the glyph source of a font, for its plain rectangle (used to draw rounded backgrounds). */
@Mixin(Font.class)
public interface FontAccessor {
	@Accessor("provider")
	Font.Provider newgen$provider();
}
