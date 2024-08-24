/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.item;

import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import lol.sylvie.sswaystones.Waystones;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    public static final Item WAYSTONE = register(new WaystoneBlockItem(new Item.Settings().rarity(Rarity.RARE)),
            "waystone");

    public static final Item PORTABLE_WAYSTONE = register(
            new PortableWaystoneItem(new Item.Settings().rarity(Rarity.EPIC).maxCount(1)), "portable_waystone");

    public static final ItemGroup ITEM_GROUP = PolymerItemGroupUtils.builder()
            .displayName(Text.translatable("itemGroup.sswaystones.item_group"))
            .icon(Items.STONE_BRICK_WALL::getDefaultStack).entries((context, entries) -> {
                entries.add(WAYSTONE);
                entries.add(PORTABLE_WAYSTONE);
            }).build();

    public static Item register(Item item, String name) {
        // Register the item
        Identifier id = Identifier.of(Waystones.MOD_ID, name);

        return Registry.register(Registries.ITEM, id, item);
    }

    public static void initialize() {
        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(Waystones.MOD_ID, "item_group"), ITEM_GROUP);
    }
}
