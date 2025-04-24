/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.item;

import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import java.util.List;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.block.WaystoneBlock;
import lol.sylvie.sswaystones.block.WaystoneStyle;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    // This is considered the "default" waystone for backwards compatibility;
    public static final WaystoneBlockItem WAYSTONE = registerWaystoneStyle(ModBlocks.WAYSTONE);

    // Other styles
    public static final WaystoneBlockItem DEEPSLATE_WAYSTONE = registerWaystoneStyle(ModBlocks.DEEPSLATE_WAYSTONE);
    public static final WaystoneBlockItem TUFF_WAYSTONE = registerWaystoneStyle(ModBlocks.TUFF_WAYSTONE);
    public static final WaystoneBlockItem MUD_WAYSTONE = registerWaystoneStyle(ModBlocks.MUD_WAYSTONE);
    public static final WaystoneBlockItem RESIN_WAYSTONE = registerWaystoneStyle(ModBlocks.RESIN_WAYSTONE);
    public static final WaystoneBlockItem SANDSTONE_WAYSTONE = registerWaystoneStyle(ModBlocks.SANDSTONE_WAYSTONE);
    public static final WaystoneBlockItem RED_SANDSTONE_WAYSTONE = registerWaystoneStyle(
            ModBlocks.RED_SANDSTONE_WAYSTONE);
    public static final WaystoneBlockItem PRISMARINE_WAYSTONE = registerWaystoneStyle(ModBlocks.PRISMARINE_WAYSTONE);
    public static final WaystoneBlockItem NETHER_BRICK_WAYSTONE = registerWaystoneStyle(
            ModBlocks.NETHER_BRICK_WAYSTONE);
    public static final WaystoneBlockItem RED_NETHER_BRICK_WAYSTONE = registerWaystoneStyle(
            ModBlocks.RED_NETHER_BRICK_WAYSTONE);
    public static final WaystoneBlockItem BLACKSTONE_WAYSTONE = registerWaystoneStyle(ModBlocks.BLACKSTONE_WAYSTONE);
    public static final WaystoneBlockItem END_STONE_WAYSTONE = registerWaystoneStyle(ModBlocks.END_STONE_WAYSTONE);

    public static final List<WaystoneBlockItem> WAYSTONES = List.of(WAYSTONE, DEEPSLATE_WAYSTONE, TUFF_WAYSTONE,
            MUD_WAYSTONE, RESIN_WAYSTONE, SANDSTONE_WAYSTONE, RED_SANDSTONE_WAYSTONE, PRISMARINE_WAYSTONE,
            NETHER_BRICK_WAYSTONE, RED_NETHER_BRICK_WAYSTONE, BLACKSTONE_WAYSTONE, END_STONE_WAYSTONE);

    public static final Item PORTABLE_WAYSTONE = register(
            new PortableWaystoneItem(
                    new Item.Settings().registryKey(PortableWaystoneItem.KEY).rarity(Rarity.EPIC).maxCount(1)),
            PortableWaystoneItem.ID);

    public static final ItemGroup ITEM_GROUP = PolymerItemGroupUtils.builder()
            .displayName(Text.translatable("itemGroup.sswaystones.item_group"))
            .icon(Items.STONE_BRICK_WALL::getDefaultStack).entries((context, entries) -> {
                for (Item item : WAYSTONES) {
                    entries.add(item);
                }
                entries.add(PORTABLE_WAYSTONE);
            }).build();

    public static Item register(Item item, Identifier identifier) {
        return Registry.register(Registries.ITEM, identifier, item);
    }

    public static WaystoneBlockItem registerWaystoneStyle(WaystoneBlock block) {
        WaystoneStyle style = block.getStyle();
        Item.Settings settings = new Item.Settings().useBlockPrefixedTranslationKey()
                .registryKey(style.getItemRegistryKey()).rarity(Rarity.RARE);
        return (WaystoneBlockItem) register(new WaystoneBlockItem(block, settings), style.getId());
    }

    public static void initialize() {
        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(Waystones.MOD_ID, "item_group"), ITEM_GROUP);
    }
}
