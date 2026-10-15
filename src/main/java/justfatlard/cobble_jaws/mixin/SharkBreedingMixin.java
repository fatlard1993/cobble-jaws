package justfatlard.cobble_jaws.mixin;

import justfatlard.cobble_jaws.Sharks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A shark's pup is a shark.
 *
 * <p>Dolphins do not breed, but a spawn egg used on one makes its young through this same method,
 * and a pup made that way never passes the spawn roll. Without this an egg on a shark hatches a
 * dolphin calf beside it.
 */
@Mixin(Dolphin.class)
public abstract class SharkBreedingMixin {

	@Inject(method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/animal/dolphin/Dolphin;",
		at = @At("RETURN"))
	private void cobbleJaws$inherit(ServerLevel level, AgeableMob mate, CallbackInfoReturnable<Dolphin> cir) {
		Dolphin pup = cir.getReturnValue();
		if (pup == null) return;
		if (Sharks.is((Dolphin) (Object) this) || Sharks.is(mate)) Sharks.become(pup, Sharks.WILD_SCALE);
	}
}
