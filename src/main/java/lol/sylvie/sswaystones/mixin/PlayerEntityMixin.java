/*
  This file is licensed under the MIT License!
  https://github.com/sylvxa/sswaystones/blob/main/LICENSE
*/
package lol.sylvie.sswaystones.mixin;

import static lol.sylvie.sswaystones.Waystones.combatTimestamps;

import lol.sylvie.sswaystones.Waystones;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin {
    @Unique
    private static boolean isCombat(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity == null)
            return false;
        if (Waystones.configuration.getInstance().pveCombat)
            return entity.isAlive();
        return source.is(DamageTypeTags.IS_PLAYER_ATTACK) || entity.isAlwaysTicking();
    }

    @Inject(method = "hurtServer", at = @At("TAIL"))
    public void updateCombatTimer(ServerLevel world, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        Player thisPlayer = (Player) (Object) this;
        if (thisPlayer.equals(source.getEntity()) || !isCombat(source))
            return;

        long timestamp = System.currentTimeMillis();
        combatTimestamps.put(thisPlayer.getUUID(), timestamp);
        if (source.getEntity() instanceof Player player)
            combatTimestamps.put(player.getUUID(), timestamp);
    }
}
