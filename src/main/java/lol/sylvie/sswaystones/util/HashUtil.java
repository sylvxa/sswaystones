/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lol.sylvie.sswaystones.storage.WaystoneRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class HashUtil {
    public static String waystoneIdentifier(BlockPos pos, ResourceKey<Level> world) {
        return String.format("<x:%s, y:%s, z:%s, w:%s>", pos.getX(), pos.getY(), pos.getZ(), world.identifier());
    }

    public static String bytesToHex(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (byte b : data) {
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }

    public static String getHash(BlockPos pos, ResourceKey<Level> world) {
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
