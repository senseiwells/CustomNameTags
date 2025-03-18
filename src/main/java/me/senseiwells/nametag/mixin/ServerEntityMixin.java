package me.senseiwells.nametag.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.senseiwells.nametag.impl.NameTagUtils;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow @Final private Entity entity;

    @ModifyExpressionValue(
        method = "sendPairingData",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z",
            ordinal = 1
        )
    )
    private boolean shouldNotSendPassengers(boolean original) {
        if (original) {
            return !(this.entity instanceof ServerPlayer player) || NameTagUtils.getNameTags(player).isEmpty();
        }
        return false;
    }
}
