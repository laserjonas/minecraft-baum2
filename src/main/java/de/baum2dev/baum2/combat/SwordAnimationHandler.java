package de.baum2dev.baum2.combat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import de.baum2dev.baum2.entity.MountHorseEntity;
import de.baum2dev.baum2.items.TemplateSwordItem;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * Plays the template sword's one-shot GeckoLib animation on every landed melee hit:
 * the on-foot moulinet flourish normally, the cavalry sweep while riding one of our mounts.
 *
 * Deliberately the same hook and scoping as MountedCombatHandler's own horse-attack
 * animation (ServerLivingEntityEvents.AFTER_DAMAGE + DamageTypes.PLAYER_ATTACK + skip
 * blocked): a mounted hit therefore triggers horse AND blade animation in the same tick,
 * and spells/arrows/our own AOE splash can't trigger a sword flourish. Whiffed swings play
 * only the vanilla arm swing - matching the horse rule that a landed hit is what animates.
 */
public final class SwordAnimationHandler {

    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(SwordAnimationHandler::onAfterDamage);
    }

    private static void onAfterDamage(
            LivingEntity victim, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked
                || !source.isOf(DamageTypes.PLAYER_ATTACK)
                || !(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return;
        }
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof TemplateSwordItem sword)
                || !(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        boolean mounted = player.getVehicle() instanceof MountHorseEntity;
        sword.triggerAnim(player, GeoItem.getOrAssignId(stack, world), TemplateSwordItem.CONTROLLER_NAME,
                mounted ? TemplateSwordItem.ATTACK_MOUNTED_TRIGGER : TemplateSwordItem.ATTACK_TRIGGER);
    }

    private SwordAnimationHandler() {
    }
}
