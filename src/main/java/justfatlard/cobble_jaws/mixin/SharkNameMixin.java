package justfatlard.cobble_jaws.mixin;

import justfatlard.cobble_jaws.Sharks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A shark is introduced as a Shark: in death messages, on block-tip's card, anywhere the game says
 * what an animal is.
 *
 * <p>Not a custom name. That would fix the words and then hang them in the air over the animal, and
 * keep it from ever despawning, which turns a wild shark into somebody's pet.
 */
@Mixin(Entity.class)
public abstract class SharkNameMixin {

	@Inject(method = "getTypeName", at = @At("HEAD"), cancellable = true)
	private void cobbleJaws$shark(CallbackInfoReturnable<Component> cir) {
		if (Sharks.is((Entity) (Object) this)) {
			cir.setReturnValue(Component.translatableWithFallback("entity.cobble-jaws-justfatlard.shark", "Shark"));
		}
	}
}
