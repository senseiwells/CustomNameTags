package me.senseiwells.nametag.impl

import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.ServerPlaceholderContext
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.StaticPreParser
import eu.pb4.placeholders.api.parsers.TagParser
import me.senseiwells.nametag.impl.config.NametagDefinition
import net.casual.arcade.nametags.Nametag
import net.casual.arcade.nametags.virtual.NametagHeight
import net.casual.arcade.observer.Observer
import net.casual.arcade.observer.utils.asPlayerOrNull
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity

class ConfiguredNametag(
    val id: String,
    val definition: NametagDefinition
): Nametag {
    private val node: TextNode by lazy { PARSER.parseNode(TextNode.convert(this.definition.text)) }

    override val updateInterval: MinecraftTimeDuration = this.definition.updateInterval.coerceAtLeast(0).Ticks

    override val height: NametagHeight
        get() = this.definition.shiftHeight

    override fun getComponent(observee: Entity): Component {
        return this.node.toComponent(ServerPlaceholderContext.of(observee))
    }

    override fun isObservable(observee: Entity, observer: Observer): Boolean {
        if (observee is ServerPlayer && !this.definition.observees.test(observee)) {
            return false
        }
        val player = observer.asPlayerOrNull() ?: return true
        if (observee.isInvisibleTo(player) && !this.definition.visibleWhenInvisible) {
            return false
        }
        if (observee.passengers.isNotEmpty() && !this.definition.visibleWithPassengers) {
            return false
        }
        return this.definition.observers.test(player)
    }

    override fun isWithinRange(observee: Entity, observer: Observer): Boolean {
        val hidden = this.definition.hiddenRadius
        val visible = this.definition.visibleRadius
        val distance = observee.distanceToSqr(observer.location.position)
        if (hidden >= 0 && distance < hidden * hidden) {
            return false
        }
        if (visible >= 0 && distance > visible * visible) {
            return false
        }
        return true
    }

    override fun isVisibleThroughWalls(observee: Entity): Boolean {
        return this.definition.visibleThroughWalls
    }

    companion object {
        private val PARSER by lazy {
            NodeParser.merge(
                TagParser.DEFAULT,
                Placeholders.SERVER_PLACEHOLDER_PARSER,
                StaticPreParser.INSTANCE
            )
        }
    }
}
