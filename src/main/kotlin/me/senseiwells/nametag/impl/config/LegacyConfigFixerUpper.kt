package me.senseiwells.nametag.impl.config

import kotlinx.serialization.json.*
import me.senseiwells.config.JsonFixerUpper

object LegacyConfigFixerUpper: JsonFixerUpper {
    private val RENAMED_FIELDS = mapOf(
        "display" to "text",
        "literal" to "text",
        "visible_when_observee_invisible" to "visible_when_invisible",
        "observee_predicate" to "for",
        "observer_predicate" to "visible_to"
    )
    private val PREDICATE_FIELDS = setOf("for", "visible_to")

    override fun fix(element: JsonElement): JsonFixerUpper.Result {
        val root = element as? JsonObject ?: return JsonFixerUpper.Result.Pass
        val nametags = (root["nametags"] ?: root["name_tags"]) as? JsonArray ?: return JsonFixerUpper.Result.Pass
        val fixed = buildJsonObject {
            put("version", NametagConfig.VERSION)
            putJsonObject("nametags") {
                for (nametag in nametags) {
                    val obj = nametag as? JsonObject ?: continue
                    val id = obj["id"]?.let(::stringOrNull) ?: continue
                    put(upgradeId(id), upgradeNametag(obj))
                }
            }
            putJsonObject("players") { }
        }
        return JsonFixerUpper.Result.Fixed(fixed)
    }

    private fun upgradeId(id: String): String {
        return id.removePrefix("minecraft:")
    }

    private fun upgradeNametag(obj: JsonObject): JsonObject {
        return buildJsonObject {
            for ((key, value) in obj) {
                if (key == "id") {
                    continue
                }
                val renamed = RENAMED_FIELDS[key] ?: key
                put(renamed, if (renamed in PREDICATE_FIELDS) upgradePredicate(value) else value)
            }
        }
    }

    private fun upgradePredicate(predicate: JsonElement): JsonElement {
        val obj = predicate as? JsonObject ?: return wrap(predicate)
        val type = obj["type"]?.let(::stringOrNull)?.removePrefix("minecraft:") ?: return wrap(predicate)
        return when (type) {
            "always_true" -> JsonPrimitive("everyone")
            "always_false" -> JsonPrimitive("nobody")
            "operator" -> single("permission_level", obj["operator"]) ?: wrap(predicate)
            "uuid" -> list("uuids", obj["uuid"] as? JsonPrimitive) ?: wrap(predicate)
            "player_name" -> list("names", obj["name"]) ?: wrap(predicate)
            "scoreboard_tag" -> list("tags", obj["value"]) ?: wrap(predicate)
            "negate" -> obj["value"]?.let { single("not", upgradePredicate(it)) } ?: wrap(predicate)
            "any", "all" -> (obj["values"] as? JsonArray)?.let { values ->
                single(type, JsonArray(values.map(::upgradePredicate)))
            } ?: wrap(predicate)
            "permission" -> if ("operator" in obj) wrap(predicate) else single("permission", obj["permission"]) ?: wrap(predicate)
            else -> wrap(predicate)
        }
    }

    private fun wrap(predicate: JsonElement): JsonElement {
        return buildJsonObject { put("predicate", predicate) }
    }

    private fun single(key: String, value: JsonElement?): JsonElement? {
        return if (value == null) null else buildJsonObject { put(key, value) }
    }

    private fun list(key: String, value: JsonElement?): JsonElement? {
        return if (value == null) null else buildJsonObject { put(key, JsonArray(listOf(value))) }
    }

    private fun stringOrNull(element: JsonElement): String? {
        val primitive = element as? JsonPrimitive ?: return null
        return if (primitive.isString) primitive.content else null
    }
}
