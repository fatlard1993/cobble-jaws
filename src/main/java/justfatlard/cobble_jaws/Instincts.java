package justfatlard.cobble_jaws;

import justfatlard.cobble_jaws.mixin.MobAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.player.Player;

/**
 * What a shark does that a dolphin does not: hunt, and guard its pups.
 *
 * <p>Everything else it keeps. A dolphin already leaves people alone and already turns on whatever
 * hits it, calling the rest of its group in, which is the temper wanted: passive unless provoked.
 * The two things added are the polar bear's other rule - come near a cub and the mother comes for
 * you - and an appetite.
 *
 * <p>Only added goals, never removed ones: adding is safe against a version that has rearranged
 * what a dolphin does. Goals are not saved, so this runs on every load, and checks for its own
 * goal classes rather than a tag so a reload cannot leave a shark without them.
 */
public final class Instincts {
	private Instincts() {}

	/** How close you can come to a pup before its mother takes it personally, in blocks. */
	private static final double GUARD_RANGE = 8.0;

	/**
	 * Ticks between looks for something to eat, on average. Often enough that a shark is seen
	 * hunting; rarely enough that a warm ocean still has fish in it.
	 */
	private static final int HUNT_INTERVAL = 200;

	/** Anything wider than this is not prey, it is a fight. */
	private static final float PREY_WIDTH = 0.7F;

	/**
	 * Faster than a dolphin's own lunge, and ahead of its idle swimming. The dolphin's own attack
	 * goal sits behind its random swimming, which is why a provoked dolphin is so slow to follow up.
	 */
	private static final double LUNGE_SPEED = 1.4;

	public static void install(Dolphin shark) {
		if (shark.getGoalSelector().getAvailableGoals().stream()
			.anyMatch(wrapped -> wrapped.getGoal() instanceof Lunge)) return;

		shark.getGoalSelector().addGoal(1, new Lunge(shark));
		var targets = ((MobAccessor) shark).cobbleJaws$targetSelector();
		targets.addGoal(2, new GuardPups(shark));
		targets.addGoal(3, new Hunt(shark));
	}

	/**
	 * What a shark eats: fish, squid, and anything small that is in the water with it.
	 *
	 * <p>Not a pufferfish, which no predator takes twice, and not anybody's pet. Only in the water,
	 * because a shark cannot follow anything out of it and would otherwise stare at a chicken on the
	 * bank forever.
	 */
	public static boolean isPrey(LivingEntity target) {
		if (!target.isInWater()) return false;
		if (target instanceof Pufferfish || target instanceof Dolphin) return false;
		if (target instanceof OwnableEntity pet && pet.getOwnerReference() != null) return false;
		if (target instanceof AbstractFish || target instanceof Squid) return true;
		return target instanceof Animal && target.getBbWidth() <= PREY_WIDTH;
	}

	/** Whether a pup of this shark's kind is near enough that the shark is standing guard. */
	private static boolean guarding(Dolphin shark, ServerLevel level) {
		if (shark.isBaby()) return false;
		return !level.getEntitiesOfClass(Dolphin.class,
			shark.getBoundingBox().inflate(GUARD_RANGE),
			other -> other.isBaby() && Sharks.is(other)).isEmpty();
	}

	private static final class Lunge extends MeleeAttackGoal {
		Lunge(Dolphin shark) {
			super(shark, LUNGE_SPEED, true);
		}
	}

	private static final class GuardPups extends NearestAttackableTargetGoal<Player> {
		GuardPups(Dolphin shark) {
			super(shark, Player.class, 10, true, false, (target, level) ->
				target.distanceToSqr(shark) < GUARD_RANGE * GUARD_RANGE && guarding(shark, level));
		}
	}

	private static final class Hunt extends NearestAttackableTargetGoal<LivingEntity> {
		Hunt(Dolphin shark) {
			super(shark, LivingEntity.class, HUNT_INTERVAL, true, false,
				(target, level) -> isPrey(target));
		}
	}
}
