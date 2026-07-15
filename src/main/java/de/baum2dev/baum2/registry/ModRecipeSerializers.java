package de.baum2dev.baum2.registry;

import de.baum2dev.baum2.items.WeaponUpgradeRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipeSerializers {

    /**
     * Backs data/baum2/recipe/weapon_upgrade.json ({"type": "baum2:weapon_upgrade"}).
     * SpecialRecipeSerializer's codec only carries the crafting-book category - all
     * matching logic is code in WeaponUpgradeRecipe.
     */
    public static final RecipeSerializer<WeaponUpgradeRecipe> WEAPON_UPGRADE = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of("baum2", "weapon_upgrade"),
            new SpecialCraftingRecipe.SpecialRecipeSerializer<>(WeaponUpgradeRecipe::new)
    );

    /** No-op - calling this forces this class (and its registrations) to load during mod init. */
    public static void bootstrap() {
    }
}
