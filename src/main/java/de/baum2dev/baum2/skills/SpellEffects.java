package de.baum2dev.baum2.skills;

import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Gameplay effects for the 8 spells in {@link Spell}. All server-side only. See
 * docs/fabric-modding.md "Combat / Skill effects" for the API research behind these calls
 * (notably: LivingEntity.damage needs a ServerWorld param, getEntitiesByClass doesn't exist
 * (use getEntitiesByType), and knockback on a player target needs a manual
 * EntityVelocityUpdateS2CPacket or the pushed player's own client never sees it).
 */
final class SpellEffects {

    static void schildstoss(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d look = player.getRotationVector();
        Vec3d center = player.getEntityPos().add(look.x * 2, 0, look.z * 2);
        DamageSource source = world.getDamageSources().indirectMagic(player, player);

        for (LivingEntity target : nearbyLivingEntities(world, center, 4, 3, 4, player)) {
            target.damage(world, source, 4.0f);
            pushAwayFrom(target, player.getX(), player.getZ(), 0.6);
        }
    }

    static void standhafteAura(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 6, 0));
    }

    static void nebelschritt(ServerPlayerEntity player) {
        Vec3d look = player.getRotationVector();
        Vec3d offset = new Vec3d(look.x, 0, look.z).normalize().multiply(5.0);
        player.requestTeleportOffset(offset.x, offset.y, offset.z);
    }

    static void klingenwirbel(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        DamageSource source = world.getDamageSources().indirectMagic(player, player);

        for (LivingEntity target : nearbyLivingEntities(world, player.getEntityPos(), 5, 4, 5, player)) {
            target.damage(world, source, 3.0f);
        }
    }

    static void runenfunke(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        double range = 12.0;
        Vec3d look = player.getRotationVector().normalize();

        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : nearbyLivingEntities(world, player.getEntityPos(), range * 2, range * 2, range * 2, player)) {
            Vec3d toCandidate = candidate.getEntityPos().subtract(player.getEntityPos());
            double distSq = toCandidate.lengthSquared();
            if (distSq > range * range || distSq < 1.0E-4) {
                continue;
            }
            if (look.dotProduct(toCandidate.normalize()) < 0.5) {
                continue; // outside a roughly 60-degree forward cone
            }
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = candidate;
            }
        }

        if (closest != null) {
            DamageSource source = world.getDamageSources().indirectMagic(player, player);
            closest.damage(world, source, 5.0f);
        }
    }

    static void arkanerKreis(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box box = Box.of(player.getEntityPos(), 10, 6, 10);
        List<ServerPlayerEntity> allies = world.getEntitiesByType(
            TypeFilter.instanceOf(ServerPlayerEntity.class), box, ServerPlayerEntity::isAlive
        );

        for (ServerPlayerEntity ally : allies) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 20 * 15, 0));
        }
    }

    static void lebensband(ServerPlayerEntity player) {
        // A flat heal (originally 6.0) is negligible against Life's post-Vitals-rework scale
        // (500-2480, see VitalsCurve.getMaxLife) and gets outpaced by passive Life Regen after
        // just a few Endurance points - balance-reviewer finding. A percentage of max life
        // scales with the player instead of going stale as Endurance grows.
        player.heal(player.getMaxHealth() * 0.12f);
    }

    static void geisterwoge(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        for (LivingEntity target : nearbyLivingEntities(world, player.getEntityPos(), 8, 4, 8, player)) {
            pushAwayFrom(target, player.getX(), player.getZ(), 0.8);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 3, 0));
        }
    }

    private static List<LivingEntity> nearbyLivingEntities(
        ServerWorld world, Vec3d center, double xSize, double ySize, double zSize, ServerPlayerEntity exclude
    ) {
        Box box = Box.of(center, xSize, ySize, zSize);
        return world.getEntitiesByType(
            TypeFilter.instanceOf(LivingEntity.class), box, entity -> entity != exclude && entity.isAlive()
        );
    }

    /** Pushes {@code target} directly away from ({@code sourceX}, {@code sourceZ}). */
    private static void pushAwayFrom(LivingEntity target, double sourceX, double sourceZ, double strength) {
        double dx = sourceX - target.getX();
        double dz = sourceZ - target.getZ();
        target.takeKnockback(strength, dx, dz);
        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(targetPlayer));
        }
    }

    private SpellEffects() {
    }
}
