package me.senseiwells.nametag

import me.senseiwells.nametag.impl.NameTagCommand
import me.senseiwells.nametag.impl.NameTagConfig
import me.senseiwells.nametag.impl.NameTagUtils
import me.senseiwells.nametag.impl.NameTagUtils.addNameTag
import me.senseiwells.nametag.impl.entity.NameTagHolder
import me.senseiwells.nametag.impl.placeholder.ExtraPlayerPlaceholders
import me.senseiwells.nametag.impl.predicate.ExtraPredicates
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.world.entity.Entity
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object CustomNameTags: ModInitializer {
    private var provider: NameTagHolder.Provider? = null

    val logger: Logger = LogManager.getLogger("CustomNameTags")

    var config: NameTagConfig

    init {
        ExtraPlayerPlaceholders.register()
        ExtraPredicates.register()

        this.config = NameTagConfig.read()
    }

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, context, _ ->
            NameTagCommand.register(dispatcher, context)
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

    @JvmStatic
    @Suppress("unused")
    fun setHolderProvider(provider: NameTagHolder.Provider) {
        if (this.provider == null) {
            this.provider = provider
            return
        }
        this.logger.error(
            "CustomNameTags had conflicting custom nametag providers! ${provider}, ${this.provider}"
        )
    }

    @JvmStatic
    fun createHolder(owner: () -> Entity): NameTagHolder {
        return this.provider?.create(owner) ?: NameTagHolder(owner)
    }
}