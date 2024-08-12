package lol.sylvie.sswaystones.block;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import lol.sylvie.sswaystones.Waystones;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block WAYSTONE = register(new WaystoneBlock(
            AbstractBlock.Settings.create()
                    .hardness(1.5f)),
            "waystone");

    public static final BlockEntityType<WaystoneBlockEntity> WAYSTONE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Waystones.MOD_ID, "waystone_block_entity"),
            BlockEntityType.Builder.create(WaystoneBlockEntity::new, WAYSTONE).build()
    );

    public static Block register(Block block, String name) {
        // Register the block
        Identifier id = Identifier.of(Waystones.MOD_ID, name);
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void initialize() {
        PolymerBlockUtils.registerBlockEntity(WAYSTONE_BLOCK_ENTITY);
    }
}
