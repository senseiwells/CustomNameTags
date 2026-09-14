package me.senseiwells.nametag

import me.senseiwells.config.JsonConfigFile
import me.senseiwells.nametag.impl.NametagCommand
import me.senseiwells.nametag.impl.NametagManager
import me.senseiwells.nametag.impl.placeholder.ExtraPlayerPlaceholders
import me.senseiwells.nametag.impl.predicate.ExtraPredicates
import net.casual.arcade.commands.register
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerRegisterCommandEvent
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.player.PlayerJoinEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.utils.Identifier
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

object CustomNameTags: ModInitializer {
    const val MOD_ID = "custom-nametags"

    val logger: Logger = LoggerFactory.getLogger(MOD_ID)
    val configDir: Path = FabricLoader.getInstance().configDir.resolve(MOD_ID)

    fun id(path: String): Identifier {
        return Identifier(MOD_ID, path)
    }

    override fun onInitialize() {
        ExtraPlayerPlaceholders.register()
        ExtraPredicates.register()

        this.migrateOldConfigs()

        GlobalEventHandler.Server.register<ServerStartEvent> { (server) ->
            NametagManager.load(server)
        }
        GlobalEventHandler.Server.register<ServerRegisterCommandEvent> {
            it.register(NametagCommand)
        }
        GlobalEventHandler.Server.register<PlayerJoinEvent> { (player) ->
            NametagManager.refresh(player)
        }
    }

    private fun migrateOldConfigs() {
        JsonConfigFile.moveDirectory(this.configDir.resolveSibling("CustomNameTags"), this.configDir, this.logger)
    }
}