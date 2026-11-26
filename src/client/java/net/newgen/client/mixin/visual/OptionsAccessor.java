package net.newgen.client.mixin.visual;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads and replaces Options' key mapping list (final in the game), so one key can be left out of Controls. */
@Mixin(Options.class)
public interface OptionsAccessor {
	@Accessor("keyMappings")
	KeyMapping[] newgen$keyMappings();

	@Mutable
	@Accessor("keyMappings")
	void newgen$setKeyMappings(KeyMapping[] mappings);
}
