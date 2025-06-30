/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.mixin;

import static lol.sylvie.sswaystones.Waystones.combatTimestamps;

import lol.sylvie.sswaystones.Waystones;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Unique
    private static boolean isCombat(DamageSource source) {
        Entity entity = source.getAttacker();
        if (entity == null)
            return false;
        if (Waystones.configuration.getInstance().pveCombat)
            return entity.isAlive();
        return source.isIn(DamageTypeTags.IS_PLAYER_ATTACK) || entity.isPlayer();
    }

    @Inject(method = "damage", at = @At("TAIL"))
    public void updateCombatTimer(ServerWorld world, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity thisPlayer = (PlayerEntity) (Object) this;
        if (thisPlayer.equals(source.getAttacker()) || !isCombat(source))
            return;

        long timestamp = System.currentTimeMillis();
        combatTimestamps.put(thisPlayer.getUuid(), timestamp);
        if (source.getAttacker() instanceof PlayerEntity player)
            combatTimestamps.put(player.getUuid(), timestamp);
    }
}
