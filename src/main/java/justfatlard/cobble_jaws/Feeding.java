package justfatlard.cobble_jaws;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * What a kill means on either side of the teeth.
 *
 * <p>Prey a shark kills is eaten: the shark is fed, and nothing is left floating, because a cod that
 * a shark has eaten is not a cod anyone can pick up afterwards. A shark somebody else kills is meat
 * like any big fish, cut into whitefish fillets the way Minedew Fishing names cod - which is what
 * vanilla already drops for a dolphin, only more of it.
 */
public final class Feeding {
	private Feeding() {}

	/** Health a meal puts back. Enough to matter in a fight that started over a fish. */
	private static final float MEAL = 4.0F;

	/** Raw cod per unit of a shark's size, on top of the dolphin's own drop. Pups give nothing. */
	private static final float COD_PER_SCALE = 2.0F;

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(Feeding::afterDeath);
		// A dolphin fed a fish goes looking for treasure to show you. A shark is not that animal.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
			Sharks.is(entity) && player.getItemInHand(hand).is(ItemTags.FISHES)
				? InteractionResult.FAIL : InteractionResult.PASS);
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			Entity dead = context.getOptional(LootContextParams.THIS_ENTITY);
			Entity killer = context.getOptional(LootContextParams.ATTACKING_ENTITY);
			if (Sharks.is(killer) && dead instanceof LivingEntity prey && Instincts.isPrey(prey)) {
				drops.clear();
			}
		});
	}

	private static void afterDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) return;

		if (Sharks.is(source.getEntity()) && Instincts.isPrey(entity)) {
			Dolphin shark = (Dolphin) source.getEntity();
			shark.heal(MEAL);
			level.playSound(null, shark.blockPosition(), SoundEvents.DOLPHIN_EAT, SoundSource.HOSTILE,
				1.0F, 0.7F);
			return;
		}

		if (entity instanceof Dolphin shark && Sharks.is(shark) && !shark.isBaby()) {
			int cod = Math.max(1, Math.round(COD_PER_SCALE * shark.getScale()));
			shark.spawnAtLocation(level, new ItemStack(Items.COD, cod));
		}
	}
}
