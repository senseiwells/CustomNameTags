package me.senseiwells.nametag.impl.placeholder

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.Placeholders
import net.casual.arcade.utils.Identifier
import net.minecraft.network.chat.Component

object ExtraPlayerPlaceholders {
    internal fun register() {
        Placeholders.register(Identifier("player", "hearts")) { ctx: PlaceholderContext, _: String? ->
            val player = ctx.player
            if (player != null) {
                PlaceholderResult.value(String.format("%.1f", player.health / 2.0F))
            } else {
                PlaceholderResult.invalid("No player!")
            }
        }
        Placeholders.register(Identifier("player", "floodgate_name")) { ctx: PlaceholderContext, _: String? ->
            val profile = ctx.player?.gameProfile ?: ctx.gameProfile
            if (profile != null) {
                PlaceholderResult.value(Component.literal(profile.name.substringAfter('.')))
            } else {
                PlaceholderResult.invalid("No player!")
            }
        }
    }
}