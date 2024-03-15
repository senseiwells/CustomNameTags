package me.senseiwells.nametag.impl

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.StaticPreParser
import eu.pb4.placeholders.api.parsers.TextParserV1
import eu.pb4.predicate.api.MinecraftPredicate
import eu.pb4.predicate.api.PredicateContext
import me.senseiwells.nametag.api.NameTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

class PlaceholderNameTag(
    val id: ResourceLocation,
    val literal: String,
    override val updateInterval: Int,
    val shiftHeight: ShiftHeight = ShiftHeight.Medium,
    val observee: MinecraftPredicate?,
    val observer: MinecraftPredicate?
): NameTag {
    private val node: TextNode by lazy { PARSER.parseNode(this.literal) }

    override fun getComponent(player: ServerPlayer): Component {
        return this.node.toText(PlaceholderContext.of(player))
    }

    override fun getShift(): ShiftHeight {
        return this.shiftHeight
    }

    override fun isObservable(observee: ServerPlayer, observer: ServerPlayer): Boolean {
        val result = this.observee?.test(PredicateContext.of(observee))?.success ?: true
        return result && (this.observer?.test(PredicateContext.of(observer))?.success ?: true)
    }

    companion object {
        private val PARSER = NodeParser.merge(
            TextParserV1.DEFAULT,
            Placeholders.DEFAULT_PLACEHOLDER_PARSER,
            StaticPreParser.INSTANCE
        )
    }
}