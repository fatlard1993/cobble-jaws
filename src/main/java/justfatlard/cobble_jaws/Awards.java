package justfatlard.cobble_jaws;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/** The advancement this mod hands out, by the code-awarded {@code impossible} idiom. */
public final class Awards {
	private Awards() {}

	/** A shark on the line too big to land. There is only one joke here and this is it. */
	public static void biggerBoat(@Nullable ServerPlayer player) {
		award(player, "bigger_boat");
	}

	private static void award(@Nullable ServerPlayer player, String path) {
		if (player == null || player.level().getServer() == null) return;
		AdvancementHolder holder = player.level().getServer().getAdvancements()
			.get(Identifier.fromNamespaceAndPath(Main.MOD_ID, path));
		if (holder == null) return;

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		if (progress.isDone()) return;
		for (String criterion : progress.getRemainingCriteria()) {
			player.getAdvancements().award(holder, criterion);
		}
	}
}
