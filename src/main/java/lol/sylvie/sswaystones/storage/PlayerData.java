/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public ArrayList<String> discoveredWaystones;

    public PlayerData() {
        this(new ArrayList<>());
    }

    public PlayerData(List<String> discoveredWaystones) {
        this.discoveredWaystones = new ArrayList<>(discoveredWaystones);
    }

    public List<String> getDiscoveredWaystones() {
        return discoveredWaystones;
    }

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(Codec.STRING.listOf().fieldOf("discovered_waystones").forGetter(PlayerData::getDiscoveredWaystones))
            .apply(instance, PlayerData::new));

}
