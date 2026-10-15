package justfatlard.cobble_jaws.mixin;

import justfatlard.cobble_jaws.Sharks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The two ways a shark is not built like a dolphin.
 *
 * <p>It breathes water. A dolphin has to come up for air, and the goal that sends it up runs ahead
 * of everything else it does, so a shark that kept the habit would break off a chase to surface.
 *
 * <p>And it does not swim with you. A dolphin keeping pace with a swimmer hands them Dolphin's
 * Grace; refusing the effect, rather than taking the dolphin's goal apart by its private class
 * name, says exactly that and nothing else.
 */
@Mixin(LivingEntity.class)
public abstract class SharkBodyMixin {

	@Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
	private void cobbleJaws$gills(CallbackInfoReturnable<Boolean> cir) {
		if (Sharks.is((Entity) (Object) this)) cir.setReturnValue(true);
	}

	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
		at = @At("HEAD"), cancellable = true)
	private void cobbleJaws$noGrace(MobEffectInstance effect, Entity source,
			CallbackInfoReturnable<Boolean> cir) {
		if (Sharks.is(source) && effect.is(MobEffects.DOLPHINS_GRACE)) cir.setReturnValue(false);
	}
}
