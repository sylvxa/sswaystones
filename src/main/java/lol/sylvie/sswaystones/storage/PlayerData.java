/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class PlayerData {
    public List<String> discoveredWaystones = new ArrayList<>();

    public NbtCompound toNbt() {
        NbtCompound playerTag = new NbtCompound();

        NbtList discoveredList = new NbtList();
        discoveredWaystones.forEach((hash) -> discoveredList.add(NbtString.of(hash)));
        playerTag.put("discovered_waystones", discoveredList);

        return playerTag;
    }

    public static PlayerData fromNbt(NbtCompound nbt) {
        PlayerData data = new PlayerData();

        NbtList nbtDiscoveredList = nbt.getList("discovered_waystones", NbtElement.STRING_TYPE);
        data.discoveredWaystones = nbtDiscoveredList.stream().map(NbtElement::asString)
                .collect(Collectors.toCollection(ArrayList::new));

        return data;
    }
}
