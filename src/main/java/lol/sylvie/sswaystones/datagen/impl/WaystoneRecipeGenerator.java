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
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;

public class WaystoneRecipeGenerator extends FabricRecipeProvider {
    public WaystoneRecipeGenerator(FabricDataOutput output,
            CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup,
            RecipeExporter recipeExporter) {
        return new RecipeGenerator(wrapperLookup, recipeExporter) {
            @Override
            public void generate() {
                for (WaystoneBlockItem item : ModItems.WAYSTONES) {
                    WaystoneStyle style = item.getStyle();
                    createShaped(RecipeCategory.TRANSPORTATION, item, 1).pattern(" E ").pattern("RWR").pattern("BBB")
                            .input('E', Items.ENDER_EYE).input('R', Items.REDSTONE).input('W', style.getWall())
                            .input('B', style.getBase()).group("waystone")
                            .criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter);
                }
            }
        };
    }

    @Override
    public String getName() {
        return "WaystoneRecipeGenerator";
    }
}
