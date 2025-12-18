package me.senseiwells.nametag.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.StaticPreParser
import eu.pb4.placeholders.api.parsers.TagParser
import eu.pb4.predicate.api.MinecraftPredicate
import eu.pb4.predicate.api.PredicateContext
import eu.pb4.predicate.api.PredicateRegistry
import net.casual.arcade.nametags.Nametag
import net.casual.arcade.nametags.virtual.NametagHeight
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.encodedOptionalFieldOf
import net.casual.arcade.utils.fieldOfAny
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import java.util.*

class PlaceholderNametag(
    val id: Identifier,
    val display: Component = Component.empty(),
    override val updateInterval: MinecraftTimeDuration = 1.Ticks,
    val visibleRadius: Double = -1.0,
    val hiddenRadius: Double = -1.0,
    val visibleThroughWalls: Boolean = true,
    val visibleWhenObserveeInvisible: Boolean = false,
    val visibleWithPassengers: Boolean = false,
    override val height: NametagHeight = NametagHeight.DEFAULT,
    val observee: Optional<MinecraftPredicate> = Optional.empty(),
    val observer: Optional<MinecraftPredicate> = Optional.empty()
): Nametag {
    private val node: TextNode by lazy { PARSER.parseNode(TextNode.convert(this.display)) }

    override fun getComponent(observee: Entity): Component {
        return this.node.toText(PlaceholderContext.of(observee))
    }

    override fun isObservable(observee: Entity, observer: ServerPlayer): Boolean {
        if (observee.isInvisibleTo(observer) && !this.visibleWhenObserveeInvisible) {
            return false
        }
        if (observee.passengers.isNotEmpty() && !this.visibleWithPassengers) {
            return false
        }

        val result = this.observee.map { it.test(PredicateContext.of(observee)).success }.orElse(true)
        return result && (this.observer.map { it.test(PredicateContext.of(observer)).success }.orElse(true))
    }

    override fun isWithinRange(observee: Entity, observer: ServerPlayer): Boolean {
        val distance = observee.distanceToSqr(observer)
        if (this.hiddenRadius >= 0 && distance < this.hiddenRadius * this.hiddenRadius) {
            return false
        }
        if (this.visibleRadius >= 0 && distance > this.visibleRadius * this.visibleRadius) {
            return false
        }
        return true
    }

    override fun isVisibleThroughWalls(observee: Entity): Boolean {
        return this.visibleThroughWalls
    }

    companion object {
        private val PARSER by lazy {
            NodeParser.merge(
                TagParser.DEFAULT,
                Placeholders.DEFAULT_PLACEHOLDER_PARSER,
                StaticPreParser.INSTANCE
            )
        }

        val CODEC: Codec<PlaceholderNametag> = RecordCodecBuilder.create { instance ->
            instance.group(
                Identifier.CODEC.fieldOf("id").forGetter(PlaceholderNametag::id),
                ComponentSerialization.CODEC.fieldOfAny("display", "literal").orElse(Component.empty()).forGetter(PlaceholderNametag::display),
                MinecraftTimeDuration.CODEC.encodedOptionalFieldOf("update_interval", 1.Ticks).forGetter(PlaceholderNametag::updateInterval),
                Codec.DOUBLE.encodedOptionalFieldOf("visible_radius", -1.0).forGetter(PlaceholderNametag::visibleRadius),
                Codec.DOUBLE.encodedOptionalFieldOf("hidden_radius", -1.0).forGetter(PlaceholderNametag::hiddenRadius),
                Codec.BOOL.optionalFieldOf("visible_through_walls", true).forGetter(PlaceholderNametag::visibleThroughWalls),
                Codec.BOOL.optionalFieldOf("visible_when_observee_invisible", false).forGetter(PlaceholderNametag::visibleWhenObserveeInvisible),
                Codec.BOOL.optionalFieldOf("visible_with_passengers", false).forGetter(PlaceholderNametag::visibleWithPassengers),
                NametagHeight.CODEC.optionalFieldOf("shift_height", NametagHeight.DEFAULT).forGetter(PlaceholderNametag::height),
                PredicateRegistry.CODEC.optionalFieldOf("observee_predicate").forGetter(PlaceholderNametag::observee),
                PredicateRegistry.CODEC.optionalFieldOf("observer_predicate").forGetter(PlaceholderNametag::observer)
            ).apply(instance, ::PlaceholderNametag)
        }
    }
}