package me.senseiwells.nametag.impl.config

import com.mojang.util.UndashedUuid
import eu.pb4.predicate.api.MinecraftPredicate
import eu.pb4.predicate.api.PredicateContext
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import java.util.*

@Serializable(with = PlayerSelector.Serializer::class)
sealed interface PlayerSelector {
    fun test(player: ServerPlayer): Boolean

    data object Everyone: PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return true
        }
    }

    data object Nobody: PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return false
        }
    }

    data object Operators: PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return player.level().server.playerList.isOp(player.nameAndId())
        }
    }

    class Names(val names: Set<String>): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return this.names.any { it.equals(player.gameProfile.name, ignoreCase = true) }
        }
    }

    class Uuids(val uuids: Set<UUID>): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return player.uuid in this.uuids
        }
    }

    class Teams(val teams: Set<String>): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            val team = player.team ?: return false
            return team.name in this.teams
        }
    }

    class Tags(val tags: Set<String>): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return this.tags.any { it in player.entityTags() }
        }
    }

    class HasPermissionLevel(val level: PermissionLevel): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return player.permissions().hasPermission(Permission.HasCommandLevel(this.level))
        }
    }

    class HasPermission(val permission: Identifier): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return (player as PermissionContextOwner).checkPermission(this.permission, false)
        }
    }

    class Not(val selector: PlayerSelector): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return !this.selector.test(player)
        }
    }

    class AnyOf(val selectors: List<PlayerSelector>): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return this.selectors.any { it.test(player) }
        }
    }

    class AllOf(val selectors: List<PlayerSelector>): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return this.selectors.all { it.test(player) }
        }
    }

    class Predicate(val predicate: MinecraftPredicate): PlayerSelector {
        override fun test(player: ServerPlayer): Boolean {
            return this.predicate.test(PredicateContext.of(player)).success
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer: KSerializer<PlayerSelector> {
        private const val EVERYONE = "everyone"
        private const val NOBODY = "nobody"
        private const val OPERATORS = "operators"

        private const val NAMES = "names"
        private const val UUIDS = "uuids"
        private const val TEAMS = "teams"
        private const val TAGS = "tags"
        private const val PERMISSION_LEVEL = "permission_level"
        private const val PERMISSION = "permission"
        private const val NOT = "not"
        private const val ANY = "any"
        private const val ALL = "all"
        private const val PREDICATE = "predicate"

        private val PREDICATE_SERIALIZER = ContextualSerializer(MinecraftPredicate::class)

        val SHORTHANDS = mapOf(
            EVERYONE to Everyone,
            NOBODY to Nobody,
            OPERATORS to Operators,
        )

        override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

        override fun deserialize(decoder: Decoder): PlayerSelector {
            val input = decoder as? JsonDecoder
                ?: throw SerializationException("Player selectors can only be decoded from json")
            return this.decode(input.json, input.decodeJsonElement())
        }

        override fun serialize(encoder: Encoder, value: PlayerSelector) {
            val output = encoder as? JsonEncoder
                ?: throw SerializationException("Player selectors can only be encoded to json")
            output.encodeJsonElement(this.encode(output.json, value))
        }

        fun decode(json: Json, element: JsonElement): PlayerSelector {
            return when (element) {
                is JsonPrimitive -> this.decodeShorthand(element)
                is JsonObject -> this.decodeObject(json, element)
                is JsonArray -> throw SerializationException("Expected a player selector but found an array")
            }
        }

        fun encode(json: Json, selector: PlayerSelector): JsonElement {
            return when (selector) {
                Everyone -> JsonPrimitive(EVERYONE)
                Nobody -> JsonPrimitive(NOBODY)
                Operators -> JsonPrimitive(OPERATORS)
                is AllOf -> this.encodeAllOf(json, selector)
                else -> JsonObject(mapOf(this.encodeCondition(json, selector)))
            }
        }

        private fun decodeShorthand(primitive: JsonPrimitive): PlayerSelector {
            if (!primitive.isString) {
                throw SerializationException("Expected a player selector but found $primitive")
            }
            val expected = SHORTHANDS.keys.joinToString { "'${it}'" }
            return SHORTHANDS[primitive.content] ?: throw SerializationException(
                "Unknown player selector '${primitive.content}', expected one of $expected"
            )
        }

        private fun decodeObject(json: Json, obj: JsonObject): PlayerSelector {
            if (obj.isEmpty()) {
                throw SerializationException("Player selector must specify at least one condition")
            }
            val conditions = obj.map { (key, value) -> this.decodeCondition(json, key, value) }
            return conditions.singleOrNull() ?: AllOf(conditions)
        }

        private fun decodeCondition(json: Json, key: String, value: JsonElement): PlayerSelector {
            return when (key) {
                NAMES -> Names(this.strings(key, value))
                UUIDS -> Uuids(this.strings(key, value).mapTo(HashSet(), ::parseUuid))
                TEAMS -> Teams(this.strings(key, value))
                TAGS -> Tags(this.strings(key, value))
                PERMISSION_LEVEL -> HasPermissionLevel(this.permissionLevel(value))
                PERMISSION -> HasPermission(Identifier.parse(this.string(key, value)))
                NOT -> Not(this.decode(json, value))
                ANY -> AnyOf(this.selectors(json, key, value))
                ALL -> AllOf(this.selectors(json, key, value))
                PREDICATE -> Predicate(json.decodeFromJsonElement(PREDICATE_SERIALIZER, value))
                else -> throw SerializationException("Unknown player selector condition '$key'")
            }
        }

        private fun encodeCondition(json: Json, selector: PlayerSelector): Pair<String, JsonElement> {
            return when (selector) {
                is Names -> NAMES to this.strings(selector.names)
                is Uuids -> UUIDS to this.strings(selector.uuids.map(UUID::toString))
                is Teams -> TEAMS to this.strings(selector.teams)
                is Tags -> TAGS to this.strings(selector.tags)
                is HasPermissionLevel -> PERMISSION_LEVEL to JsonPrimitive(selector.level.id())
                is HasPermission -> PERMISSION to JsonPrimitive(selector.permission.toShortString())
                is Not -> NOT to this.encode(json, selector.selector)
                is AnyOf -> ANY to JsonArray(selector.selectors.map { this.encode(json, it) })
                is AllOf -> ALL to JsonArray(selector.selectors.map { this.encode(json, it) })
                is Predicate -> PREDICATE to json.encodeToJsonElement(PREDICATE_SERIALIZER, selector.predicate)
                Everyone, Nobody, Operators -> throw SerializationException("Cannot encode $selector as a condition")
            }
        }

        private fun encodeAllOf(json: Json, selector: AllOf): JsonElement {
            val conditions = LinkedHashMap<String, JsonElement>()
            for (inner in selector.selectors) {
                if (inner is Everyone || inner is Nobody || inner is Operators || inner is AllOf) {
                    return JsonObject(mapOf(this.encodeCondition(json, selector)))
                }
                val (key, value) = this.encodeCondition(json, inner)
                if (conditions.put(key, value) != null) {
                    return JsonObject(mapOf(this.encodeCondition(json, selector)))
                }
            }
            return JsonObject(conditions)
        }

        private fun string(key: String, element: JsonElement): String {
            val primitive = element as? JsonPrimitive
            if (primitive == null || !primitive.isString) {
                throw SerializationException("Expected '$key' to be a string but found $element")
            }
            return primitive.content
        }

        private fun strings(key: String, element: JsonElement): Set<String> {
            return when (element) {
                is JsonArray -> element.mapTo(LinkedHashSet()) { this.string(key, it) }
                else -> setOf(this.string(key, element))
            }
        }

        private fun strings(values: Collection<String>): JsonElement {
            return values.singleOrNull()?.let(::JsonPrimitive) ?: JsonArray(values.map(::JsonPrimitive))
        }

        private fun selectors(json: Json, key: String, element: JsonElement): List<PlayerSelector> {
            val array = element as? JsonArray
                ?: throw SerializationException("Expected '$key' to be a list of player selectors but found $element")
            return array.map { this.decode(json, it) }
        }

        private fun permissionLevel(element: JsonElement): PermissionLevel {
            val primitive = element as? JsonPrimitive
                ?: throw SerializationException("Expected '$PERMISSION_LEVEL' to be a number or a name but found $element")
            primitive.intOrNull?.let { return PermissionLevel.byId(it) }
            return PermissionLevel.entries.find { it.serializedName == primitive.content }
                ?: throw SerializationException("Unknown permission level '${primitive.content}'")
        }

        private fun parseUuid(string: String): UUID {
            return try {
                UndashedUuid.fromStringLenient(string)
            } catch (_: IllegalArgumentException) {
                throw SerializationException("Invalid uuid '$string'")
            }
        }
    }
}
