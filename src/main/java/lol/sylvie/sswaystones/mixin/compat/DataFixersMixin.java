/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.mixin.compat;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.schemas.Schema;
import lol.sylvie.sswaystones.compat.WaystoneStorageFileFix;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.filefix.FileFixerUpper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// TODO: Fabric API should have something for this sometime soon
// See https://github.com/FabricMC/fabric-api/pull/5257
@Mixin(DataFixers.class)
public abstract class DataFixersMixin {
    @Definition(id = "addSchema", method = "Lnet/minecraft/util/filefix/FileFixerUpper$Builder;addSchema(Lcom/mojang/datafixers/DataFixerBuilder;ILjava/util/function/BiFunction;)Lcom/mojang/datafixers/schemas/Schema;")
    @Expression("?.addSchema(?, 4772, ?)")
    @ModifyExpressionValue(method = "addFixers", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static Schema sswaystones$addFileFix(Schema original,
            @Local(argsOnly = true) FileFixerUpper.Builder fileFixerUpper) {
        fileFixerUpper.addFixer(new WaystoneStorageFileFix(original));
        return original;
    }
}
