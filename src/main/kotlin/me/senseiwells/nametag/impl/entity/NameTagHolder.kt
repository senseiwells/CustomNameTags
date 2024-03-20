package me.senseiwells.nametag.impl.entity

import eu.pb4.polymer.virtualentity.api.ElementHolder
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import me.senseiwells.nametag.api.NameTag
import me.senseiwells.nametag.impl.ShiftHeight
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.util.function.Consumer

class NameTagHolder(
    private val owner: ServerGamePacketListenerImpl
): ElementHolder() {
    private val nametags = Object2ObjectLinkedOpenHashMap<NameTag, NameTagElement>()
    private val watching = Object2ObjectLinkedOpenHashMap<ServerGamePacketListenerImpl, MutableSet<NameTagElement>>()

    val player: ServerPlayer
        get() = this.owner.player

    fun add(tag: NameTag) {
        val element = NameTagElement(this, tag)
        this.nametags[tag] = element
        // Manually call the first update
        element.update()
    }

    fun remove(tag: NameTag) {
        val element = this.nametags.remove(tag) ?: return

        for (connection in element.watching) {
            element.sendRemovePackets(connection::send)
            val watching = this.watching[connection]
            watching?.remove(element)
            this.resendNameTagStackFor(watching ?: listOf(), connection::send)
        }
    }

    fun removeAll() {
        for (element in this.nametags.values) {
            for (connection in element.watching) {
                element.sendRemovePackets(connection::send)
            }
        }
        this.watching.clear()
        this.nametags.clear()
    }

    fun sneak() {
        for (element in this.nametags.values) {
            element.sneak()
        }
    }

    fun unsneak() {
        for (element in this.nametags.values) {
            element.unsneak()
        }
    }

    fun isNameTagVisibleTo(tag: NameTag, player: ServerPlayer): Boolean {
        val element = this.nametags[tag] ?: return false
        return element.watching.contains(player.connection)
    }

    fun resendNamesTagTo(player: ServerPlayer, consumer: Consumer<Packet<ClientGamePacketListener>>) {
        val elements = this.watching[player.connection] ?: return
        for (element in elements) {
            element.sendSpawnPackets(consumer)
        }
    }

    override fun startWatching(connection: ServerGamePacketListenerImpl): Boolean {
        if (super.startWatching(connection)) {
            this.updateWatcher(connection)
            return true
        }
        return false
    }

    override fun stopWatching(connection: ServerGamePacketListenerImpl): Boolean {
        if (super.stopWatching(connection)) {
            val watching = this.watching.remove(connection)
            if (watching != null) {
                for (element in watching) {
                    element.watching.remove(connection)
                    element.sendRemovePackets(connection::send)
                }
            }
            return true
        }
        return false
    }

    override fun onTick() {
        for (element in this.nametags.values) {
            element.tick()
        }
        for (connection in this.watchingPlayers) {
            this.updateWatcher(connection)
        }
    }

    private fun updateWatcher(connection: ServerGamePacketListenerImpl) {
        val elements = this.watching.getOrPut(connection, ::ObjectLinkedOpenHashSet)

        var dirty = false
        for (element in this.nametags.values) {
            val watching = element.watching.contains(connection)

            // This checks if the player is visible to our watcher
            val canWatch = this.player.broadcastToPlayer(connection.player) &&
                !this.player.isInvisible &&
                element.tag.isObservable(this.player, connection.player)

            if (watching) {
                if (!canWatch) {
                    element.watching.remove(connection)
                    elements.remove(element)
                    element.sendRemovePackets(connection::send)
                    dirty = true
                }
            } else if (canWatch) {
                element.watching.add(connection)
                elements.add(element)
                element.sendSpawnPackets(connection::send)
                dirty = true
            }
        }

        if (dirty) {
            this.resendNameTagStackFor(elements, connection::send)
        }
    }

    // This function resends all the riding positions of each entity
    private fun resendNameTagStackFor(
        watching: Collection<NameTagElement>,
        consumer: Consumer<Packet<ClientGamePacketListener>>
    ) {
        if (watching.isEmpty()) {
            return
        }

        var previous = this.player.id
        var shift = ShiftHeight.MediumLarge
        val entities = IntArrayList()
        for (element in this.nametags.values.reversed()) {
            if (!watching.contains(element)) {
                continue
            }

            element.updateShiftPackets(shift, consumer)

            // We shift the nametag up by our shift
            val current = element.shift.id
            entities.add(current)
            consumer.accept(VirtualEntityUtils.createRidePacket(previous, entities))
            entities.clear()

            entities.addAll(element.getTagEntityIds())
            previous = current
            shift = element.tag.getShift()
        }

        if (entities.isNotEmpty()) {
            consumer.accept(VirtualEntityUtils.createRidePacket(previous, entities))
        }
    }
}