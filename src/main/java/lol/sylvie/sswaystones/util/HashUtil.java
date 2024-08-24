/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HashUtil {
    public static String waystoneIdentifier(BlockPos pos, RegistryKey<World> world) {
        return String.format("<x:%s, y:%s, z:%s, w:%s>", pos.getX(), pos.getY(), pos.getZ(), world.getValue());
    }

    public static String bytesToHex(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (byte b : data) {
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }

    public static String getHash(BlockPos pos, RegistryKey<World> world) {
        String identifier = waystoneIdentifier(pos, world);
        try {
            return bytesToHex(MessageDigest.getInstance("SHA-256").digest(identifier.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Couldn't generate hash for waystone record!");
        }
    }

    public static String getHash(WaystoneRecord record) {
        return getHash(record.getPos(), record.getWorldKey());
    }
}
