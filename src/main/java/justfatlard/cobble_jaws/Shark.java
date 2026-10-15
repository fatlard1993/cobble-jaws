package justfatlard.cobble_jaws;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A shark: a dolphin's body with none of a dolphin's habits.
 *
 * <p>Built on the dolphin because it is the animal built like a shark already - a long body, a fin
 * on its back, a tail it drives itself with - and because Pandorical clients draw a registered
 * entity as the vanilla one it stands on. That is also why this class adds no synced data: the
 * client reads the shark's data by the dolphin's layout.
 *
 * <p>From the dolphin it keeps the swimming, the leaping, following boats and turning on whatever
 * hits it with the rest of its pod, which is the temper wanted: passive unless provoked. It drops
 * every goal the dolphin itself defines - leading you to treasure, giving you Dolphin's Grace,
 * playing with items - and adds two of the polar bear's rules instead: come near a pup and its
 * mother comes for you, and it hunts.
 */
public class Shark extends Dolphin {
	/** A wild shark's size against a dolphin, which is 1. Size is scale, never the hitbox. */
	public static final float WILD_SCALE = 1.15F;

	/**
	 * Per unit of scale. A dolphin is 10 health and 3 damage; a shark is twice as hard to kill and
	 * bites harder, and a bigger one more so, because a large shark that was only visually larger
	 * would be a lie the first time one fought back.
	 */
	private static final double HEALTH_PER_SCALE = 20.0;
	private static final double DAMAGE_PER_SCALE = 4.0;

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

	public Shark(EntityType<? extends Shark> type, Level level) {
		super(type, level);
		// A dolphin carrying a stick about is a dolphin.
		this.setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Dolphin.createAttributes()
			.add(Attributes.SCALE, WILD_SCALE)
			.add(Attributes.MAX_HEALTH, HEALTH_PER_SCALE * WILD_SCALE)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE_PER_SCALE * WILD_SCALE);
	}

	/** Grow it to a size other than the wild one: health and bite go with it. */
	public void grow(float scale) {
		base(Attributes.SCALE, scale);
		base(Attributes.MAX_HEALTH, HEALTH_PER_SCALE * scale);
		base(Attributes.ATTACK_DAMAGE, DAMAGE_PER_SCALE * scale);
		this.setHealth(this.getMaxHealth());
	}

	private void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			double value) {
		AttributeInstance instance = this.getAttribute(attribute);
		if (instance != null) instance.setBaseValue(value);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// Every goal the dolphin class defines for itself is a dolphin habit; the ones it borrows
		// from the game's shared goals are the body, and stay.
		this.goalSelector.removeAllGoals(goal -> goal.getClass().getEnclosingClass() == Dolphin.class);
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, LUNGE_SPEED, true));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
			(target, level) -> target.distanceToSqr(this) < GUARD_RANGE * GUARD_RANGE && guarding(level)));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, HUNT_INTERVAL,
			true, false, (target, level) -> isPrey(target)));
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

	/** Whether one of its own pups is near enough that it is standing guard. */
	private boolean guarding(ServerLevel level) {
		if (this.isBaby()) return false;
		return !level.getEntitiesOfClass(Shark.class, this.getBoundingBox().inflate(GUARD_RANGE),
			Shark::isBaby).isEmpty();
	}

	/** It breathes water, so it never breaks off a chase to surface. */
	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	/** A dolphin fed a fish goes looking for treasure to show you. A shark takes no fish from your hand. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).is(ItemTags.FISHES)) return InteractionResult.PASS;
		return super.mobInteract(player, hand);
	}

	@Override
	public @Nullable Dolphin getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return Sharks.SHARK.create(level, EntitySpawnReason.BREEDING);
	}

	/** Named with a fallback, so a vanilla client shown the dolphin underneath still reads Shark. */
	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback(this.getType().getDescriptionId(), "Shark");
	}
}
