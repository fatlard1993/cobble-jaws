package justfatlard.cobble_jaws;

import java.util.Set;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Putting sharks in the seas dolphins live in, on top of the dolphins.
 *
 * <p>One spawn entry each, at weight 1 in pods of one or two: half as common as dolphins in the
 * warm seas, where a dolphin's weight is 2, and as common in the open ocean, where it is 1.
 */
public final class Spawns {
	private Spawns() {}

	private static final int WEIGHT = 1;
	private static final int MIN_GROUP = 1;
	private static final int MAX_GROUP = 2;

	private static final Set<ResourceKey<Biome>> SHARK_WATER = Set.of(
		Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.OCEAN, Biomes.DEEP_OCEAN);

	public static void register() {
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(SHARK_WATER),
			MobCategory.WATER_CREATURE, Sharks.SHARK, WEIGHT, MIN_GROUP, MAX_GROUP);
	}

	/**
	 * Whether sharks live here. Fishing asks the same question, so a shark never takes a bait in
	 * water no shark swims in.
	 */
	public static boolean sharkWater(Holder<Biome> biome) {
		for (ResourceKey<Biome> ocean : SHARK_WATER) {
			if (biome.is(ocean)) return true;
		}
		return false;
	}
}
