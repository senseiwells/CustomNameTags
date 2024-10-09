package me.senseiwells.nametag.impl.entity

import com.google.common.collect.MapMaker
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

    init {
        shifts[this.id] = this
    }

    fun sendSpawnPackets(
        position: Vec3,
        consumer: Consumer<Packet<ClientGamePacketListener>>,
        shift: ShiftHeight = ShiftHeight.SMALL,
    ) {
        shift.applySpawnPackets(this.id, this.uuid, position, consumer)
    }

    companion object {
        private val shifts = MapMaker().weakValues().makeMap<Int, NameTagShiftElement>()

        @JvmStatic
        fun isNameTagShift(id: Int): Boolean {
             return this.shifts.containsKey(id)
        }
    }
}