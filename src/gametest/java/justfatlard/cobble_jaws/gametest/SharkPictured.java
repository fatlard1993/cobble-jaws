package justfatlard.cobble_jaws.gametest;

import justfatlard.cobble_jaws.Shark;
import justfatlard.cobble_jaws.Sharks;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.gametest.Models;
import justfatlard.pandorical.gametest.Pictures;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * A shark and its pup, for the eye: side on, from ahead and above, and from overhead mid-stroke.
 * Held still in clear air, because water tints everything blue and the point is the shape. Taken as
 * {@code shark-side}, {@code shark-front} and {@code shark-above}; with {@code -Ppictures} they are
 * kept in this repo's {@code screenshots/}.
 */
public final class SharkPictured implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isContentReady(connection.getServerPlayer()));
			Pictures.stage(context, server, 6000);

			Vec3 centre = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos ground = Pictures.ground(level, connection.getServerPlayer().getBlockX(),
					connection.getServerPlayer().getBlockZ());
				Vec3 at = Vec3.atBottomCenterOf(ground).add(0, 1.5, 0);
				pose(level, at, false);
				pose(level, at.add(0, 0, 3.2), true);
				return at;
			});
			context.waitTicks(20);

			Pictures.look(server, centre.add(5.5, -0.9, 1.4), centre.add(0, 0.2, 1.4));
			Pictures.settle(context, connection);
			System.out.println("[cobble-jaws test] picture " + Pictures.shoot(context, connection, "shark-side").toAbsolutePath());

			Pictures.look(server, centre.add(-3.2, 0.2, -3.6), centre.add(0, 0.2, 0.6));
			Pictures.settle(context, connection);
			System.out.println("[cobble-jaws test] picture " + Pictures.shoot(context, connection, "shark-front").toAbsolutePath());

			// It swims by sweeping its tail side to side, the fin a beat behind: posed as the
			// renderer poses it, the tail turns about the upright, and never flexes up and down.
			String swim = context.computeOnClient(client -> {
				Entity adult = null;
				for (Entity each : client.level.entitiesForRendering()) {
					if (each instanceof Shark s && !s.isBaby()) adult = each;
				}
				if (adult == null) return "no shark to pose";
				float least = Float.MAX_VALUE, most = -Float.MAX_VALUE, flex = 0F;
				for (int i = 0; i < 8; i++) {
					ModelPart root = Models.posedBody(adult, 0.5F, i);
					if (root == null) return "the shark is not drawn from its own body";
					ModelPart tail = root.getChild("body").getChild("tail");
					least = Math.min(least, tail.yRot);
					most = Math.max(most, tail.yRot);
					flex = Math.max(flex, Math.abs(tail.xRot - (float) Math.toRadians(-6)));
				}
				if (most - least < 0.3F) return "the tail sweeps only " + (most - least) + " radians as it swims";
				if (flex > 0.01F) return "the tail flexes up and down by " + flex;
				return "";
			});
			if (!swim.isEmpty()) throw new AssertionError(swim);

			// From above, mid-stroke.
			server.runOnServer(s -> {
				for (Entity each : s.overworld().getEntities(Sharks.SHARK, e -> true)) {
					PandoricalApi.animations().play(each, "cobble-jaws-justfatlard:shark_swim", true);
				}
			});
			Pictures.look(server, centre.add(0.01, 5.5, 1.4), centre.add(0, 0, 1.4));
			context.waitTicks(5);
			Pictures.settle(context, connection);
			System.out.println("[cobble-jaws test] picture " + Pictures.shoot(context, connection, "shark-above").toAbsolutePath());
		}
	}

	/** Still, level, nose toward -z, hanging where it was put. */
	private static void pose(ServerLevel level, Vec3 at, boolean pup) {
		Shark shark = Sharks.SHARK.create(level, EntitySpawnReason.COMMAND);
		if (pup) shark.setAge(-24000);
		shark.snapTo(at.x, at.y, at.z, 180F, 0F);
		shark.setNoAi(true);
		shark.setNoGravity(true);
		shark.setPersistenceRequired();
		level.addFreshEntity(shark);
	}
}
