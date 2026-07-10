package de.baum2dev.baum2.mounts;

import java.util.List;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypeFilter;
import de.baum2dev.baum2.entity.MountHorseEntity;

/**
 * Enforces the per-tier combat rules while mounted:
 * - Wanderross (canFightMounted = false): melee attacks are cancelled outright.
 * - Eisenross: normal melee allowed, nothing else changes.
 * - Schlachtross: each landed melee hit additionally splashes AOE damage around the struck
 *   target (a fraction of the real damage dealt, so Strength/crit scaling carries over).
 * Every landed mounted melee hit also plays the horse's own attack animation (the "every
 * horse attack must be animated" rule) - triggered server-side on the mount entity.
 *
 * Scoped to true melee swings via DamageTypes.PLAYER_ATTACK - deliberately narrower than
 * PoisonDaggerHandler's any-damage check, so spells cast while mounted don't splash (that
 * handler's unscoped-ness is a logged open issue, not a precedent to copy).
 */
public final class MountedCombatHandler {

    /**
     * At most this many extra victims per swing (nearest first). Without a cap the splash
     * multiplies the already-flagged melee DPS ceiling (HANDOFF.md, "Combat System v1") by an
     * unbounded target count against clustered spawns - balance-reviewer critical finding.
     */
    private static final int MAX_SPLASH_TARGETS = 5;

    /** Reentrancy guard: the AOE splash below itself fires AFTER_DAMAGE for each victim. */
    private static boolean applyingAoeSplash = false;

    public static void registerEvents() {
        // Cancel the swing before damage is computed if the mount forbids fighting.
        // AttackEntityCallback fires on both logical sides, so the mounted client gets the
        // miss for free (no ghost swing), and the server stays authoritative.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.getVehicle() instanceof MountHorseEntity mount
                    && !mount.tier().canFightMounted()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(
                            Text.literal("The " + mount.tier().id() + " is not trained for combat."), true);
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register(MountedCombatHandler::onAfterDamage);
    }

    private static void onAfterDamage(
            LivingEntity victim, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || applyingAoeSplash) {
            return;
        }
        // Only real melee swings (not spells, arrows, or our own splash) by a mounted player.
        if (!source.isOf(DamageTypes.PLAYER_ATTACK)
                || !(source.getAttacker() instanceof ServerPlayerEntity player)
                || !(player.getVehicle() instanceof MountHorseEntity mount)) {
            return;
        }

        // Rule: every horse attack is animated - a landed mounted hit IS the horse attacking
        // (it lunges/stomps alongside the rider's swing).
        mount.playAttackAnimation();

        double radius = mount.tier().meleeAoeRadius();
        if (radius <= 0.0 || damageTaken <= 0.0F) {
            return;
        }

        ServerWorld world = (ServerWorld) victim.getEntityWorld();
        float splashDamage = damageTaken * mount.tier().meleeAoeDamageFraction();
        // getEntitiesByType, not getEntitiesByClass - the latter no longer exists in this
        // mapping (docs/fabric-modding.md, "AoE nearby-entity query").
        List<LivingEntity> splashTargets = world.getEntitiesByType(
                TypeFilter.instanceOf(LivingEntity.class),
                victim.getBoundingBox().expand(radius),
                other -> other != victim && other != player && other != mount
                        && !(other instanceof PlayerEntity)
                        && !(other instanceof MountHorseEntity)
                        && other.isAlive());

        splashTargets.sort(java.util.Comparator.comparingDouble(victim::squaredDistanceTo));

        applyingAoeSplash = true;
        try {
            for (LivingEntity splashTarget : splashTargets.subList(0, Math.min(MAX_SPLASH_TARGETS, splashTargets.size()))) {
                splashTarget.damage(world, player.getDamageSources().playerAttack(player), splashDamage);
            }
        } finally {
            applyingAoeSplash = false;
        }
    }

    private MountedCombatHandler() {
    }
}
