/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import java.util.List;
import lol.sylvie.sswaystones.block.WaystoneBlock;
import lol.sylvie.sswaystones.block.WaystoneStyle;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import xyz.nucleoid.packettweaker.PacketContext;

public class WaystoneBlockItem extends BlockItem implements PolymerItem {
    private final WaystoneStyle style;

    public WaystoneBlockItem(WaystoneBlock block, Properties settings) {
        super(block, settings);
        this.style = block.getStyle();
    }

    public WaystoneStyle getStyle() {
        return style;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
        return style.getWall().asItem();
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        ItemStack out = PolymerItemUtils.createItemStack(itemStack, tooltipType, context);
        out.set(DataComponents.ITEM_MODEL, style.getWallId());
        out.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(style.getId().toString()), List.of()));
        return out;
    }
}
