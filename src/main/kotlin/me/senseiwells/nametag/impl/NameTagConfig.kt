package me.senseiwells.nametag.impl

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import me.senseiwells.nametag.CustomNameTags
import me.senseiwells.nametag.impl.serialization.NameTagsSerializer
import me.senseiwells.nametag.impl.serialization.SerializableResourceLocation
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import org.apache.commons.lang3.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class NameTagConfig(
    @SerialName("name_tags")
    @Serializable(with = NameTagsSerializer::class)
    val nametags: Object2ObjectLinkedOpenHashMap<SerializableResourceLocation, PlaceholderNameTag> = Object2ObjectLinkedOpenHashMap()
) {
    companion object {
        private val config: Path = FabricLoader.getInstance().configDir.resolve("CustomNameTags").resolve("config.json")

        private val json = Json {
            encodeDefaults = true
            prettyPrint = true
            prettyPrintIndent = "  "
        }

        fun read(): NameTagConfig {
            if (!this.config.exists()) {
                CustomNameTags.logger.info("Generating default config")
                return NameTagConfig().also { this.write(it) }
            }
            return try {
                this.config.inputStream().use {
                    json.decodeFromStream(it)
                }
            } catch (e: Exception) {
                CustomNameTags.logger.error("Failed to read CustomNameTag config, generating default", e)
                NameTagConfig().also { this.write(it) }
            }
        }

        @JvmStatic
        fun write(config: NameTagConfig) {
            try {
                this.config.parent.createDirectories()
                this.config.outputStream().use {
                    json.encodeToStream(config, it)
                }
            } catch (e: IOException) {
                CustomNameTags.logger.error("Failed to write CustomNameTag config", e)
            } catch (e: SerializationException) {
                CustomNameTags.logger.error("Failed to serialize CustomNameTag config", e)
            }
        }
    }
}