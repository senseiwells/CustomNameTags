package me.senseiwells.nametag.mixin;

import me.senseiwells.nametag.impl.NameTagUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

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
            if (packet instanceof ClientboundBundlePacket bundlePacket) {
                List<Packet<? super ClientGamePacketListener>> copy = new ArrayList<>();
                for (Packet<? super ClientGamePacketListener> sub : bundlePacket.subPackets()) {
                    if (sub instanceof ClientboundSetPassengersPacket passengersPacket) {
                        copy.add(NameTagUtils.modifyPassengersPacket(player, passengersPacket));
                    } else {
                        copy.add(sub);
                    }
                }
                return new ClientboundBundlePacket(copy);
            }
        }

        return packet;
    }
}
