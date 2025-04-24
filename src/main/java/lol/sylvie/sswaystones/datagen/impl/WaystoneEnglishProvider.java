package lol.sylvie.sswaystones.datagen.impl;

import lol.sylvie.sswaystones.block.ModBlocks;
import lol.sylvie.sswaystones.block.WaystoneBlock;
import lol.sylvie.sswaystones.block.WaystoneStyle;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.data.DataOutput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class WaystoneEnglishProvider extends FabricLanguageProvider {
    public WaystoneEnglishProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup, TranslationBuilder translationBuilder) {
        for (WaystoneBlock block : ModBlocks.WAYSTONES) {
            WaystoneStyle style = block.getStyle();
            if (style == WaystoneStyle.STONE) continue;
            String baseName = Text.translatable(style.getBase().getTranslationKey()).getString();

            if (baseName.endsWith("s"))
                baseName = baseName.substring(0, baseName.length() - 1);

            translationBuilder.add(block.getTranslationKey(), String.format("%s %s", baseName, "Waystone"));
        }
    }
}
