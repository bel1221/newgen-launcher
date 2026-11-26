package net.newgen.client.mixin.utility;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.network.chat.Component;
import net.newgen.client.mods.utility.PackOrganizerMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pack Organizer: our own search filter, favourites first among the available packs, and a favourite button. */
@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen implements PackOrganizerMod.Refresh {
	@Shadow
	private TransferableSelectionList availablePackList;
	@Shadow
	private TransferableSelectionList selectedPackList;
	@Shadow
	private EditBox search;

	@Shadow
	private void updateFilteredEntries(String query) {
		throw new AssertionError();
	}

	protected PackSelectionScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "filterEntries(Ljava/lang/String;Ljava/util/stream/Stream;Lnet/minecraft/client/gui/screens/packs/TransferableSelectionList;)V",
			at = @At("HEAD"), cancellable = true)
	private void newgen$filter(String query, Stream<PackSelectionModel.Entry> entries, TransferableSelectionList list, CallbackInfo ci) {
		PackOrganizerMod mod = PackOrganizerMod.I;
		if (!PackOrganizerMod.active() || list == null) return;
		ci.cancel();
		boolean available = list == availablePackList;
		Stream<PackSelectionModel.Entry> out = entries.filter(e -> PackOrganizerMod.matches(query, e.getId(), e.getTitle().getString(),
				mod.searchDescriptions.value ? e.getDescription().getString() : ""));
		if (available) {
			if (mod.favouritesOnly.value) out = out.filter(e -> mod.isFavourite(e.getId()));
			if (mod.favouritesFirst.value) {
				List<PackSelectionModel.Entry> sorted = out.sorted(Comparator.comparing(e -> !mod.isFavourite(e.getId()))).toList();
				out = sorted.stream();
			}
		}
		list.updateList(out, null);
	}

	@Inject(method = "init()V", at = @At("TAIL"))
	private void newgen$favouriteButton(CallbackInfo ci) {
		if (!PackOrganizerMod.active()) return;
		addRenderableWidget(Button.builder(Component.literal("★"), b -> {
			TransferableSelectionList.Entry picked = availablePackList != null ? availablePackList.getSelected() : null;
			if (picked == null && selectedPackList != null) picked = selectedPackList.getSelected();
			if (picked == null) return;
			PackOrganizerMod.I.toggleFavourite(picked.getPackId());
			newgen$refreshPacks();
		}).bounds(width - 26, 6, 20, 20).tooltip(Tooltip.create(Component.translatable("newgen_client.pack_organizer.favourite_tip"))).build());
	}

	@Inject(method = "tick()V", at = @At("HEAD"))
	private void newgen$resort(CallbackInfo ci) {
		if (PackOrganizerMod.active() && PackOrganizerMod.I.dirty) {
			PackOrganizerMod.I.dirty = false;
			newgen$refreshPacks();
		}
	}

	@Override
	public void newgen$refreshPacks() {
		if (search != null) updateFilteredEntries(search.getValue());
	}
}
