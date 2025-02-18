package me.senseiwells.nametag.impl.entity

import eu.pb4.polymer.virtualentity.api.ElementHolder
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import me.senseiwells.nametag.api.NameTag
import me.senseiwells.nametag.impl.ShiftHeight
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Entity
import org.jetbrains.annotations.ApiStatus.OverrideOnly
import java.util.function.Consumer

open class NameTagHolder(
    private val owner: () -> Entity
): ElementHolder() {
    protected val nametags = Object2ObjectLinkedOpenHashMap<NameTag, NameTagElement>()
    protected val watching = Object2ObjectLinkedOpenHashMap<ServerGamePacketListenerImpl, MutableSet<NameTagElement>>()
    protected val cached = Int2ObjectOpenHashMap<IntArray>()

    val entity: Entity
        get() = this.owner()

    fun add(tag: NameTag) {
        val element = NameTagElement(this, tag)
        this.nametags[tag] = element
        // Manually call the first update
        element.update()
        this.onAdd(element)
    }

    fun remove(tag: NameTag) {
        val element = this.nametags.remove(tag) ?: return

        for (connection in element.watching) {
            element.sendRemovePackets(connection::send)
            val watching = this.watching[connection]
            watching?.remove(element)
            this.resendNameTagStackFor(watching ?: listOf(), connection.player, connection::send)
        }
        this.onRemove(element)
    }

    fun removeAll() {
        for (element in this.nametags.values) {
            for (connection in element.watching) {
                element.sendRemovePackets(connection::send)
            }
        }
        this.watching.clear()
        this.nametags.clear()
        this.onRemoveAll()
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

    @Suppress("unused")
    fun firstNametag(): NameTagElement? {
        return this.nametags.values.firstOrNull()
    }

    open fun isNameTagVisibleTo(tag: NameTag, player: ServerPlayer): Boolean {
        val element = this.nametags[tag] ?: return false
        return element.watching.contains(player.connection)
    }

    open fun resendNamesTagTo(player: ServerPlayer, consumer: Consumer<Packet<ClientGamePacketListener>>) {
        val elements = this.watching[player.connection] ?: return
        for (element in elements) {
            element.sendSpawnPackets(consumer)
        }
        this.resendNameTagStackFor(elements, player, consumer)
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
            this.cached.remove(connection.player.id)
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

    @OverrideOnly
    open fun onSendDirtyPacket(element: NameTagElement, packet: Packet<*>) {

    }

    protected open fun onAdd(element: NameTagElement) {

    }

    protected open fun onRemove(element: NameTagElement) {

    }

    protected open fun onRemoveAll() {

    }

    protected fun updateWatcher(connection: ServerGamePacketListenerImpl) {
        val elements = this.watching.getOrPut(connection, ::ObjectLinkedOpenHashSet)

        var dirty = false
        for (element in this.nametags.values) {
            val watching = element.watching.contains(connection)

            // This checks if the player is visible to our watcher
            val canWatch = this.entity.broadcastToPlayer(connection.player) &&
                element.tag.isObservable(this.entity, connection.player) &&
                element.tag.isWithinRange(this.entity, connection.player)

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
            this.resendNameTagStackFor(elements, connection.player, connection::send)
        }
    }

    // This function resends all the riding positions of each entity
    protected fun resendNameTagStackFor(
        watching: Collection<NameTagElement>,
        observee: ServerPlayer,
        consumer: Consumer<Packet<ClientGamePacketListener>>
    ) {
        if (watching.isEmpty()) {
            return
        }

        val ridden = Int2ObjectOpenHashMap<IntArray>()
        var previous = this.entity.id
        var shift = ShiftHeight.DEFAULT
        val entities = IntArrayList()
        for (element in this.nametags.values.reversed()) {
            if (!watching.contains(element)) {
                continue
            }

            element.updateShiftPackets(shift, consumer)

            // We shift the nametag up by our shift
            val current = element.shift.id
            entities.add(current)
            ridden.put(previous, entities.toIntArray())
            entities.clear()

            entities.addAll(element.getTagEntityIds())
            previous = current
            shift = element.tag.getShift()
        }

        val own = ridden.remove(this.entity.id)
            ?: throw IllegalStateException("Name tag owner expected to have visible nametags")

        this.cached.put(observee.id, own)

        consumer.accept(ClientboundSetPassengersPacket(this.entity))

        for (entry in ridden.int2ObjectEntrySet()) {
            consumer.accept(VirtualEntityUtils.createRidePacket(entry.intKey, entities.elements()))
        }
        if (entities.isNotEmpty()) {
            consumer.accept(VirtualEntityUtils.createRidePacket(previous, entities))
        }
    }

    internal fun getCachedIdsFor(observee: ServerPlayer): IntArray? {
        return this.cached.get(observee.id)
    }

    fun interface Provider {
        fun create(owner: () -> Entity): NameTagHolder
    }
}