/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.datagen.impl;

import java.util.concurrent.CompletableFuture;
import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.block.WaystoneBlock;
import lol.sylvie.sswaystones.block.WaystoneStyle;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;

public class WaystoneEnglishProvider extends FabricLanguageProvider {
    public WaystoneEnglishProvider(FabricDataOutput dataOutput,
            CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider wrapperLookup, TranslationBuilder translationBuilder) {
        for (WaystoneBlock block : ModBlocks.WAYSTONES) {
            WaystoneStyle style = block.getStyle();
            if (style == WaystoneStyle.STONE)
                continue;
            String baseName = Component.translatable(style.getBase().getDescriptionId()).getString();

            if (baseName.endsWith("s"))
                baseName = baseName.substring(0, baseName.length() - 1);

            translationBuilder.add(block.getDescriptionId(), String.format("%s %s", baseName, "Waystone"));
        }
    }
}
