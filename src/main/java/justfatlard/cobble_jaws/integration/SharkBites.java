package justfatlard.cobble_jaws.integration;

import com.minedew.fishing.api.MinedewFishingApi;
import com.minedew.fishing.fish.FishMovementPattern;
import com.minedew.fishing.fish.FishSize;
import com.minedew.fishing.fish.Quarry;
import justfatlard.cobble_jaws.Awards;
import justfatlard.cobble_jaws.Sharks;
import justfatlard.cobble_jaws.Spawns;
import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sharks on a fishing line, when Minedew Fishing is here to fight them.
 *
 * <p>A small or medium one is landed like any fish and cut into whitefish, two or three fillets the
 * way a cod is. A large one, and a trophy, is not landed at all: it comes up out of the water alive,
 * angry, and at you, and the fillets are whatever you can take off it.
 *
 * <p>Must only be touched behind a mod-loaded check: it names Minedew Fishing types directly and
 * loading it without that mod throws.
 */
public final class SharkBites implements Quarry {
	private SharkBites() {}

	private static final SharkBites SHARK = new SharkBites();

	/** Of the bites in shark water, how many are a shark. Deep water is where the big ones are. */
	private static final float OCEAN_CHANCE = 0.03F;
	private static final float DEEP_CHANCE = 0.06F;

	/** How big a shark that comes up alive is; a wild one is {@link Sharks#WILD_SCALE}. */
	private static final float LARGE_SCALE = 1.35F;
	private static final float TROPHY_SCALE = 1.7F;

	/** How hard it comes out of the water at you, in blocks per tick along the line. */
	private static final double BREACH_SPEED = 0.6;
	private static final double BREACH_LIFT = 0.45;

	public static void register() {
		MinedewFishingApi.addBiter(SharkBites::bite);
	}

	private static @Nullable Quarry bite(ServerLevel level, ServerPlayer player, FishingHook hook) {
		Holder<Biome> biome = level.getBiome(hook.blockPosition());
		if (!Spawns.sharkWater(biome)) return null;
		float chance = biome.is(BiomeTags.IS_DEEP_OCEAN) ? DEEP_CHANCE : OCEAN_CHANCE;
		return level.getRandom().nextFloat() < chance ? SHARK : null;
	}

	@Override
	public String getDisplayName() {
		return "Shark";
	}

	// Hard lunges, then every so often a frenzy. Run through Minedew Fishing's tune_sim.py against
	// the vanilla four it sits between cod and salmon at every size, for a good player and a sloppy
	// one alike, and a parked bobber lands almost none of them: a different fight, not a harder one.
	@Override
	public float getBaseSpeed() {
		return 1.00F;
	}

	@Override
	public float getBaseAggressiveness() {
		return 0.95F;
	}

	@Override
	public FishMovementPattern getPattern() {
		return FishMovementPattern.FAST_DART;
	}

	@Override
	public FishMovementPattern getAccent() {
		return FishMovementPattern.FAST_ERRATIC;
	}

	@Override
	public int getAccentPeriodTicks() {
		return 140;
	}

	@Override
	public int getAccentDurationTicks() {
		return 30;
	}

	/** Raw cod, which Minedew Fishing names a whitefish fillet. */
	@Override
	public Item getBonusItem() {
		return Items.COD;
	}

	@Override
	public EntityType<?> getDisplayEntity() {
		return EntityTypes.DOLPHIN;
	}

	@Override
	public boolean isJunk() {
		return false;
	}

	/** The fish that flies in is a dolphin in a shark's skin, and never tagged: it is theatre. */
	@Override
	public void dressDisplay(Mob shown, FishSize size) {
		PandoricalApi.entityOverlays().set(shown, Sharks.skin(false));
	}

	@Override
	public boolean landAlive(ServerLevel level, ServerPlayer player, Vec3 origin, FishSize size) {
		float scale = switch (size) {
			case LARGE -> LARGE_SCALE;
			case TROPHY -> TROPHY_SCALE;
			default -> 0F;
		};
		if (scale == 0F) return false;

		Dolphin shark = EntityTypes.DOLPHIN.create(level, EntitySpawnReason.TRIGGERED);
		if (shark == null) return false;
		shark.setPos(origin.x, origin.y, origin.z);
		Sharks.become(shark, scale);
		// It was dragged up on a hook, so it is not going to wander off and despawn mid-fight.
		shark.setPersistenceRequired();
		if (!level.addFreshEntity(shark)) return false;

		// Provoked: the hook counts as the first blow, so it answers it like one.
		shark.setLastHurtByMob(player);
		shark.setTarget(player);

		Vec3 toward = player.position().subtract(origin);
		Vec3 flat = new Vec3(toward.x, 0, toward.z);
		if (flat.lengthSqr() > 1.0E-4) {
			shark.setDeltaMovement(flat.normalize().scale(BREACH_SPEED).add(0, BREACH_LIFT, 0));
			shark.setYRot((float) Math.toDegrees(Math.atan2(-flat.x, flat.z)));
		}
		level.sendParticles(ParticleTypes.SPLASH, origin.x, origin.y, origin.z, 40, 0.8, 0.2, 0.8, 0.2);
		level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED,
			SoundSource.HOSTILE, 1.0F, 0.7F);
		level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.DOLPHIN_ATTACK,
			SoundSource.HOSTILE, 1.0F, 0.6F);

		Awards.biggerBoat(player);
		return true;
	}
}
