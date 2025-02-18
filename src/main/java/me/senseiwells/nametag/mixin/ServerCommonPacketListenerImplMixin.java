package me.senseiwells.nametag.mixin;

import me.senseiwells.nametag.impl.NameTagUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @ModifyVariable(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Packet<?> modifyPacket(Packet<?> packet) {
        if ((Object) this instanceof ServerGamePacketListenerImpl connection) {
            ServerPlayer player = connection.player;
            if (packet instanceof ClientboundSetPassengersPacket passengersPacket) {
                return NameTagUtils.modifyPassengersPacket(player, passengersPacket);
            }
        }

        return packet;
    }
}
