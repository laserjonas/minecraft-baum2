package de.baum2dev.baum2.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
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

        if (attacker != null && isXpEligibleMonster(entity)) {
            long xpReward = calculateXpReward(entity);
            PlayerLevelSystem.addExperience(attacker, xpReward);
            LevelUpHandler.checkLevelUp(attacker);
        }
    }

    /**
     * The old check was {@code instanceof HostileEntity}, which silently excluded every
     * monster outside that exact class hierarchy - Slimes/Magma Cubes, Ghasts, Phantoms,
     * Shulkers, Hoglins (all {@link Monster} but not HostileEntity) and Camel Husks/Zombie
     * Nautiluses (monster spawn group, mount/tameable hierarchy) gave zero XP, found by
     * balance review when the stone ladder started spawning them as wave mobs. Eligible =
     * implements Monster OR registered in the MONSTER spawn group - both together cover the
     * whole vanilla hostile roster without paying XP for passive animals.
     */
    private static boolean isXpEligibleMonster(LivingEntity entity) {
        return entity instanceof Monster || entity.getType().getSpawnGroup() == SpawnGroup.MONSTER;
    }

    private static long calculateXpReward(LivingEntity entity) {
        int maxHealth = (int) entity.getMaxHealth();
        return 10L + maxHealth / 2;
    }
}
