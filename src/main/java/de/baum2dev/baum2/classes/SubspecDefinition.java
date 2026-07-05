package de.baum2dev.baum2.classes;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Immutable data for one class sub-specialization: display text plus its single passive
 * stat bonus. Mirrors {@link ClassDefinition}'s shape.
 */
public record SubspecDefinition(
    ClassSubspec subspec,
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
