package justfatlard.cobble_jaws;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Prey a shark kills is eaten: the shark is fed, and nothing is left floating, because a cod that a
 * shark has eaten is not a cod anyone can pick up afterwards.
 */
public final class Feeding {
	private Feeding() {}

	/** Health a meal puts back. Enough to matter in a fight that started over a fish. */
	private static final float MEAL = 4.0F;

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof Shark shark) || !Shark.isPrey(entity)) return;
			if (!(entity.level() instanceof ServerLevel level)) return;
			shark.heal(MEAL);
			level.playSound(null, shark.blockPosition(), SoundEvents.DOLPHIN_EAT, SoundSource.HOSTILE, 1.0F, 0.7F);
		});
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			Entity dead = context.getOptional(LootContextParams.THIS_ENTITY);
			if (context.getOptional(LootContextParams.ATTACKING_ENTITY) instanceof Shark
				&& dead instanceof LivingEntity prey && Shark.isPrey(prey)) {
				drops.clear();
			}
		});
	}
}
