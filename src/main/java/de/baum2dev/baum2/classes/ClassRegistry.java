package de.baum2dev.baum2.classes;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;

/**
 * Static lookup for the 4 playable classes and their passive bonuses. Not final balance —
 * see HANDOFF.md for the pending balance-review step.
 */
public final class ClassRegistry {
    private static final Map<PlayerClass, ClassDefinition> DEFINITIONS = build();

    public static ClassDefinition get(PlayerClass playerClass) {
        return DEFINITIONS.get(playerClass);
    }

    public static Collection<ClassDefinition> all() {
        return DEFINITIONS.values();
    }

    private static Map<PlayerClass, ClassDefinition> build() {
        Map<PlayerClass, ClassDefinition> definitions = new EnumMap<>(PlayerClass.class);

        definitions.put(PlayerClass.EISENWAECHTER, new ClassDefinition(
            PlayerClass.EISENWAECHTER,
            "Eisenwächter",
            "Ein standhafter Beschützer mit erhöhter Lebenskraft.",
            EntityAttributes.MAX_HEALTH,
            EntityAttributeModifier.Operation.ADD_VALUE,
            4.0,
            Identifier.of("baum2", "class_bonus/eisenwaechter_max_health")
        ));

        definitions.put(PlayerClass.SCHATTENLAEUFER, new ClassDefinition(
            PlayerClass.SCHATTENLAEUFER,
            "Schattenläufer",
            "Ein flinker Kämpfer mit erhöhter Bewegungsgeschwindigkeit.",
            EntityAttributes.MOVEMENT_SPEED,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            0.10,
            Identifier.of("baum2", "class_bonus/schattenlaeufer_movement_speed")
        ));

        definitions.put(PlayerClass.RUNENWIRKER, new ClassDefinition(
            PlayerClass.RUNENWIRKER,
            "Runenwirker",
            "Ein Kenner alter Runen mit gesteigertem Glück.",
            EntityAttributes.LUCK,
            EntityAttributeModifier.Operation.ADD_VALUE,
            1.0,
            Identifier.of("baum2", "class_bonus/runenwirker_luck")
        ));

        definitions.put(PlayerClass.SEELENHUETER, new ClassDefinition(
            PlayerClass.SEELENHUETER,
            "Seelenhüter",
            "Ein Wächter der Seelen mit zusätzlicher Absorption.",
            EntityAttributes.MAX_ABSORPTION,
            EntityAttributeModifier.Operation.ADD_VALUE,
            4.0,
            Identifier.of("baum2", "class_bonus/seelenhueter_max_absorption")
        ));

        return definitions;
    }

    private ClassRegistry() {
    }
}
