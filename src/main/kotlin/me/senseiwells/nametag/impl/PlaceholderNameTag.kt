package me.senseiwells.nametag.impl

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.StaticPreParser
import eu.pb4.placeholders.api.parsers.TextParserV1
import eu.pb4.predicate.api.MinecraftPredicate
import eu.pb4.predicate.api.PredicateContext
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.senseiwells.nametag.api.NameTag
import me.senseiwells.nametag.impl.serialization.MinecraftPredicateSerializer
import me.senseiwells.nametag.impl.serialization.ResourceLocationSerializer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

@Serializable
@OptIn(ExperimentalSerializationApi::class)
class PlaceholderNameTag(
    @Serializable(with = ResourceLocationSerializer::class)
    val id: ResourceLocation,
    val literal: String,
    @SerialName("update_interval")
    override val updateInterval: Int = 1,
    @SerialName("shift_height")
    @EncodeDefault(Mode.NEVER)
    val shiftHeight: ShiftHeight = ShiftHeight.Medium,
    @EncodeDefault(Mode.NEVER)
    @SerialName("observee_predicate")
    @Serializable(with = MinecraftPredicateSerializer::class)
    val observee: MinecraftPredicate? = null,
    @EncodeDefault(Mode.NEVER)
    @SerialName("observer_predicate")
    @Serializable(with = MinecraftPredicateSerializer::class)
    val observer: MinecraftPredicate? = null
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