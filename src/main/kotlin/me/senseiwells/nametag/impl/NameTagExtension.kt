package me.senseiwells.nametag.impl

import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment
import me.senseiwells.nametag.ExtensionHolder
import me.senseiwells.nametag.api.NameTag
import me.senseiwells.nametag.impl.entity.NameTagHolder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl

class NameTagExtension(
    private val owner: ServerGamePacketListenerImpl
) {
    private val holder = NameTagHolder(this.owner)

    internal fun respawn(player: ServerPlayer) {
        EntityAttachment.ofTicking(this.holder, player)
    }

    internal fun addNameTag(tag: NameTag) {
        this.holder.add(tag)
    }

    internal fun removeNameTag(tag: NameTag) {
        this.holder.remove(tag)
    }

    internal fun removeAllNameTags() {
        this.holder.removeAll()
    }

    fun sneak() {
        this.holder.sneak()
    }

    fun unsneak() {
        this.holder.unsneak()
    }

    companion object {
        @JvmStatic
        fun ServerPlayer.getNameTagExtension(): NameTagExtension {
            return (this.connection as ExtensionHolder).`nametag$getExtension`()
        }
    }
}