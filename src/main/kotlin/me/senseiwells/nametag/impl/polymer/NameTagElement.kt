package me.senseiwells.nametag.impl.polymer

import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.nametag.impl.nametags.NameTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.function.Consumer

class NameTagElement(
    private val owner: NameTagHolder,
    val tag: NameTag
) {
    private val background = TextDisplayElement()
    private val foreground = TextDisplayElement()

    private var ticks = 0

    private val player: ServerPlayer
        get() = this.owner.player

    private val watching = Object2ObjectOpenHashMap<UUID, NameTagPointers>()

    private val shift = NameTagShiftElement()

    init {
        this.initialiseDisplay(this.background)
        this.initialiseDisplay(this.foreground)

        this.background.seeThrough = true
        this.background.textOpacity = 30.toByte()
        this.foreground.seeThrough = false
        this.foreground.textOpacity = 255.toByte()
        this.foreground.setBackground(0)
    }

    fun watch(player: ServerPlayer, previous: NameTagElement? = null) {
        if (this.isWatching(player)) {
            return
        }
        val pointers = NameTagPointers()
        pointers.previous = previous

        this.watching[player.uuid] = pointers
        this.sendWatchPackets(player, pointers, Consumer(player.connection::send))

        if (previous != null) {
            val previousPointers = previous.watching[player.uuid]
                ?: throw IllegalStateException("Previous pointers were somehow null??")
            pointers.next = previousPointers.next
            val next = pointers.next
            if (next != null) {
                val nextPointers = next.watching[player.uuid]
                    ?: throw IllegalStateException("Next pointers were somehow null?")
                next.sendRidePacket(nextPointers, Consumer(player.connection::send))
            }
        }
    }

    fun unwatch(player: ServerPlayer) {
        val pointers = this.watching.remove(player.uuid) ?: return
        player.connection.send(ClientboundRemoveEntitiesPacket(
            this.background.entityId,
            this.foreground.entityId
        ))

        val previous = pointers.previous
        if (previous != null) {
            // We added our own anchor for this nametag
            player.connection.send(ClientboundRemoveEntitiesPacket(this.shift.entityId))

            val previousPointers = previous.watching[player.uuid]
                ?: throw IllegalStateException("Previous pointers were somehow null??")
            previousPointers.next = pointers.next
        }

        val next = pointers.next
        if (next != null) {
            val nextPointers = next.watching[player.uuid]
                ?: throw IllegalStateException("Next pointers were somehow null??")
            nextPointers.previous = previous

            next.sendRidePacket(nextPointers, Consumer(player.connection::send))
        }
    }

    fun isWatching(player: ServerPlayer): Boolean {
        return this.watching.contains(player.uuid)
    }

    fun sneak() {
        // When the player sneaks, the background becomes
        // non-see-through and the foreground becomes invisible
        this.background.seeThrough = false
        this.foreground.textOpacity = -127
    }

    fun unsneak() {
        // When the player un-sneaks, we return to default
        this.background.seeThrough = true
        // Not sure why 255 is required here, 128 doesn't work.
        this.foreground.textOpacity = 255.toByte()
    }

    fun update() {
        val text = this.tag.getComponent(this.player)
        this.foreground.text = text
        this.background.text = text
    }

    private fun initialiseDisplay(display: TextDisplayElement) {
        // Our nametags are going to be 'riding' the player,
        // so we ignore all position updates
        display.ignorePositionUpdates()
        // The nametag rotates with the player's camera.
        display.billboardMode = Display.BillboardConstraints.CENTER
    }

    fun tick() {
        if (this.tag.updateInterval > 0 && this.ticks++ % this.tag.updateInterval == 0) {
            this.update()
        }
    }

    private fun sendWatchPackets(
        player: ServerPlayer,
        pointers: NameTagPointers,
        consumer: Consumer<Packet<ClientGamePacketListener>>
    ) {
        this.sendInitialPacketsForTag(this.background, consumer)
        this.sendInitialPacketsForTag(this.foreground, consumer)
        val previous = pointers.previous
        if (previous != null) {
            this.shift.startWatching(this.player, consumer)
            val previousPointers = previous.watching[player.uuid] ?:
                throw IllegalStateException("What")
            previous.sendRidePacketFor(previousPointers, consumer)
        }
        this.sendRidePacket(pointers, consumer)
    }

    private fun sendRidePacket(
        pointers: NameTagPointers,
        consumer: Consumer<Packet<ClientGamePacketListener>>
    ) {
        val next = pointers.next?.shift?.entityId
        this.sendRidePacketFor(pointers, consumer, next)
    }

    private fun sendInitialPacketsForTag(
        display: TextDisplayElement,
        consumer: Consumer<Packet<ClientGamePacketListener>>
    ) {
        consumer.accept(ClientboundAddEntityPacket(
            display.entityId,
            display.uuid,
            this.player.x,
            this.player.y,
            this.player.z,
            display.pitch,
            display.yaw,
            EntityType.TEXT_DISPLAY,
            0,
            Vec3.ZERO,
            display.yaw.toDouble()
        ))

        val changed = display.dataTracker.changedEntries
        if (changed != null) {
            consumer.accept(ClientboundSetEntityDataPacket(display.entityId, changed))
        }
    }

    private fun sendRidePacketFor(
        pointers: NameTagPointers,
        consumer: Consumer<Packet<ClientGamePacketListener>>,
        next: Int? = null
    ) {
        val ids = IntArrayList()
        ids.add(this.foreground.entityId)
        ids.add(this.background.entityId)
        if (next != null) {
            ids.add(next)
        }

        val previousId = if (pointers.previous != null) this.shift.entityId else this.owner.shift.entityId
        consumer.accept(VirtualEntityUtils.createRidePacket(previousId, ids))
    }
}
