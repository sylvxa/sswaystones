/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.item;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.gui.ViewerUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

public class PortableWaystoneItem extends SimplePolymerItem {
    public static final Identifier ID = Waystones.id("portable_waystone");
    public static final RegistryKey<Item> KEY = RegistryKey.of(Registries.ITEM.getKey(), ID);

    public PortableWaystoneItem(Settings settings) {
        super(settings, Items.ENDER_EYE);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user instanceof ServerPlayerEntity player)
            ViewerUtil.openGui(player, null);

        return ActionResult.SUCCESS;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        ItemStack out = PolymerItemUtils.createItemStack(itemStack, tooltipType, context);
        out.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return out;
    }
}
