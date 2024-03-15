package me.senseiwells.nametag.impl

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import eu.pb4.predicate.api.GsonPredicateSerializer
import eu.pb4.predicate.api.MinecraftPredicate
import me.senseiwells.nametag.CustomNameTags
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import java.io.IOException
import kotlin.io.path.*

// TODO: Improve this...
object NameTagConfig {
    private val PATH = FabricLoader.getInstance().configDir.resolve("CustomNameTags").resolve("config.json")
    private val GSON = GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeHierarchyAdapter(MinecraftPredicate::class.java, GsonPredicateSerializer.INSTANCE)
        .create()
    private const val DEFAULT_UPDATE_INTERVAL = 1

    var nametags: MutableMap<ResourceLocation, PlaceholderNameTag> = LinkedHashMap()

    fun read() {
        if (PATH.exists()) {
            try {
                val json = PATH.bufferedReader().use {
                    GSON.fromJson(it, JsonObject::class.java)
                }
                this.deserialize(json)
            } catch (e: IOException) {
                CustomNameTags.logger.error("Failed to read config", e)
            } catch (e: JsonParseException) {
                CustomNameTags.logger.error("Failed to read config", e)
            }
        }
    }

    fun save() {
        try {
            PATH.parent.createDirectories()
            PATH.bufferedWriter().use {
                GSON.toJson(this.serialize(), it)
            }
        } catch (e: IOException) {
            CustomNameTags.logger.error("Failed to write config", e)
        }
    }

    private fun serialize(): JsonObject {
        val json = JsonObject()
        val tags = JsonArray()
        for (nametag in this.nametags.values) {
            val tag = JsonObject()
            tag.addProperty("id", nametag.id.toString())
            tag.addProperty("update_interval", nametag.updateInterval)
            tag.addProperty("literal", nametag.literal)
            tag.addProperty("shift_height", nametag.shiftHeight.name)
            if (nametag.observee != null) {
                tag.add("observee_predicate", GSON.toJsonTree(nametag.observee))
            }
            if (nametag.observer != null) {
                tag.add("observer_predicate", GSON.toJsonTree(nametag.observer))
            }
            tags.add(tag)
        }
        json.add("name_tags", tags)
        return json
    }

    private fun deserialize(json: JsonObject) {
        val element = json.get("name_tags")
        if (element != null && element is JsonArray) {
            for (tag in element) {
                if (tag !is JsonObject) {
                    continue
                }
                var property = tag.get("id") ?: continue
                val id = ResourceLocation.tryParse(property.asString) ?: continue
                val interval = if (tag.has("update_interval")) {
                    property = tag.get("update_interval")
                    if (property is JsonPrimitive && property.isNumber) {
                        property.asInt
                    } else DEFAULT_UPDATE_INTERVAL
                } else DEFAULT_UPDATE_INTERVAL
                property = tag.get("literal") ?: continue
                val literal = property.asString

                val shift = try {
                    if (tag.has("shift_height")) {
                        ShiftHeight.valueOf(tag.get("shift_height").asString)
                    } else ShiftHeight.Medium
                } catch (e: IllegalArgumentException) {
                    CustomNameTags.logger.error("Failed to read 'shift_height' of $id, using default")
                    ShiftHeight.Medium
                }

                val observee = if (tag.has("observee_predicate")) {
                    this.runCatching {
                        GSON.fromJson(tag.get("observee_predicate"), MinecraftPredicate::class.java)
                    }.getOrNull()
                } else null
                val observer = if (tag.has("observer_predicate")) {
                    this.runCatching {
                        GSON.fromJson(tag.get("observer_predicate"), MinecraftPredicate::class.java)
                    }.getOrNull()
                } else null
                this.nametags[id] = PlaceholderNameTag(id, literal, interval, shift, observee, observer)
            }
        }
    }
}