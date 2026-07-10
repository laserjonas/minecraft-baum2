package de.baum2dev.baum2.combat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import de.baum2dev.baum2.registry.ModItems;

/**
 * Drevathis's Cursed Blade has 0 base stats (see ModItems) - its entire purpose is this proc:
 * every melee hit also fires "Wave of Darkness" for 10% of the wielder's real, live Attack
 * Damage (so it scales with the player's own Strength investment), same shape (20x8) as the
 * boss's own Wave of Darkness but with no cast-time telegraph, since it's a passive proc. Aimed
 * from the wielder toward whatever was actually hit ({@code entity}, the first callback param)
 * - an earlier version aimed along the wielder's look vector instead and only queried
 * {@code PlayerEntity} targets in {@code DarkWaveEffect}, meaning the proc could never damage
 * the mob you actually hit and instead only ever hit other nearby players (balance-reviewer
 * finding, fixed here and in {@code DarkWaveEffect} together).
 */
public class DrevathisCursedBladeHandler {
    private static final double WAVE_RANGE = 20.0;
    private static final double WAVE_WIDTH = 8.0;
    private static final float PROC_DAMAGE_RATIO = 0.10F;

    /**
     * Reentrancy guard (balance-reviewer finding, mount-system session): the wave's own damage
     * is indirectMagic with the wielder as attacker, so without this each wave victim's
     * AFTER_DAMAGE re-entered this handler and cast another full wave - recursive already for
     * a single melee hit, and quadratic once Schlachtross's AOE splash lands multi-target
     * hits. A wave can no longer proc further waves; one swing = at most one wave per victim
     * of that swing.
     */
    private static boolean castingWave = false;

    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(DrevathisCursedBladeHandler::onAfterDamage);
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || castingWave) {
            return;
        }
        if (!(source.getAttacker() instanceof PlayerEntity player)
                || player.getMainHandStack().getItem() != ModItems.DREVATHIS_CURSED_BLADE) {
            return;
        }
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        float procDamage = (float) player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE) * PROC_DAMAGE_RATIO;
        if (procDamage <= 0.0F) {
            return;
        }
        Vec3d direction = entity.getEntityPos().subtract(player.getEntityPos());
        castingWave = true;
        try {
            DarkWaveEffect.cast(world, player, player.getEntityPos(), direction, procDamage, WAVE_RANGE, WAVE_WIDTH);
        } finally {
            castingWave = false;
        }
    }

    private DrevathisCursedBladeHandler() {
    }
}
