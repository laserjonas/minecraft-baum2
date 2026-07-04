package de.baum2dev.baum2.classes;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Immutable data for one playable class: display text plus its single passive stat bonus.
 */
public record ClassDefinition(
    PlayerClass playerClass,
    String displayName,
    String description,
    RegistryEntry<EntityAttribute> bonusAttribute,
    EntityAttributeModifier.Operation bonusOperation,
    double bonusAmount,
    Identifier bonusModifierId
) {
    public EntityAttributeModifier toModifier() {
        return new EntityAttributeModifier(bonusModifierId, bonusAmount, bonusOperation);
    }
}
