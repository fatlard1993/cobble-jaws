package justfatlard.cobble_jaws;

import justfatlard.pandorical.api.EntityRegistration;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The shark as the game knows it: an entity type, its spawn egg, and how Pandorical draws it.
 *
 * <p>Pandorical clients draw it as the dolphin it stands on, in a shark's skin; vanilla clients are
 * shown the dolphin itself, because a shark nobody can see biting you is worse than a dolphin that
 * does. It is a dolphin's size because the client sizes the stand-in as its base: a shark's extra
 * bulk is its scale attribute, which every client reads.
 */
public final class Sharks {
	private Sharks() {}

	private static final Identifier ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "shark");

	public static final EntityType<Shark> SHARK = FabricEntityType.Builder.createMob(Shark::new,
			MobCategory.WATER_CREATURE, mob -> mob
				.spawnPlacement(SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					AgeableWaterCreature::checkSurfaceAgeableWaterCreatureSpawnRules)
				.defaultAttributes(Shark::createAttributes))
		.sized(EntityTypes.DOLPHIN.getWidth(), EntityTypes.DOLPHIN.getHeight())
		.eyeHeight(EntityTypes.DOLPHIN.getDimensions().eyeHeight())
		.build(ResourceKey.create(Registries.ENTITY_TYPE, ID));

	private static final Identifier EGG_ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "shark_spawn_egg");

	public static final Item SPAWN_EGG = new SpawnEggItem(new Item.Properties()
		.setId(ResourceKey.create(Registries.ITEM, EGG_ID))
		.spawnEgg(SHARK));

	public static Identifier skin(boolean pup) {
		return Identifier.fromNamespaceAndPath(Main.MOD_ID,
			pup ? "textures/entity/shark_pup.png" : "textures/entity/shark.png");
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ID, SHARK);
		Registry.register(BuiltInRegistries.ITEM, EGG_ID, SPAWN_EGG);

		PandoricalApi.content().registerEntity(SHARK, new EntityRegistration()
			.base(EntityTypes.DOLPHIN)
			.texture(skin(false))
			.babyTexture(skin(true))
			.vanillaFallback());
		PandoricalApi.content().registerItem(EGG_ID.toString(), new ItemRegistration());

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> output.accept(SPAWN_EGG));
	}
}
