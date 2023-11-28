package me.senseiwells.nametag

import me.senseiwells.nametag.impl.NameTagCommand
import me.senseiwells.nametag.impl.NameTagConfig
import me.senseiwells.nametag.impl.NameTagExtension.Companion.getNameTagExtension
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object CustomNameTags: ModInitializer {
    val logger: Logger = LogManager.getLogger("CustomNameTags")

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            NameTagCommand.register(dispatcher)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            NameTagConfig.save()
        }
        ServerPlayConnectionEvents.JOIN.register { connection, _, _ ->
            for (tag in NameTagConfig.nametags.values) {
                connection.player.getNameTagExtension().addNameTag(tag)
            }
        }

        NameTagConfig.read()
    }
}