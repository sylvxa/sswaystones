/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.datagen;

import lol.sylvie.sswaystones.datagen.impl.WaystoneLootTableGenerator;
import lol.sylvie.sswaystones.datagen.impl.WaystoneRecipeGenerator;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class WaystoneDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator dataGenerator) {
        FabricDataGenerator.Pack pack = dataGenerator.createPack();
        pack.addProvider(WaystoneRecipeGenerator::new);
        pack.addProvider(WaystoneLootTableGenerator::new);
        // Disabled since it's hard to merge static and dynamic json files :(
        // pack.addProvider(WaystoneEnglishProvider::new);
    }
}
