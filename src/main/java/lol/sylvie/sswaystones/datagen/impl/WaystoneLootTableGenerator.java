/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.datagen.impl;

import java.util.concurrent.CompletableFuture;
import lol.sylvie.sswaystones.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;

public class WaystoneLootTableGenerator extends FabricBlockLootTableProvider {
    public WaystoneLootTableGenerator(FabricDataOutput dataOutput,
            CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        for (Block block : ModBlocks.WAYSTONES) {
            dropSelf(block);
        }
    }
}
