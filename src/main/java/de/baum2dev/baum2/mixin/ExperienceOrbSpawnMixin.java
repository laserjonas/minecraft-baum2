package de.baum2dev.baum2.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbSpawnMixin {

    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
    private static void preventAllExperienceOrbSpawn(ServerWorld world, Vec3d pos, int amount, CallbackInfo ci) {
        ci.cancel();
    }
}
