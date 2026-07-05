package de.baum2dev.baum2.combat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import de.baum2dev.baum2.registry.ModItems;

/** Applies Poison to whatever a player hits while holding the Poison Dagger. */
public class PoisonDaggerHandler {
    private static final int POISON_DURATION_TICKS = 100;
    private static final int POISON_AMPLIFIER = 0;

    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(PoisonDaggerHandler::onAfterDamage);
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked) {
            return;
        }
        if (source.getAttacker() instanceof PlayerEntity player
                && player.getMainHandStack().getItem() == ModItems.POISON_DAGGER) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER));
        }
    }
}
