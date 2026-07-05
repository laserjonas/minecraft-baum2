package de.baum2dev.baum2.skills;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.classes.ClassSubspec;

/**
 * Pure lookup table from (spell, sub-spec) to that sub-spec's forked behavior for the spell, if
 * any - see {@link SubspecSpellEffects} for the actual effects and {@code SpellCaster
 * .attemptCast} for the resolution point. Mirrors {@code classes.SubspecRegistry}'s static-
 * lookup idiom. A spell/sub-spec pair with no entry here just casts the spell's own base effect.
 */
final class SpellVariantRegistry {
    private static final Map<Spell, Map<ClassSubspec, Consumer<ServerPlayerEntity>>> VARIANTS = build();

    static Optional<Consumer<ServerPlayerEntity>> variantFor(Spell spell, ClassSubspec subspec) {
        return Optional.ofNullable(VARIANTS.getOrDefault(spell, Map.of()).get(subspec));
    }

    private static Map<Spell, Map<ClassSubspec, Consumer<ServerPlayerEntity>>> build() {
        Map<Spell, Map<ClassSubspec, Consumer<ServerPlayerEntity>>> variants = new EnumMap<>(Spell.class);

        Map<ClassSubspec, Consumer<ServerPlayerEntity>> schildstossForks = new EnumMap<>(ClassSubspec.class);
        schildstossForks.put(ClassSubspec.BOLLWERK, SubspecSpellEffects::schildstossBollwerk);
        schildstossForks.put(ClassSubspec.STAHLFAUST, SubspecSpellEffects::schildstossStahlfaust);
        variants.put(Spell.SCHILDSTOSS, schildstossForks);

        Map<ClassSubspec, Consumer<ServerPlayerEntity>> klingenwirbelForks = new EnumMap<>(ClassSubspec.class);
        klingenwirbelForks.put(ClassSubspec.STURMKLINGE, SubspecSpellEffects::klingenwirbelSturmklinge);
        klingenwirbelForks.put(ClassSubspec.SCHATTENPIRSCHER, SubspecSpellEffects::klingenwirbelSchattenpirscher);
        variants.put(Spell.KLINGENWIRBEL, klingenwirbelForks);

        Map<ClassSubspec, Consumer<ServerPlayerEntity>> runenfunkeForks = new EnumMap<>(ClassSubspec.class);
        runenfunkeForks.put(ClassSubspec.SPLITTERRUNE, SubspecSpellEffects::runenfunkeSplitterrune);
        runenfunkeForks.put(ClassSubspec.GLUECKSRUNE, SubspecSpellEffects::runenfunkeGluecksrune);
        variants.put(Spell.RUNENFUNKE, runenfunkeForks);

        Map<ClassSubspec, Consumer<ServerPlayerEntity>> lebensbandForks = new EnumMap<>(ClassSubspec.class);
        lebensbandForks.put(ClassSubspec.WURZELWALL, SubspecSpellEffects::lebensbandWurzelwall);
        lebensbandForks.put(ClassSubspec.WESENSFUELLE, SubspecSpellEffects::lebensbandWesensfuelle);
        variants.put(Spell.LEBENSBAND, lebensbandForks);

        return variants;
    }

    private SpellVariantRegistry() {
    }
}
