package justfatlard.cobble_jaws;

import java.util.Map;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Putting sharks in the sea without taking dolphins out of it.
 *
 * <p>A shark is a dolphin, so there is no shark spawn to add: there is one more dolphin spawn in
 * each ocean dolphins live in, and a share of the pods that arrive turn out to be sharks. The share
 * is that one extra entry's part of the whole dolphin weight, so as many dolphins come as before,
 * and the sharks are on top: half as common as dolphins in the warm seas, as common in the open
 * ocean.
 *
 * <p>A pod is decided once, as a pod. The game hands every animal in a spawn group the same group
 * data object, so the first to arrive rolls and the rest follow it; a pod of three dolphins and a
 * shark would be a meal, not a pod.
 */
public final class Spawns {
	private Spawns() {}

	private static final int SHARK_WEIGHT = 1;
	private static final int MIN_GROUP = 1;
	private static final int MAX_GROUP = 2;

	/** Vanilla's own dolphin weight in each ocean that has dolphins, read off the 26.3 biomes. */
	private static final Map<ResourceKey<Biome>, Integer> DOLPHIN_WEIGHT = Map.of(
		Biomes.WARM_OCEAN, 2,
		Biomes.LUKEWARM_OCEAN, 2,
		Biomes.DEEP_LUKEWARM_OCEAN, 2,
		Biomes.OCEAN, 1,
		Biomes.DEEP_OCEAN, 1);

	/** Which pods have rolled, by the group data they share. Weak, so a finished spawn is let go. */
	private static final Map<SpawnGroupData, Boolean> PODS = new WeakHashMap<>();

	public static void register() {
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(DOLPHIN_WEIGHT.keySet()),
			MobCategory.WATER_CREATURE, EntityTypes.DOLPHIN, SHARK_WEIGHT, MIN_GROUP, MAX_GROUP);
	}

	/**
	 * Called as each dolphin finishes spawning. Only for the world's own spawns: an egg hatches the
	 * dolphin its name promises, and a shark is summoned by tag (see {@link Sharks}).
	 */
	public static void onSpawn(Dolphin dolphin, Holder<Biome> biome, EntitySpawnReason reason,
			SpawnGroupData pod) {
		if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) return;
		if (Sharks.is(dolphin)) return;

		boolean sharks = PODS.computeIfAbsent(pod, ignored -> roll(biome, dolphin.getRandom()));
		if (sharks) Sharks.become(dolphin, Sharks.WILD_SCALE);
	}

	/**
	 * Whether sharks live here. Fishing asks the same question, so a shark never takes a bait in
	 * water no shark swims in.
	 */
	public static boolean sharkWater(Holder<Biome> biome) {
		for (ResourceKey<Biome> ocean : DOLPHIN_WEIGHT.keySet()) {
			if (biome.is(ocean)) return true;
		}
		return false;
	}

	private static boolean roll(Holder<Biome> biome, RandomSource random) {
		for (Map.Entry<ResourceKey<Biome>, Integer> ocean : DOLPHIN_WEIGHT.entrySet()) {
			if (biome.is(ocean.getKey())) {
				return random.nextInt(ocean.getValue() + SHARK_WEIGHT) < SHARK_WEIGHT;
			}
		}
		return false;
	}
}
