package de.baum2dev.baum2.economy;

import de.baum2dev.baum2.entity.DrevathisEntity;
import de.baum2dev.baum2.entity.FallenCometStoneEntity;
import de.baum2dev.baum2.entity.MonsterLevelProvider;
import de.baum2dev.baum2.entity.SilverfishBroodcallerEntity;
import de.baum2dev.baum2.entity.SpiderQueenEntity;
import de.baum2dev.baum2.entity.ZombieColossusEntity;
import de.baum2dev.baum2.events.MobDeathHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Awards Baum Credits to the killer of any monster, alongside the XP MobDeathHandler already
 * grants (same eligibility check, so the two rewards can't drift apart).
 *
 * <p>Reward = type factor x monster level. Monsters without a real per-mob level
 * (everything not implementing MonsterLevelProvider) count as level 1, matching the
 * nameplate's own "Lvl. 1" placeholder.
 */
public class CreditRewardHandler {

    private static final long NORMAL_FACTOR = 1;
    /** Public: RissobeliskBlockEntity pays the stone rate too (its reward path is a block break, not a death). */
    public static final long STONE_FACTOR = 5;
    private static final long BOSS_FACTOR = 10;

    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(CreditRewardHandler::onEntityDeath);
    }

    private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity attacker)
                || !MobDeathHandler.isXpEligibleMonster(entity)) {
            return;
        }
        int level = entity instanceof MonsterLevelProvider leveled ? leveled.getMonsterLevel() : 1;
        awardCredits(attacker, creditFactor(entity) * Math.max(1, level));
    }

    /**
     * Grant + actionbar feedback in one place, so reward paths that never fire AFTER_DEATH
     * (RissobeliskBlockEntity's block-break kill) pay out and announce identically.
     */
    public static void awardCredits(ServerPlayerEntity player, long credits) {
        BaumCreditsManager.addCredits(player, credits);
        player.sendMessage(Text.literal("+" + credits + " Baum Credits"), true);
    }

    private static long creditFactor(LivingEntity entity) {
        if (entity instanceof FallenCometStoneEntity) {
            return STONE_FACTOR;
        }
        if (entity instanceof DrevathisEntity
                || entity instanceof SpiderQueenEntity
                || entity instanceof ZombieColossusEntity
                || entity instanceof SilverfishBroodcallerEntity) {
            return BOSS_FACTOR;
        }
        return NORMAL_FACTOR;
    }
}
