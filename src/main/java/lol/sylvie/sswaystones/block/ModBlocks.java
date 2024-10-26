/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import lol.sylvie.sswaystones.Waystones;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block WAYSTONE = register(
            new WaystoneBlock(AbstractBlock.Settings.create().registryKey(WaystoneBlock.KEY).hardness(1.5f)),
            WaystoneBlock.ID);

    public static final BlockEntityType<WaystoneBlockEntity> WAYSTONE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Identifier.of(Waystones.MOD_ID, "waystone_block_entity"),
            FabricBlockEntityTypeBuilder.create(WaystoneBlockEntity::new, WAYSTONE).build());

    public static Block register(Block block, Identifier identifier) {
        return Registry.register(Registries.BLOCK, identifier, block);
    }

    public static void initialize() {
        PolymerBlockUtils.registerBlockEntity(WAYSTONE_BLOCK_ENTITY);
    }
}
