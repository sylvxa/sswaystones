/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.item;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.gui.ViewerUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import xyz.nucleoid.packettweaker.PacketContext;

public class PortableWaystoneItem extends SimplePolymerItem {
    public static final Identifier ID = Waystones.id("portable_waystone");
    public static final ResourceKey<Item> KEY = ResourceKey.create(BuiltInRegistries.ITEM.key(), ID);

    public PortableWaystoneItem(Properties settings) {
        super(settings, Items.ENDER_EYE);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (user instanceof ServerPlayer player)
            ViewerUtil.openGui(player, null);

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        ItemStack out = PolymerItemUtils.createItemStack(itemStack, tooltipType, context);
        out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        out.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(ID.toString()), List.of()));
        return out;
    }
}
