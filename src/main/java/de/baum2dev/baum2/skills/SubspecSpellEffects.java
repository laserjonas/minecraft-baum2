package de.baum2dev.baum2.skills;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.TypeFilter;
import de.baum2dev.baum2.progression.PlayerProgressData;
import de.baum2dev.baum2.progression.VitalsCurve;

/**
 * Sub-spec variants of the 4 class damage/heal spells (see {@link SpellVariantRegistry}). Each
 * method here mirrors its base spell in {@link SpellEffects} with one behavioral fork per the
 * approved Class Overhaul v2 plan - reuses {@link SpellEffects}'s package-private helpers rather
 * than duplicating targeting/knockback/scaling logic.
 */
final class SubspecSpellEffects {

    /** Bollwerk: Schildstoß also grants the caster Resistance I for 3s. */
    static void schildstossBollwerk(ServerPlayerEntity player) {
        SpellEffects.schildstoss(player);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0));
    }

    /** Stahlfaust: trades Schildstoß's knockback for extra damage (0.6->0.2 knockback, 0.35->0.55 coefficient). */
    static void schildstossStahlfaust(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d look = player.getRotationVector();
        Vec3d center = player.getEntityPos().add(look.x * 2, 0, look.z * 2);
        DamageSource source = world.getDamageSources().indirectMagic(player, player);
        float damage = SpellEffects.scaledDamage(player, 4.0f, 0.55f, VitalsCurve::getBaseAttack, PlayerProgressData::getStrength);

        for (LivingEntity target : SpellEffects.nearbyLivingEntities(world, center, 4, 3, 4, player)) {
            target.damage(world, source, damage);
            SpellEffects.pushAwayFrom(target, player.getX(), player.getZ(), 0.2);
        }
    }

    /** Sturmklinge: Klingenwirbel fires twice, 5 ticks apart, 70% damage each. */
    static void klingenwirbelSturmklinge(ServerPlayerEntity player) {
        klingenwirbelHit(player, 0.7f);
        DelayedSpellEffectScheduler.schedule(player, 5, p -> klingenwirbelHit(p, 0.7f));
    }

    private static void klingenwirbelHit(ServerPlayerEntity player, float damageMultiplier) {
        ServerWorld world = player.getEntityWorld();
        DamageSource source = world.getDamageSources().indirectMagic(player, player);
        float damage = SpellEffects.scaledDamage(player, 3.0f, 0.35f, VitalsCurve::getBaseAttack, PlayerProgressData::getStrength) * damageMultiplier;

        for (LivingEntity target : SpellEffects.nearbyLivingEntities(world, player.getEntityPos(), 5, 4, 5, player)) {
            target.damage(world, source, damage);
        }
    }

    /** Schattenpirscher: Klingenwirbel applies Weakness I for 4s to every hit target. */
    static void klingenwirbelSchattenpirscher(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        DamageSource source = world.getDamageSources().indirectMagic(player, player);
        float damage = SpellEffects.scaledDamage(player, 3.0f, 0.35f, VitalsCurve::getBaseAttack, PlayerProgressData::getStrength);

        for (LivingEntity target : SpellEffects.nearbyLivingEntities(world, player.getEntityPos(), 5, 4, 5, player)) {
            target.damage(world, source, damage);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));
        }
    }

    /** Splitterrune: Runenfunke also strikes one more enemy within 3 blocks for 50% damage. */
    static void runenfunkeSplitterrune(ServerPlayerEntity player) {
        LivingEntity primary = SpellEffects.findRunenfunkeTarget(player);
        if (primary == null) {
            return;
        }
        ServerWorld world = player.getEntityWorld();
        DamageSource source = world.getDamageSources().indirectMagic(player, player);
        float damage = SpellEffects.scaledDamage(player, 5.0f, 0.6f, VitalsCurve::getBaseMagicAttack, PlayerProgressData::getIntelligence);
        primary.damage(world, source, damage);

        for (LivingEntity secondary : SpellEffects.nearbyLivingEntities(world, primary.getEntityPos(), 6, 6, 6, player)) {
            if (secondary != primary) {
                secondary.damage(world, source, damage * 0.5f);
                break;
            }
        }
    }

    /** Glücksrune: 20% chance Runenfunke doesn't consume its cooldown (Mana is still spent). */
    static void runenfunkeGluecksrune(ServerPlayerEntity player) {
        SpellEffects.runenfunke(player);
        if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
            SkillCooldownManager.clearCooldown(player, Spell.RUNENFUNKE);
        }
    }

    /** Wurzelwall: Lebensband also grants the caster Resistance I for 3s. */
    static void lebensbandWurzelwall(ServerPlayerEntity player) {
        SpellEffects.lebensband(player);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0));
    }

    /** Wesensfülle: Lebensband also heals up to 2 nearby allies (6 blocks) for the same 12% of their max life. */
    static void lebensbandWesensfuelle(ServerPlayerEntity player) {
        SpellEffects.lebensband(player);

        ServerWorld world = player.getEntityWorld();
        List<ServerPlayerEntity> allies = world.getEntitiesByType(
            TypeFilter.instanceOf(ServerPlayerEntity.class),
            Box.of(player.getEntityPos(), 12, 6, 12),
            ally -> ally != player && ally.isAlive()
        );

        int healed = 0;
        for (ServerPlayerEntity ally : allies) {
            if (healed >= 2) {
                break;
            }
            ally.heal(ally.getMaxHealth() * 0.12f);
            healed++;
        }
    }

    private SubspecSpellEffects() {
    }
}
