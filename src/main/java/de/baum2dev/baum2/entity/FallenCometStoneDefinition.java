package de.baum2dev.baum2.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import org.jetbrains.annotations.Nullable;

/**
 * Everything that distinguishes one fallen-comet-stone mini-boss from another. The stones all
 * share one entity class ({@link FallenCometStoneEntity}), one GeckoLib geometry/animation
 * pair, and one renderer - a stone IS this record plus a texture. The full table lives in
 * {@code registry.FallenCometStones}; registration loops over it in {@code ModEntities} and
 * {@code Baum2Client}.
 *
 * @param name            registry path AND texture name, e.g. {@code "stone_of_spiders"}
 *                        (entity id {@code baum2:<name>}, texture
 *                        {@code textures/entity/<name>.png}, lang key
 *                        {@code entity.baum2.<name>})
 * @param level           monster level shown on the nameplate; also drives max health
 *                        (20 HP per level - the ratio both original stones established)
 * @param wave            what spawns on each 10%-of-max-health increment lost
 * @param drops           built fresh on every death (ItemStacks are mutable - never share one)
 * @param ambientParticle optional cosmetic client-side particle drifting off the stone
 *                        (Stone of Zombies' smoke), null for none
 */
public record FallenCometStoneDefinition(
        String name,
        int level,
        List<WaveMob<?>> wave,
        Supplier<List<ItemStack>> drops,
        @Nullable ParticleEffect ambientParticle) {

    public double maxHealth() {
        return this.level * 20.0;
    }

    /**
     * One mob entry of a stone's wave: {@code count} spawns of {@code type} per wave, each
     * optionally adjusted by {@code customizer} right after creation (babies, zombification
     * immunity, ...).
     */
    public record WaveMob<T extends MobEntity>(EntityType<T> type, int count, @Nullable Consumer<T> customizer) {

        public static <T extends MobEntity> WaveMob<T> of(EntityType<T> type, int count) {
            return new WaveMob<>(type, count, null);
        }

        public static <T extends MobEntity> WaveMob<T> of(EntityType<T> type, int count, Consumer<T> customizer) {
            return new WaveMob<>(type, count, customizer);
        }
    }
}
