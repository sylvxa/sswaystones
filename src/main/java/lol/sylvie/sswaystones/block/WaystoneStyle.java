/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.block;

import java.util.Optional;
import lol.sylvie.sswaystones.Waystones;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallBlock;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

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
        Optional<RegistryKey<Block>> entry = Registries.BLOCK.getEntry(this.getBase()).getKey();
        if (entry.isEmpty())
            return Identifier.ofVanilla("stone_brick");
        return entry.get().getValue();
    }

    public WallBlock getWall() {
        return wall;
    }

    public Identifier getWallId() {
        Optional<RegistryKey<Item>> entry = Registries.ITEM.getEntry(this.getWall().asItem()).getKey();
        if (entry.isEmpty())
            return Identifier.ofVanilla("stone_brick_wall");
        return entry.get().getValue();
    }

    public Identifier getId() {
        if (this == STONE) {
            return Waystones.id("waystone");
        }

        return Waystones.id(getBaseId().getPath() + "_waystone");
    }

    public RegistryKey<Block> getBlockRegistryKey() {
        return RegistryKey.of(Registries.BLOCK.getKey(), getId());
    }

    public RegistryKey<Item> getItemRegistryKey() {
        return RegistryKey.of(Registries.ITEM.getKey(), getId());
    }
}
