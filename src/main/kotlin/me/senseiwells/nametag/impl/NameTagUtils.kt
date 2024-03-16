package me.senseiwells.nametag.impl

import me.senseiwells.nametag.ExtensionHolder
import me.senseiwells.nametag.api.NameTag
import net.minecraft.server.level.ServerPlayer
import org.jetbrains.annotations.ApiStatus.Internal

object NameTagUtils {
    private val ServerPlayer.nameTagExtension: NameTagExtension
        get() = (this.connection as ExtensionHolder).`nametag$getExtension`()

    fun ServerPlayer.addNameTag(tag: NameTag) {
        this.nameTagExtension.addNameTag(tag)
    }

    fun ServerPlayer.removeNameTag(tag: NameTag) {
        this.nameTagExtension.removeNameTag(tag)
    }

    fun ServerPlayer.removeAllNameTags() {
        this.nameTagExtension.removeAllNameTags()
    }

    @Internal
    @JvmStatic
    fun sneakNameTags(player: ServerPlayer) {
        player.nameTagExtension.sneak()
    }

    @Internal
    @JvmStatic
    fun unsneakNameTags(player: ServerPlayer) {
        player.nameTagExtension.unsneak()
    }

    @Internal
    @JvmStatic
    fun respawnNameTags(player: ServerPlayer) {
        player.nameTagExtension.respawn(player)
    }
}