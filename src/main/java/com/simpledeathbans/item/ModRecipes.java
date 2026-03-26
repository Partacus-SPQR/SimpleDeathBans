package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

/**
 * Registers custom recipes for the mod.
 * Note: In modern Fabric, recipes are typically defined via JSON in data packs.
 * This class provides programmatic registration as a fallback.
 */
public class ModRecipes {
    
    public static void register() {
        // Recipe is defined in data/simpledeathbans/recipe/resurrection_totem.json
        SimpleDeathBans.LOGGER.info("Recipe registration initialized");
    }
}
