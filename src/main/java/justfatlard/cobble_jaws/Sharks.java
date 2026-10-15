package justfatlard.cobble_jaws;

import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import org.jspecify.annotations.Nullable;

/**
 * Which dolphins are sharks, and what being one does to the body.
 *
 * <p>A shark is a {@link Dolphin}. The game will not let a server-side mod hand a client a new
 * creature to draw, so a new animal would be invisible on every vanilla client; what it will do is
 * let an existing one be a different size and wear a different skin. The dolphin is the one built
 * like a shark already - a long body, a fin on its back, a tail it drives itself with - and it
 * already fights back when hit, which is half the temper wanted.
 *
 * <p>Which dolphin is a shark is written on it as a tag rather than held in a table here. Tags ride
 * along in the entity's own save data, survive this mod being removed, and can be read and set by
 * command, which is also how a shark is summoned by hand:
 * {@code /summon minecraft:dolphin ~ ~ ~ {Tags:["shark"]}}.
 */
public final class Sharks {
	private Sharks() {}

	public static final String TAG = "shark";

	/** A wild shark against a dolphin, which is 1. */
	public static final float WILD_SCALE = 1.15F;

	/**
	 * Per unit of scale. A dolphin is 10 health and 3 damage; a shark is twice as hard to kill and
	 * bites harder, and a bigger one more so, because a large shark that was only visually larger
	 * would be a lie the first time one fought back.
	 */
	private static final double HEALTH_PER_SCALE = 20.0;
	private static final double DAMAGE_PER_SCALE = 4.0;

	private static final Identifier ADULT_SKIN =
		Identifier.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/shark.png");
	/** A pup is drawn with the baby dolphin's model, which lays its texture out differently. */
	private static final Identifier PUP_SKIN =
		Identifier.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/shark_pup.png");

	private static final Identifier SIZE_ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "shark_size");

	public static boolean is(@Nullable Entity entity) {
		return entity instanceof Dolphin && entity.entityTags().contains(TAG);
	}

	public static Identifier skin(boolean pup) {
		return pup ? PUP_SKIN : ADULT_SKIN;
	}

	/**
	 * Make this dolphin a shark of the given size, once.
	 *
	 * <p>Size, health and bite are written into the animal's own attributes, so they are saved with
	 * it and nothing here has to say them again.
	 */
	public static void become(Dolphin dolphin, float scale) {
		dolphin.addTag(TAG);
		permanent(dolphin.getAttribute(Attributes.SCALE), scale - 1.0,
			AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		permanent(dolphin.getAttribute(Attributes.MAX_HEALTH), HEALTH_PER_SCALE * scale - 10.0,
			AttributeModifier.Operation.ADD_VALUE);
		permanent(dolphin.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_PER_SCALE * scale - 3.0,
			AttributeModifier.Operation.ADD_VALUE);
		dolphin.setHealth(dolphin.getMaxHealth());
	}

	private static void permanent(@Nullable AttributeInstance attribute, double amount,
			AttributeModifier.Operation operation) {
		if (attribute == null) return;
		attribute.removeModifier(SIZE_ID);
		attribute.addPermanentModifier(new AttributeModifier(SIZE_ID, amount, operation));
	}

	/**
	 * Everything about a shark that is not saved with it, said again each time it loads.
	 *
	 * <p>The skin is held in memory by Pandorical and dropped when the entity unloads, and goals are
	 * never saved at all. A shark tagged by hand arrives with no size, so it gets the wild one.
	 */
	public static void ensure(Dolphin dolphin) {
		if (!is(dolphin)) return;
		AttributeInstance scale = dolphin.getAttribute(Attributes.SCALE);
		if (scale != null && !scale.hasModifier(SIZE_ID)) become(dolphin, WILD_SCALE);
		// A shark plays with nothing: a dolphin carrying a stick about is a dolphin.
		dolphin.setCanPickUpLoot(false);
		dress(dolphin);
		Instincts.install(dolphin);
	}

	/** The right skin for the body it has now: a pup's model is not an adult's. */
	public static void dress(Dolphin dolphin) {
		if (!is(dolphin)) return;
		PandoricalApi.entityOverlays().set(dolphin, skin(dolphin.isBaby()));
	}
}
