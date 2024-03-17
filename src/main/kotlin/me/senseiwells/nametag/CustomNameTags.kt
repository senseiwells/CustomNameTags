package me.senseiwells.nametag

import me.senseiwells.nametag.impl.NameTagCommand
import me.senseiwells.nametag.impl.NameTagConfig
import me.senseiwells.nametag.impl.NameTagUtils
import me.senseiwells.nametag.impl.NameTagUtils.addNameTag
import me.senseiwells.nametag.impl.placeholder.ExtraPlayerPlaceholders
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object CustomNameTags: ModInitializer {
    val logger: Logger = LogManager.getLogger("CustomNameTags")

    var config = NameTagConfig.read()

    override fun onInitialize() {
        ExtraPlayerPlaceholders.register()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            NameTagCommand.register(dispatcher)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            NameTagConfig.write(config)
        }
        ServerPlayConnectionEvents.JOIN.register { connection, _, _ ->
            val player = connection.player
            NameTagUtils.respawnNameTags(player)
            for (tag in config.nametags.values) {
                player.addNameTag(tag)
            }
        }
    }
}