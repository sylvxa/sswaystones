/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.datagen.impl;

import java.util.concurrent.CompletableFuture;
import lol.sylvie.sswaystones.block.WaystoneStyle;
import lol.sylvie.sswaystones.item.ModItems;
import lol.sylvie.sswaystones.item.WaystoneBlockItem;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

public class WaystoneRecipeGenerator extends FabricRecipeProvider {
    public WaystoneRecipeGenerator(FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider wrapperLookup,
            RecipeOutput recipeExporter) {
        return new RecipeProvider(wrapperLookup, recipeExporter) {
            @Override
            public void buildRecipes() {
                for (WaystoneBlockItem item : ModItems.WAYSTONES) {
                    WaystoneStyle style = item.getStyle();
                    shaped(RecipeCategory.TRANSPORTATION, item, 1).pattern(" E ").pattern("RWR").pattern("BBB")
                            .define('E', Items.ENDER_EYE).define('R', Items.REDSTONE).define('W', style.getWall())
                            .define('B', style.getBase()).group("waystone")
                            .unlockedBy(getHasName(item), has(item)).save(output);
                }
            }
        };
    }

    @Override
    public String getName() {
        return "WaystoneRecipeGenerator";
    }
}
