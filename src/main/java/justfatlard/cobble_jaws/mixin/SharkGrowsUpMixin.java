package justfatlard.cobble_jaws.mixin;

import justfatlard.cobble_jaws.Sharks;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A pup and an adult are drawn with two different models, each laying its skin out its own way, so
 * the skin has to change on the tick the body does. Left alone, a grown shark wears its pup skin
 * stretched over the wrong boxes until its chunk next reloads.
 */
@Mixin(AgeableMob.class)
public abstract class SharkGrowsUpMixin {

	@Inject(method = "ageBoundaryReached", at = @At("TAIL"))
	private void cobbleJaws$redress(CallbackInfo ci) {
		if ((Object) this instanceof Dolphin dolphin && !dolphin.level().isClientSide()) {
			Sharks.dress(dolphin);
		}
	}
}
