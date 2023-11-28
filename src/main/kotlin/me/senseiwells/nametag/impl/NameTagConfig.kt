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


object NameTagConfig {
    private val PATH = FabricLoader.getInstance().configDir.resolve("CustomNameTags").resolve("config.json")
    private val GSON = GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeHierarchyAdapter(MinecraftPredicate::class.java, GsonPredicateSerializer.INSTANCE)
        .create()

    var nametags: MutableMap<ResourceLocation, NameTag> = LinkedHashMap()

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
            tag.addProperty("literal", nametag.literal)
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
                property = json.get("update_interval")
                val interval = if (property != null && property is JsonPrimitive && property.isNumber) {
                    element.asInt
                } else 1
                property = tag.get("literal") ?: continue
                val literal = property.asString
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
                this.nametags[id] = NameTag(id, literal, interval, observee, observer)
            }
        }
    }
}