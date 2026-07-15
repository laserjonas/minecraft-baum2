package de.baum2dev.baum2.items;

import java.util.Set;

import de.baum2dev.baum2.registry.ModComponents;
import de.baum2dev.baum2.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Applies WeaponTier upgrades to this mod's own weapons (never vanilla items - explicit
 * user requirement). One shared code path for every weapon: the tier lives in a per-stack
 * component (baum2:weapon_tier), so there are no separate "tempered gold sword" Items.
 *
 * <p>What a tier upgrade changes on the stack:
 * <ul>
 * <li>the {@code baum2:weapon_tier} component - the item-definition JSONs select the
 *     tier texture off it directly (silver sheen / gilded shine variants);</li>
 * <li>{@code ENCHANTMENT_GLINT_OVERRIDE = true} - both upgraded tiers glint;</li>
 * <li>{@code ITEM_NAME} - tier prefix, e.g. "Perfect Poison Dagger";</li>
 * <li>{@code ATTRIBUTE_MODIFIERS} - rebuilt from the ITEM's pristine defaults (never from
 *     the stack's current, possibly already-scaled component), with positive attack-damage
 *     modifier values scaled by the tier's multiplier. Negative values are deliberately NOT
 *     scaled: Drevathis's Cursed Blade has -1.0 attack damage by design (pure proc weapon),
 *     and scaling that would make upgrades a downgrade - its tiers are visual/name-only
 *     until its own upgrade identity is designed.</li>
 * </ul>
 *
 * Espenklinge limitation: its GUI sprite switches per tier, but its GeckoLib in-hand
 * texture (espenklinge_geo.png) stays the base one - per-tier GeckoLib textures are a
 * separate, larger task (see HANDOFF.md).
 */
public class WeaponUpgradeSystem {

    /** This mod's own weapons only - the upgrade recipe matches nothing else. */
    private static final Set<Item> UPGRADEABLE_WEAPONS = Set.of(
            ModItems.GOLD_SWORD,
            ModItems.POISON_DAGGER,
            ModItems.COLOSSAL_WARCLUB,
            ModItems.DREVATHIS_CURSED_BLADE,
            ModItems.ESPENKLINGE
    );

    public static boolean isUpgradeableWeapon(ItemStack stack) {
        return UPGRADEABLE_WEAPONS.contains(stack.getItem());
    }

    public static WeaponTier getTier(ItemStack stack) {
        return WeaponTier.fromComponentValue(stack.get(ModComponents.WEAPON_TIER));
    }

    /** @return an upgraded copy (count 1) of the given weapon, or EMPTY if not upgradeable. */
    public static ItemStack upgraded(ItemStack weapon) {
        if (!isUpgradeableWeapon(weapon)) {
            return ItemStack.EMPTY;
        }
        return getTier(weapon).next()
                .map(nextTier -> {
                    ItemStack result = weapon.copyWithCount(1);
                    applyTier(result, nextTier);
                    return result;
                })
                .orElse(ItemStack.EMPTY);
    }

    private static void applyTier(ItemStack stack, WeaponTier tier) {
        stack.set(ModComponents.WEAPON_TIER, tier.componentValue());
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponentTypes.ITEM_NAME,
                Text.literal(tier.displayPrefix() + " ").append(stack.getItem().getName()));
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, scaledModifiers(stack, tier));
    }

    /**
     * The item's own DEFAULT modifiers (pristine - not the stack's current component, which
     * a previous tier already scaled) with positive attack-damage values multiplied.
     * Reusing each modifier's own stable Identifier keeps vanilla's BASE_ATTACK_DAMAGE
     * bookkeeping (tooltip merging with the player's own attack attribute) intact.
     */
    private static AttributeModifiersComponent scaledModifiers(ItemStack stack, WeaponTier tier) {
        AttributeModifiersComponent defaults = stack.getItem().getComponents()
                .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        AttributeModifiersComponent.Builder scaled = AttributeModifiersComponent.builder();
        for (AttributeModifiersComponent.Entry entry : defaults.modifiers()) {
            EntityAttributeModifier modifier = entry.modifier();
            double value = modifier.value();
            // .equals, not the deprecated RegistryEntry.matches(RegistryEntry) - registry
            // Reference entries are canonical, one instance per key.
            if (entry.attribute().equals(EntityAttributes.ATTACK_DAMAGE) && value > 0) {
                value *= tier.damageMultiplier();
            }
            scaled.add(entry.attribute(),
                    new EntityAttributeModifier(modifier.id(), value, modifier.operation()),
                    entry.slot());
        }
        return scaled.build();
    }
}
