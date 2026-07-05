package de.baum2dev.baum2.classes;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;

/**
 * Static lookup for the 8 class sub-specializations (2 per class) and their passive bonuses.
 * Not final balance — see HANDOFF.md for the pending balance-review step. Values chosen to be
 * modest and proportional to the parent classes' own already-reviewed bonuses (ClassRegistry).
 */
public final class SubspecRegistry {
    private static final Map<ClassSubspec, SubspecDefinition> DEFINITIONS = build();

    public static SubspecDefinition get(ClassSubspec subspec) {
        return DEFINITIONS.get(subspec);
    }

    public static Collection<SubspecDefinition> all() {
        return DEFINITIONS.values();
    }

    public static Collection<SubspecDefinition> forClass(PlayerClass playerClass) {
        return DEFINITIONS.values().stream()
            .filter(definition -> definition.subspec().parentClass() == playerClass)
            .toList();
    }

    private static Map<ClassSubspec, SubspecDefinition> build() {
        Map<ClassSubspec, SubspecDefinition> definitions = new EnumMap<>(ClassSubspec.class);

        definitions.put(ClassSubspec.BOLLWERK, new SubspecDefinition(
            ClassSubspec.BOLLWERK,
            "Bollwerk",
            "Ein unerschütterlicher Schild, der jeden Angriff auffängt.",
            EntityAttributes.ARMOR,
            EntityAttributeModifier.Operation.ADD_VALUE,
            2.0,
            Identifier.of("baum2", "subspec_bonus/bollwerk_armor")
        ));

        definitions.put(ClassSubspec.STAHLFAUST, new SubspecDefinition(
            ClassSubspec.STAHLFAUST,
            "Stahlfaust",
            "Wandelt eiserne Standhaftigkeit in rohe Schlagkraft um.",
            EntityAttributes.ATTACK_DAMAGE,
            EntityAttributeModifier.Operation.ADD_VALUE,
            1.5,
            Identifier.of("baum2", "subspec_bonus/stahlfaust_attack_damage")
        ));

        definitions.put(ClassSubspec.SCHATTENPIRSCHER, new SubspecDefinition(
            ClassSubspec.SCHATTENPIRSCHER,
            "Schattenpirscher",
            "Schlägt gnadenlos aus dem Verborgenen zu.",
            EntityAttributes.ATTACK_DAMAGE,
            EntityAttributeModifier.Operation.ADD_VALUE,
            1.5,
            Identifier.of("baum2", "subspec_bonus/schattenpirscher_attack_damage")
        ));

        definitions.put(ClassSubspec.STURMKLINGE, new SubspecDefinition(
            ClassSubspec.STURMKLINGE,
            "Sturmklinge",
            "Führt ihre Klinge in einem beständigen Sturm aus Hieben.",
            EntityAttributes.ATTACK_SPEED,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            0.10,
            Identifier.of("baum2", "subspec_bonus/sturmklinge_attack_speed")
        ));

        definitions.put(ClassSubspec.SPLITTERRUNE, new SubspecDefinition(
            ClassSubspec.SPLITTERRUNE,
            "Splitterrune",
            "Lädt Runen mit zerschmetternder Wucht auf.",
            EntityAttributes.ATTACK_DAMAGE,
            EntityAttributeModifier.Operation.ADD_VALUE,
            1.5,
            Identifier.of("baum2", "subspec_bonus/splitterrune_attack_damage")
        ));

        definitions.put(ClassSubspec.GLUECKSRUNE, new SubspecDefinition(
            ClassSubspec.GLUECKSRUNE,
            "Glücksrune",
            "Verstärkt das Glück, das dem Runenwirker bereits innewohnt.",
            EntityAttributes.LUCK,
            EntityAttributeModifier.Operation.ADD_VALUE,
            1.0,
            Identifier.of("baum2", "subspec_bonus/gluecksrune_luck")
        ));

        definitions.put(ClassSubspec.WURZELWALL, new SubspecDefinition(
            ClassSubspec.WURZELWALL,
            "Wurzelwall",
            "Verwurzelt sich fest und wehrt jeden Stoß ab.",
            EntityAttributes.KNOCKBACK_RESISTANCE,
            EntityAttributeModifier.Operation.ADD_VALUE,
            0.10,
            Identifier.of("baum2", "subspec_bonus/wurzelwall_knockback_resistance")
        ));

        definitions.put(ClassSubspec.WESENSFUELLE, new SubspecDefinition(
            ClassSubspec.WESENSFUELLE,
            "Wesensfülle",
            "Lässt das eigene Wesen über sich hinauswachsen.",
            EntityAttributes.MAX_HEALTH,
            EntityAttributeModifier.Operation.ADD_VALUE,
            4.0,
            Identifier.of("baum2", "subspec_bonus/wesensfuelle_max_health")
        ));

        return definitions;
    }

    private SubspecRegistry() {
    }
}
