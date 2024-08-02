package lol.sylvie.sswaystones.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import lol.sylvie.sswaystones.block.ModBlocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class WaystoneBlockItem extends BlockItem implements PolymerItem {
    public WaystoneBlockItem(Settings settings) {
        super(ModBlocks.WAYSTONE, settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.STONE_BRICK_WALL;
    }
}
