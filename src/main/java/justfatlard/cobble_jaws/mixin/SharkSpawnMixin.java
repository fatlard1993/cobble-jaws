package justfatlard.cobble_jaws.mixin;

import justfatlard.cobble_jaws.Spawns;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Whether a dolphin pod is sharks, decided once, as it arrives.
 *
 * <p>On {@code Mob} rather than {@code Dolphin}, at the return, so the group data the dolphin made
 * for its pod is already there to read: that object is what the rest of the pod is handed.
 */
@Mixin(Mob.class)
public abstract class SharkSpawnMixin {

	@Inject(method = "finalizeSpawn", at = @At("RETURN"))
	private void cobbleJaws$maybeShark(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
		if (!((Object) this instanceof Dolphin dolphin)) return;
		SpawnGroupData pod = cir.getReturnValue();
		if (pod == null) return;
		Spawns.onSpawn(dolphin, level.getBiome(dolphin.blockPosition()), reason, pod);
	}
}
