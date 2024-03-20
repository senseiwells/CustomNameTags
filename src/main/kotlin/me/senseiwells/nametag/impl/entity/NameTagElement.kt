package me.senseiwells.nametag.impl.entity

import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import me.senseiwells.nametag.api.NameTag
import me.senseiwells.nametag.impl.ShiftHeight
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.util.function.Consumer

class NameTagElement(
    private val owner: NameTagHolder,
    val tag: NameTag
) {
    internal val watching = ObjectOpenHashSet<ServerGamePacketListenerImpl>()

    private val background = TextDisplayElement()
    private val foreground = TextDisplayElement()

    private var ticks = 0

    private val player: ServerPlayer
        get() = this.owner.player

    internal val shift = NameTagShiftElement()

    init {
        this.initialiseDisplay(this.background)
        this.initialiseDisplay(this.foreground)

        this.background.seeThrough = this.tag.visibleThroughWalls
        this.background.textOpacity = 30.toByte()
        this.foreground.seeThrough = false
        this.foreground.textOpacity = 255.toByte()
        this.foreground.setBackground(0)
    }

    fun sneak() {
        // When the player sneaks, the background becomes
        // non-see-through and the foreground becomes invisible
        this.background.seeThrough = false
        this.foreground.textOpacity = -127

        this.sendDirtyPackets()
    }

    fun unsneak() {
        // When the player un-sneaks, we return to default
        this.background.seeThrough = this.tag.visibleThroughWalls
        // Not sure why 255 is required here, 128 doesn't work.
        this.foreground.textOpacity = 255.toByte()

        this.sendDirtyPackets()
    }

    fun update() {
        val text = this.tag.getComponent(this.player)
        this.foreground.text = text
        this.background.text = text

        this.sendDirtyPackets()
    }

    fun getTagEntityIds(): IntList {
        return IntList.of(this.foreground.entityId, this.background.entityId)
    }

    private fun initialiseDisplay(display: TextDisplayElement) {
        // Our nametags are going to be 'riding' the player,
        // so we ignore all position updates
        display.ignorePositionUpdates()
        // The nametag rotates with the player's camera.
        display.billboardMode = Display.BillboardConstraints.CENTER
        display.translation = Vector3f(0.0F, -0.2F, 0.0F)
    }

    fun tick() {
        if (this.tag.updateInterval > 0 && this.ticks++ % this.tag.updateInterval == 0) {
            this.update()
        }
    }

    fun sendSpawnPackets(consumer: Consumer<Packet<ClientGamePacketListener>>) {
        this.sendInitialPacketsForTag(this.background, consumer)
        this.sendInitialPacketsForTag(this.foreground, consumer)
        this.shift.sendSpawnPackets(this.player.position(), consumer)
    }

    fun sendRemovePackets(consumer: Consumer<Packet<ClientGamePacketListener>>) {
        consumer.accept(ClientboundRemoveEntitiesPacket(
            this.background.entityId,
            this.foreground.entityId,
            this.shift.id
        ))
    }

    fun updateShiftPackets(type: ShiftHeight, consumer: Consumer<Packet<ClientGamePacketListener>>) {
        consumer.accept(ClientboundRemoveEntitiesPacket(this.shift.id))
        this.shift.sendSpawnPackets(this.player.position(), consumer, type)
    }

    private fun sendDirtyPackets() {
        this.sendDirtyPacketsForTag(this.foreground)
        this.sendDirtyPacketsForTag(this.background)
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

    private fun sendDirtyPacketsForTag(display: TextDisplayElement) {
        val dirty = display.dataTracker.dirtyEntries
        if (dirty != null) {
            val packet = ClientboundSetEntityDataPacket(display.entityId, dirty)
            for (watcher in this.watching) {
                watcher.send(packet)
            }
        }
    }
}
