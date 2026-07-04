package de.baum2dev.baum2.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    protected int experiencePoints;

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void preventHostileExperienceDrop(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof HostileEntity) {
            this.experiencePoints = 0;
            ci.cancel();
        }
    }
}
