package justfatlard.cobble_jaws.gametest;

import com.minedew.fishing.fish.FishSize;
import com.minedew.fishing.fish.Quarry;
import justfatlard.cobble_jaws.Sharks;
import justfatlard.cobble_jaws.integration.SharkBites;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * What the mod promises, in a glass tank in the sky: a shark ignores you until you come near its
 * pup, hunts and eats a cod without leaving it floating, breathes water, swims with nobody, is
 * called a Shark, and the big one on a fishing line comes up alive and after you.
 */
public final class SharkBehaviour implements FabricClientGameTest {

	private static final int TANK = 10;
	private static final int DEPTH = 5;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("gamemode survival @a");
			server.runCommand("gamerule spawn_mobs false");
			BlockPos tank = server.computeOnServer(s ->
				connection.getServerPlayer().blockPosition().above(40));
			server.runOnServer(s -> fill(s.overworld(), tank));

			// A shark summoned by tag is a whole shark: sized, named, breathing water.
			UUID shark = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				Dolphin made = spawn(level, tank.offset(3, 2, 3), false);
				check(made.getScale() > 1.1F, "a tagged dolphin was never sized: " + made.getScale());
				check(made.getMaxHealth() > 20F, "a tagged dolphin was never toughened: " + made.getMaxHealth());
				check("Shark".equals(made.getName().getString()), "a shark is called " + made.getName().getString());
				check(made.canBreatheUnderwater(), "a shark has to come up for air");
				Dolphin dolphin = EntityTypes.DOLPHIN.create(level, EntitySpawnReason.COMMAND);
				check(!dolphin.canBreatheUnderwater(), "plain dolphins stopped needing air");
				check(Sharks.is(made.getBreedOffspring(level, made)), "a shark's pup is a dolphin");
				return made.getUUID();
			});

			// It swims with nobody.
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				Dolphin made = (Dolphin) s.overworld().getEntity(shark);
				check(!player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), made),
					"a shark handed out Dolphin's Grace");
				check(!player.hasEffect(MobEffects.DOLPHINS_GRACE), "Dolphin's Grace stuck anyway");
			});

			// Left alone, it leaves you alone: in the water with it for five seconds.
			server.runOnServer(s -> connection.getServerPlayer().teleportTo(
				tank.getX() + 5.5, tank.getY() + 2, tank.getZ() + 5.5));
			context.waitTicks(100);
			server.runOnServer(s -> check(((Dolphin) s.overworld().getEntity(shark)).getTarget() == null,
				"a shark nobody touched went after the player"));

			// Near its pup, it does not.
			server.runOnServer(s -> spawn(s.overworld(), tank.offset(5, 2, 6), true));
			boolean guarded = false;
			for (int i = 0; i < 10 && !guarded; i++) {
				context.waitTicks(20);
				guarded = server.computeOnServer(s -> ((Dolphin) s.overworld().getEntity(shark)).getTarget()
					instanceof ServerPlayer);
			}
			check(guarded, "a shark let the player swim right up to its pup");

			// Clear the tank of everything but the shark; then feed it.
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				// Up on the roof, out of the water and out of reach, where a survival player can
				// stand for a minute and a half without drowning or falling.
				connection.getServerPlayer().teleportTo(tank.getX() + 5.5, tank.getY() + DEPTH + 4,
					tank.getZ() + 5.5);
				level.getEntitiesOfClass(Dolphin.class, box(tank), d -> !d.getUUID().equals(shark))
					.forEach(Dolphin::discard);
				Dolphin made = (Dolphin) level.getEntity(shark);
				made.setTarget(null);
				made.setLastHurtByMob(null);
				made.setHealth(made.getMaxHealth() - 8F);
				Cod cod = EntityTypes.COD.create(level, EntitySpawnReason.COMMAND);
				cod.snapTo(tank.getX() + 7.5, tank.getY() + 2, tank.getZ() + 7.5);
				cod.setPersistenceRequired();
				level.addFreshEntity(cod);
			});
			boolean eaten = false;
			for (int i = 0; i < 90 && !eaten; i++) {
				context.waitTicks(20);
				eaten = server.computeOnServer(s -> s.overworld()
					.getEntitiesOfClass(Cod.class, box(tank), Cod::isAlive).isEmpty());
			}
			check(eaten, "a shark sat in a tank with a cod for ninety seconds and never ate it");
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				List<ItemEntity> floating = level.getEntitiesOfClass(ItemEntity.class, box(tank),
					item -> item.getItem().is(Items.COD));
				check(floating.isEmpty(), "an eaten cod was left floating");
				Dolphin made = (Dolphin) level.getEntity(shark);
				check(made.getHealth() > made.getMaxHealth() - 8F, "a meal did not feed the shark");
			});

			// On a fishing line: the small one pays out, the large one comes up alive.
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				ServerPlayer player = connection.getServerPlayer();
				player.teleportTo(tank.getX() + 5.5, tank.getY() + DEPTH + 1, tank.getZ() + 0.5);
				level.getEntitiesOfClass(Dolphin.class, box(tank)).forEach(Dolphin::discard);
				Quarry quarry = sharkQuarry();
				Vec3 origin = new Vec3(tank.getX() + 5.5, tank.getY() + DEPTH - 0.5, tank.getZ() + 5.5);
				check(quarry.getBonusItem() == Items.COD, "a shark is not cut into whitefish");
				check(!quarry.landAlive(level, player, origin, FishSize.SMALL), "a small shark came up alive");
				check(!quarry.landAlive(level, player, origin, FishSize.MEDIUM), "a medium shark came up alive");
				check(level.getEntitiesOfClass(Dolphin.class, box(tank)).isEmpty(),
					"a shark that was landed was put in the water too");
				check(quarry.landAlive(level, player, origin, FishSize.LARGE), "a large shark was landed");
				List<Dolphin> up = level.getEntitiesOfClass(Dolphin.class, box(tank));
				check(up.size() == 1 && Sharks.is(up.getFirst()), "a large shark did not surface");
				check(up.getFirst().getTarget() == player, "a hooked shark came up calm");
				check(up.getFirst().getScale() > Sharks.WILD_SCALE, "a large shark is wild-sized");
			});
		}
	}

	private static Quarry sharkQuarry() {
		try {
			var field = SharkBites.class.getDeclaredField("SHARK");
			field.setAccessible(true);
			return (Quarry) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not reach the shark quarry", e);
		}
	}

	private static Dolphin spawn(ServerLevel level, BlockPos at, boolean pup) {
		Dolphin dolphin = EntityTypes.DOLPHIN.create(level, EntitySpawnReason.COMMAND);
		dolphin.addTag(Sharks.TAG);
		if (pup) dolphin.setAge(-24000);
		dolphin.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		dolphin.setPersistenceRequired();
		level.addFreshEntity(dolphin);
		return dolphin;
	}

	/**
	 * A closed glass tank, water with an air gap over it. Closed because a shark is a dolphin and
	 * leaps, and a tank in the sky with an open top is a forty-block drop one jump away.
	 */
	private static void fill(ServerLevel level, BlockPos corner) {
		for (int x = -1; x <= TANK; x++) for (int z = -1; z <= TANK; z++) for (int y = 0; y <= DEPTH + 3; y++) {
			boolean wall = x < 0 || z < 0 || x == TANK || z == TANK || y == 0 || y == DEPTH + 3;
			level.setBlockAndUpdate(corner.offset(x, y, z), wall ? Blocks.GLASS.defaultBlockState()
				: y <= DEPTH ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
		}
	}

	private static AABB box(BlockPos corner) {
		return new AABB(corner.getX(), corner.getY(), corner.getZ(),
			corner.getX() + TANK, corner.getY() + DEPTH + 3, corner.getZ() + TANK);
	}

	private static void check(boolean condition, String failure) {
		if (!condition) throw new AssertionError(failure);
	}
}
