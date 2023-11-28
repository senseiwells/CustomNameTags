package me.senseiwells.nametag.impl

import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.StaticPreParser
import eu.pb4.placeholders.api.parsers.TextParserV1
import eu.pb4.predicate.api.MinecraftPredicate
import net.minecraft.resources.ResourceLocation

class NameTag(
    val id: ResourceLocation,
    val literal: String,
    val updateInterval: Int,
    val observee: MinecraftPredicate?,
    val observer: MinecraftPredicate?
) {
    val node: TextNode by lazy { PARSER.parseNode(this.literal) }

    companion object {
        private val PARSER = NodeParser.merge(
            TextParserV1.DEFAULT,
            Placeholders.DEFAULT_PLACEHOLDER_PARSER,
            StaticPreParser.INSTANCE
        )
    }
}