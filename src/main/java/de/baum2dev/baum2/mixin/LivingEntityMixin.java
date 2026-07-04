package de.baum2dev.baum2.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Redirect(method = "dropExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"))
    private static void preventHostileExperienceSpawn(ServerWorld world, Vec3d pos, int amount) {
        // Don't spawn experience orbs - prevents all vanilla XP drops
    }
}
