package lol.sylvie.sswaystones.block;

import lol.sylvie.sswaystones.Waystones;
import lol.sylvie.sswaystones.item.WaystoneBlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    public static final Item WAYSTONE = register(new WaystoneBlockItem(
            new Item.Settings().rarity(Rarity.RARE)),
            "waystone");

    public static Item register(Item item, String name) {
        // Register the item
        Identifier id = Identifier.of(Waystones.MOD_ID, name);

        return Registry.register(Registries.ITEM, id, item);
    }

    public static void initialize() {}
}
