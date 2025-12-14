/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import java.util.Optional;
import lol.sylvie.sswaystones.Waystones;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;

public enum WaystoneStyle {
    STONE(Blocks.STONE_BRICKS, Blocks.STONE_BRICK_WALL), DEEPSLATE(Blocks.DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_BRICK_WALL), TUFF(Blocks.TUFF_BRICKS, Blocks.TUFF_BRICK_WALL), MUD(Blocks.MUD_BRICKS,
                    Blocks.MUD_BRICK_WALL), RESIN(Blocks.RESIN_BRICKS, Blocks.RESIN_BRICK_WALL), SANDSTONE(
                            Blocks.SANDSTONE, Blocks.SANDSTONE_WALL), RED_SANDSTONE(Blocks.RED_SANDSTONE,
                                    Blocks.RED_SANDSTONE_WALL), PRISMARINE(Blocks.PRISMARINE,
                                            Blocks.PRISMARINE_WALL), NETHER_BRICK(Blocks.NETHER_BRICKS,
                                                    Blocks.NETHER_BRICK_WALL), RED_NETHER_BRICK(
                                                            Blocks.RED_NETHER_BRICKS,
                                                            Blocks.RED_NETHER_BRICK_WALL), BLACKSTONE(
                                                                    Blocks.POLISHED_BLACKSTONE_BRICKS,
                                                                    Blocks.POLISHED_BLACKSTONE_WALL), END_STONE(
                                                                            Blocks.END_STONE_BRICKS,
                                                                            Blocks.END_STONE_BRICK_WALL);

    private final Block base;
    private final WallBlock wall;

    WaystoneStyle(Block base, Block wall) {
        this.base = base;
        this.wall = (WallBlock) wall;
    }

    public Block getBase() {
        return base;
    }

    public Identifier getBaseId() {
        Optional<ResourceKey<Block>> entry = BuiltInRegistries.BLOCK.wrapAsHolder(this.getBase()).unwrapKey();
        return entry.map(ResourceKey::identifier).orElseGet(() -> Identifier.withDefaultNamespace("stone_brick"));
    }

    public WallBlock getWall() {
        return wall;
    }

    public Identifier getWallId() {
        Optional<ResourceKey<Item>> entry = BuiltInRegistries.ITEM.wrapAsHolder(this.getWall().asItem()).unwrapKey();
        return entry.map(ResourceKey::identifier).orElseGet(() -> Identifier.withDefaultNamespace("stone_brick_wall"));
    }

    public Identifier getId() {
        if (this == STONE) {
            return Waystones.id("waystone");
        }

        return Waystones.id(getBaseId().getPath() + "_waystone");
    }

    public ResourceKey<Block> getBlockRegistryKey() {
        return ResourceKey.create(BuiltInRegistries.BLOCK.key(), getId());
    }

    public ResourceKey<Item> getItemRegistryKey() {
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), getId());
    }
}
