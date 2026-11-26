package net.newgen.client.mods.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** A player's latency, as the tab list knows it, next to their name tag. Hook: EntityRendererPingMixin. */
public class PingNametagsMod extends Mod {
	public static PingNametagsMod I;

	private final Setting.Choice format = choice("ping_format", "Ping format", "[42ms]", "[42ms]", "42ms", "42");
	private final Setting.Choice side = choice("ping_side", "Ping position", "After name", "After name", "Before name");
	private final Setting.Bool colour = bool("ping_colour", "Colour by ping", true);
	private final Setting.Color plain = color("ping_text_colour", "Ping colour (when not by ping)", 0xFFAAAAAA);

	public PingNametagsMod() {
		super("ping_nametags", "Ping on Nametags", "Shows each player's ping (from the tab list) next to their name tag");
		I = this;
	}

	/** The name tag with the ping added, or the original when off, not a player, or the ping is unknown. */
	public static Component decorate(Component original, Entity entity) {
		PingNametagsMod m = I;
		if (m == null || !m.enabled || original == null || !(entity instanceof Player player)) return original;
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) return original;
		PlayerInfo info = connection.getPlayerInfo(player.getUUID());
		if (info == null || info.getLatency() <= 0) return original;
		int ms = info.getLatency();
		MutableComponent ping = Component.literal(text(ms, m.format.value))
				.withColor((m.colour.value ? colour(ms) : m.plain.current()) & 0xFFFFFF);
		return m.side.value.equals("Before name")
				? Component.empty().append(ping).append(" ").append(original)
				: Component.empty().append(original).append(" ").append(ping);
	}

	static String text(int ms, String format) {
		return switch (format) {
			case "42ms" -> ms + "ms";
			case "42" -> String.valueOf(ms);
			default -> "[" + ms + "ms]";
		};
	}

	/** Green when low, through yellow and orange, to red when high (0xRRGGBB). */
	static int colour(int ms) {
		if (ms < 50) return 0x55FF55;
		if (ms < 100) return 0xA8E05F;
		if (ms < 150) return 0xFFFF55;
		if (ms < 250) return 0xFFAA00;
		return 0xFF5555;
	}
}
