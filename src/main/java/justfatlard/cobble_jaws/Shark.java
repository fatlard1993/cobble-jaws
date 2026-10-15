package justfatlard.cobble_jaws;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FollowPlayerRiddenEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindLiquidGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A shark: passive unless provoked, and a hunter.
 *
 * <p>Its own animal, which Pandorical clients build from its model and nothing else. It swims the
 * way a dolphin does - the same steering and turning, the same pull through the water - because
 * that is the game's best swimmer, but it is not one: it breathes water, never surfaces, takes no
 * fish from your hand and leads you to no treasure.
 *
 * <p>Left alone, it leaves you alone. Hit it and it comes for you, with the rest of its pod; come
 * near a pup and its mother comes for you, as a polar bear does over its cub. And it hunts.
 */
public class Shark extends AgeableWaterCreature {
	/** A wild shark's size against a dolphin's. Size is scale, never the hitbox. */
	public static final float WILD_SCALE = 1.15F;

	/**
	 * Per unit of scale. A dolphin is 10 health and 3 damage; a shark is twice as hard to kill and
	 * bites harder, and a bigger one more so, because a large shark that was only visually larger
	 * would be a lie the first time one fought back.
	 */
	private static final double HEALTH_PER_SCALE = 20.0;
	private static final double DAMAGE_PER_SCALE = 4.0;

	/** As fast as a dolphin, the game's quickest swimmer. */
	private static final double SWIM_SPEED = 1.2;

	/** How close you can come to a pup before its mother takes it personally, in blocks. */
	private static final double GUARD_RANGE = 8.0;

	/**
	 * Ticks between looks for something to eat, on average. Often enough that a shark is seen
	 * hunting; rarely enough that a warm ocean still has fish in it.
	 */
	private static final int HUNT_INTERVAL = 200;

	/** Anything wider than this is not prey, it is a fight. */
	private static final float PREY_WIDTH = 0.7F;

	/** Ahead of its idle swimming, so a provoked shark follows up at once. */
	private static final double LUNGE_SPEED = 1.4;

	/** A pup against its mother: a little bigger than half, like a dolphin calf. */
	private static final float PUP_SCALE = 0.65F;

	public Shark(EntityType<? extends Shark> type, Level level) {
		super(type, level);
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
		this.lookControl = new SmoothSwimmingLookControl(this, 10);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, SWIM_SPEED)
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
		this.goalSelector.addGoal(0, new TryFindLiquidGoal(this, FluidTags.WATER));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, LUNGE_SPEED, true));
		this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
		// Circling under a boat is a shark's oldest trick.
		this.goalSelector.addGoal(8, new FollowPlayerRiddenEntityGoal(this, AbstractBoat.class));
		this.goalSelector.addGoal(9, new AvoidEntityGoal<>(this, Guardian.class, 8.0F, 1.0, 1.0));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Guardian.class).setAlertOthers());
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
		if (target instanceof Pufferfish || target instanceof Dolphin || target instanceof Shark) return false;
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

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
		this.setXRot(0F);
		// Now and then one of a pod is a pup, as with dolphins.
		return super.finalizeSpawn(level, difficulty, spawnReason,
			groupData != null ? groupData : new AgeableMob.AgeableMobGroupData(0.1F));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	/** Through the water as a dolphin goes; sinking a little when it has nothing to chase. */
	@Override
	protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
		this.moveRelative(this.getSpeed(), input);
		this.move(MoverType.SELF, this.getDeltaMovement());
		this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
		if (this.getTarget() == null) this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
	}

	/** Out of the water it thrashes, which is how one stranded on a beach gets back in. */
	@Override
	public void tick() {
		super.tick();
		if (!this.isNoAi() && !this.isInWater() && this.onGround()) {
			this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2F - 1F) * 0.2F, 0.5,
				(this.random.nextFloat() * 2F - 1F) * 0.2F));
			this.setYRot(this.random.nextFloat() * 360F);
			this.setOnGround(false);
			this.needsSync = true;
			this.playSound(SoundEvents.SALMON_FLOP, this.getSoundVolume(), this.getVoicePitch());
		}
	}

	/** It breathes water, so it never breaks off a chase to surface. */
	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	/** A pup is too small to bite. */
	@Override
	public boolean canAttack(LivingEntity target) {
		return !this.isBaby() && super.canAttack(target);
	}

	@Override
	public float getAgeScale() {
		return this.isBaby() ? PUP_SCALE : 1F;
	}

	/** Its head turns with its whole body, as a fish's does. */
	@Override
	public int getMaxHeadXRot() {
		return 1;
	}

	@Override
	public int getMaxHeadYRot() {
		return 1;
	}

	@Override
	public @Nullable Shark getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return Sharks.SHARK.create(level, EntitySpawnReason.BREEDING);
	}

	/** Named with a fallback, so a vanilla client shown the dolphin in its place still reads Shark. */
	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback(this.getType().getDescriptionId(), "Shark");
	}

	// A shark makes no sound of its own until it is hurt. A big fish's voice, pitched down.

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SALMON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SALMON_DEATH;
	}

	@Override
	protected SoundEvent getSwimSound() {
		return SoundEvents.DOLPHIN_SWIM;
	}

	@Override
	protected SoundEvent getSwimSplashSound() {
		return SoundEvents.DOLPHIN_SPLASH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.6F;
	}
}
