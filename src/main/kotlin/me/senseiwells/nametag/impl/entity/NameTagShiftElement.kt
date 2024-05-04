package me.senseiwells.nametag.impl.entity

import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils
import me.senseiwells.nametag.impl.ShiftHeight
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.function.Consumer

class NameTagShiftElement {
    private val uuid: UUID = UUID.randomUUID()
    val id = VirtualEntityUtils.requestEntityId()

    fun sendSpawnPackets(
        position: Vec3,
        consumer: Consumer<Packet<ClientGamePacketListener>>,
        shift: ShiftHeight = ShiftHeight.SMALL,
    ) {
        shift.applySpawnPackets(this.id, this.uuid, position, consumer)
    }
}