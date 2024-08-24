/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;

public class NameGenerator {
    private static final String[] NAME_FIRST = readResourceAsString("assets/sswaystones/lang/name_first.txt")
            .replace("\r", "").split("\n");
    private static final String[] NAME_LAST = readResourceAsString("assets/sswaystones/lang/name_last.txt")
            .replace("\r", "").split("\n");

    private static String readResourceAsString(String path) {
        try (InputStream stream = NameGenerator.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null)
                return "MISSINGNO";
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "MISSINGNO";
        }
    }

    public static String generateName() {
        Random random = new Random();
        int firstIndex = random.nextInt(0, NAME_FIRST.length);
        int lastIndex = random.nextInt(0, NAME_LAST.length);
        return StringUtils.capitalize(NAME_FIRST[firstIndex] + NAME_LAST[lastIndex]);
    }
}
