package justfatlard.cobble_jaws;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sharks, out of the dolphin the game already has.
 *
 * <p>Left alone, a shark leaves you alone. Hit one, or come near its pup, and it comes for you; and
 * it hunts the fish, squid and small animals that share its water. See {@link Shark} for why it is
 * built on the dolphin, which is the one decision everything else here follows from.
 */
public class Main implements ModInitializer {
	public static final String MOD_ID = "cobble-jaws-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String FISHING = "minedew-fishing";

	@Override
	public void onInitialize() {
		Sharks.register();
		Spawns.register();
		Feeding.register();
		// The skins, the egg's picture and the names.
		PandoricalApi.content().registerModAssets(MOD_ID);

		// Isolated in its own class and only reached from here: it names Minedew Fishing's types
		// directly, so it must not load without that mod.
		boolean fishing = FabricLoader.getInstance().isModLoaded(FISHING);
		if (fishing) justfatlard.cobble_jaws.integration.SharkBites.register();

		LOGGER.info("[{}] Sharks in the warm seas{}", MOD_ID, fishing ? ", and on the line" : "");
	}
}
