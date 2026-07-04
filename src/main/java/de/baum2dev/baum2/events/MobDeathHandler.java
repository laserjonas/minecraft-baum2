package de.baum2dev.baum2.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.progression.PlayerLevelSystem;

public class MobDeathHandler {

    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(MobDeathHandler::onEntityDeath);
    }

    private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
        ServerPlayerEntity attacker = null;
        if (damageSource.getAttacker() instanceof ServerPlayerEntity) {
            attacker = (ServerPlayerEntity) damageSource.getAttacker();
        }

        if (attacker != null && entity instanceof HostileEntity) {
            long xpReward = calculateXpReward(entity);
            PlayerLevelSystem.addExperience(attacker, xpReward);
            LevelUpHandler.checkLevelUp(attacker);
        }
    }

    private static long calculateXpReward(LivingEntity entity) {
        int maxHealth = (int) entity.getMaxHealth();
        return 10L + maxHealth / 2;
    }
}
