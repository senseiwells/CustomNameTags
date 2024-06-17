package me.senseiwells.nametag.impl.placeholder

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.resources.ResourceLocation

object ExtraPlayerPlaceholders {
    fun register() {
        Placeholders.register(ResourceLocation.fromNamespaceAndPath("player", "hearts")) { ctx: PlaceholderContext, _: String? ->
            val player = ctx.player
            if (player != null) {
                PlaceholderResult.value(String.format("%.1f", player.health / 2.0F))
            } else {
                PlaceholderResult.invalid("No player!")
            }
        }
    }
}