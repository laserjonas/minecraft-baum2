package de.baum2dev.baum2.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.progression.PlayerProgressData;
import de.baum2dev.baum2.progression.VitalsCurve;

/**
 * Rolls our own Dexterity-driven Crit Chance % on every player melee attack, independent of
 * vanilla's own fall-based critical hit (which is already folded into the damage float by
 * the time this runs). No Fabric API event can modify the final damage amount of
 * Entity.sidedDamage (only cancel/allow events exist), so this intercepts the argument at
 * the actual call site inside PlayerEntity.attack().
 */
@Mixin(PlayerEntity.class)
public class PlayerAttackDamageMixin {
    private static final float CRIT_DAMAGE_MULTIPLIER = 1.5f;

    @ModifyArg(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;sidedDamage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
            ),
            index = 1
    )
    private float baum2$applyCritRoll(float amount) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return amount;
        }

        PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(attacker);
        double critChance = VitalsCurve.getCritChance(progress.getDexterity());
        if (Math.random() * 100.0 < critChance) {
            return amount * CRIT_DAMAGE_MULTIPLIER;
        }
        return amount;
    }
}
