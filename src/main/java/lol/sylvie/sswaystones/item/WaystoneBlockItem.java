/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import java.util.List;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.block.ModBlocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

public class WaystoneBlockItem extends BlockItem implements PolymerItem {
    public static final Identifier ID = Waystones.id("waystone");
    public static final RegistryKey<Item> KEY = RegistryKey.of(Registries.ITEM.getKey(), ID);

    public WaystoneBlockItem(Settings settings) {
        super(ModBlocks.WAYSTONE, settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
        return Items.STONE_BRICK_WALL;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        ItemStack out = PolymerItemUtils.createItemStack(itemStack, tooltipType, context);
        out.set(DataComponentTypes.ITEM_MODEL, Identifier.ofVanilla("stone_brick_wall"));
        out.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                new CustomModelDataComponent(List.of(), List.of(), List.of(ID.toString()), List.of()));
        return out;
    }
}
