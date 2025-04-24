/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.datagen.mixin;

import lol.sylvie.sswaystones.Waystones;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.data.DataOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FabricLanguageProvider.class)
public class FabricLanguageProviderMixin {
    @Shadow
    @Final
    protected FabricDataOutput dataOutput;

    // For some reason, Fabric API has no way to change the output folder without
    // some weird hacks like this or rewriting a bunch of code.
    @ModifyArg(method = "getLangFilePath", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/api/datagen/v1/FabricDataOutput;getResolver(Lnet/minecraft/data/DataOutput$OutputType;Ljava/lang/String;)Lnet/minecraft/data/DataOutput$PathResolver;"))
    private DataOutput.OutputType forceDataLanguageGeneration(DataOutput.OutputType type) {
        if (!dataOutput.getModId().equals(Waystones.MOD_ID))
            return type;
        return DataOutput.OutputType.DATA_PACK;
    }
}
