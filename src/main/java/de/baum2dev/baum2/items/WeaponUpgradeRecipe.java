package de.baum2dev.baum2.items;

import de.baum2dev.baum2.registry.ModRecipeSerializers;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Placeholder upgrade recipe (explicit user decision for v1): one of this mod's own weapons
 * ALONE anywhere in a crafting grid -> the same weapon at the next WeaponTier. "Specific
 * craft recipes" with real material costs are a planned follow-up; only this class and its
 * data file (data/baum2/recipe/weapon_upgrade.json) need replacing for that - the tier
 * logic itself lives in WeaponUpgradeSystem.
 *
 * <p>matches()/craft() run fully server-side (crafting result preview is
 * server-authoritative in 1.21.11 - see docs/fabric-modding.md "Per-ItemStack weapon
 * upgrade tier", item 5).
 */
public class WeaponUpgradeRecipe extends SpecialCraftingRecipe {

    public WeaponUpgradeRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return input.getStackCount() == 1 && !WeaponUpgradeSystem.upgraded(findSingleStack(input)).isEmpty();
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return WeaponUpgradeSystem.upgraded(findSingleStack(input));
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return ModRecipeSerializers.WEAPON_UPGRADE;
    }

    private static ItemStack findSingleStack(CraftingRecipeInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
