package me.senseiwells.nametag.impl.entity

import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils
import me.senseiwells.nametag.impl.ShiftHeight
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.function.Consumer

class NameTagShiftElement {
    private val uuid: UUID = UUID.randomUUID()
    val id = VirtualEntityUtils.requestEntityId()

    fun sendSpawnPackets(
        position: Vec3,
        consumer: Consumer<Packet<ClientGamePacketListener>>,
        shift: ShiftHeight = ShiftHeight.Medium,
    ) {
        consumer.accept(ClientboundAddEntityPacket(
            this.id,
            this.uuid,
            position.x,
            position.y,
            position.z,
            0.0F,
            0.0F,
            shift.type,
            0,
            Vec3.ZERO,
            0.0
        ))

        val packet = shift.createDataPacket(this.id)
        if (packet != null) {
            consumer.accept(packet)
        }
    }
}