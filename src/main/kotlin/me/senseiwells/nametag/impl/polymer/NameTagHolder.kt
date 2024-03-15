package me.senseiwells.nametag.impl.polymer

import eu.pb4.polymer.virtualentity.api.ElementHolder
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import me.senseiwells.nametag.impl.nametags.NameTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl

class NameTagHolder(
    private val owner: ServerGamePacketListenerImpl
): ElementHolder() {
    private val nametags = Object2ObjectLinkedOpenHashMap<NameTag, NameTagElement>()

    internal val shift = NameTagShiftElement()

    val player: ServerPlayer
        get() = this.owner.player

    init {
        this.addElement(this.shift)
    }

    fun add(tag: NameTag) {
        val element = NameTagElement(this, tag)
        this.nametags[tag] = element
        // Manually call the first update
        element.update()
    }

    fun remove(tag: NameTag) {
        this.nametags.remove(tag)
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

    override fun startWatching(connection: ServerGamePacketListenerImpl): Boolean {
        if (super.startWatching(connection)) {
            this.updateWatcher(connection.player)
            return true
        }
        return false
    }

    override fun stopWatching(connection: ServerGamePacketListenerImpl): Boolean {
        if (super.stopWatching(connection)) {
            for (nametag in this.nametags.values) {
                try {
                    nametag.unwatch(connection.player)
                } catch (e: Exception) {
                    // TODO!!!!
                }
            }
            return true
        }
        return false
    }

    override fun onTick() {
        for (connection in this.watchingPlayers) {
            this.updateWatcher(connection.player)
        }
    }

    internal fun initialise() {
        VirtualEntityUtils.addVirtualPassenger(this.player, this.shift.entityId)
    }

    private fun updateWatcher(player: ServerPlayer) {
        var previous: NameTagElement? = null
        for (element in this.nametags.values) {
            val watching = element.isWatching(player)
            val canWatch = element.tag.isObservable(this.player, player)

            if (watching) {
                if (!canWatch) {
                    element.unwatch(player)
                } else {
                    previous = element
                }
            } else if (canWatch) {
                element.watch(player, previous)
                previous = element
            }
        }
    }
}