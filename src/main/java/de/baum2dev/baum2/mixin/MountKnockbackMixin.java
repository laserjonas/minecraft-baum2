package de.baum2dev.baum2.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.baum2dev.baum2.entity.MountHorseEntity;

/**
 * "While mounted, the player cannot be knocked back": cancels LivingEntity.takeKnockback for
 * any entity riding one of our mounts. No Fabric API event covers knockback (confirmed, see
 * docs/fabric-modding.md "Knockback suppression for a mounted player"), so this is a plain
 * cancellable HEAD inject. Deliberately scoped to MountHorseEntity vehicles only - riding a
 * boat/vanilla horse keeps vanilla behavior.
 */
@Mixin(LivingEntity.class)
public abstract class MountKnockbackMixin {

    @Inject(method = "takeKnockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void baum2$suppressKnockbackWhileMounted(double strength, double x, double z, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getVehicle() instanceof MountHorseEntity) {
            ci.cancel();
        }
    }
}
