/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.datagen.impl;

import java.util.concurrent.CompletableFuture;
import lol.sylvie.sswaystones.block.WaystoneStyle;
import lol.sylvie.sswaystones.item.ModItems;
import lol.sylvie.sswaystones.item.WaystoneBlockItem;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

public class WaystoneRecipeGenerator extends FabricRecipeProvider {
    public WaystoneRecipeGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes,
            BootstrapContext<Advancement> advancements) {
        return new RecipeProvider(recipes, advancements) {
            @Override
            public void buildRecipes() {
                for (WaystoneBlockItem item : ModItems.WAYSTONES) {
                    WaystoneStyle style = item.getStyle();
                    shaped(RecipeCategory.TRANSPORTATION, item, 1).pattern(" E ").pattern("RWR").pattern("BBB")
                            .define('E', Items.ENDER_EYE).define('R', Items.REDSTONE).define('W', style.getWall())
                            .define('B', style.getBase()).group("waystone").unlockedBy(getHasName(item), has(item))
                            .save(output);
                }
            }
        };
    }

    @Override
    public String getName() {
        return "WaystoneRecipeGenerator";
    }
}
