package me.senseiwells.nametag.impl

import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment
import me.senseiwells.nametag.NameTagHolderExtension
import me.senseiwells.nametag.api.NameTag
import me.senseiwells.nametag.impl.entity.NameTagHolder
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerPlayer
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Consumer

object NameTagUtils {
    private val ServerPlayer.nameTagHolder: NameTagHolder
        get() = (this.connection as NameTagHolderExtension).`nametag$getHolder`()

    fun ServerPlayer.addNameTag(tag: NameTag) {
        this.nameTagHolder.add(tag)
    }

    fun ServerPlayer.removeNameTag(tag: NameTag) {
        this.nameTagHolder.remove(tag)
    }

    fun ServerPlayer.removeAllNameTags() {
        this.nameTagHolder.removeAll()
    }

    @Suppress("unused")
    fun ServerPlayer.isNameTagVisibleTo(nametag: NameTag, other: ServerPlayer): Boolean {
        return this.nameTagHolder.isNameTagVisibleTo(nametag, other)
    }

    fun ServerPlayer.resendNameTagsTo(player: ServerPlayer, consumer: Consumer<Packet<ClientGamePacketListener>>) {
        this.nameTagHolder.resendNamesTagTo(player, consumer)
    }

    @Internal
    @JvmStatic
    fun ServerPlayer.sneakNameTags() {
        this.nameTagHolder.sneak()
    }

    @Internal
    @JvmStatic
    fun ServerPlayer.unsneakNameTags() {
        this.nameTagHolder.unsneak()
    }

    @Internal
    @JvmStatic
    fun respawnNameTags(player: ServerPlayer) {
        EntityAttachment.ofTicking(player.nameTagHolder, player)
    }
}