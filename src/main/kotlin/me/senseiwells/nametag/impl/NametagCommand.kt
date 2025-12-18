package me.senseiwells.nametag.impl

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.lucko.fabric.api.permissions.v0.Permissions
import me.senseiwells.nametag.CustomNameTags
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.literal
import net.casual.arcade.commands.success
import net.casual.arcade.nametags.extensions.EntityNametagExtension.Companion.addNametag
import net.casual.arcade.nametags.extensions.EntityNametagExtension.Companion.removeNametag
import net.casual.arcade.nametags.extensions.EntityNametagExtension.Companion.removeNametags
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ComponentArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.PermissionLevel

object NametagCommand: CommandTree {
    private val TAG_ALREADY_EXISTS = SimpleCommandExceptionType(Component.literal("A NameTag with that id already exists!"))
    private val NO_TAG_EXISTS = SimpleCommandExceptionType(Component.literal("No NameTag with that id exists!"))

    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral("nametag") {
            requires { Permissions.check(it, "custom-nametags.command.nametag", PermissionLevel.GAMEMASTERS) }
            literal("create") {
                argument("identifier", IdentifierArgument.id()) {
                    argument("text", ComponentArgument.textComponent(buildContext)) {
                        executes(::createNameTag)
                    }
                }
            }
            literal("delete") {
                argument("identifier", IdentifierArgument.id()) {
                    suggests { _, b -> SharedSuggestionProvider.suggestResource(CustomNameTags.getNametagIds(), b) }
                    executes(::deleteNameTag)
                }
            }
            literal("reload") {
                executes(::reloadNameTags)
            }
        }
    }

    private fun createNameTag(context: CommandContext<CommandSourceStack>): Int {
        val id = IdentifierArgument.getId(context, "identifier")
        val literal = ComponentArgument.getRawComponent(context, "text")

        if (CustomNameTags.getNametagIds().contains(id)) {
            throw TAG_ALREADY_EXISTS.create()
        }

        val tag = PlaceholderNametag(id, literal)
        CustomNameTags.addNametag(id, tag)
        for (player in context.source.server.playerList.players) {
            player.addNametag(tag)
        }
        CustomNameTags.writeConfig(context.source.server)
        return context.source.success(Component.literal("Successfully create NameTag with id $id"))
    }

    private fun deleteNameTag(context: CommandContext<CommandSourceStack>): Int {
        val id = IdentifierArgument.getId(context, "identifier")
        val tag = CustomNameTags.removeNametag(id) ?: throw NO_TAG_EXISTS.create()
        for (player in context.source.server.playerList.players) {
            player.removeNametag(tag)
        }
        CustomNameTags.writeConfig(context.source.server)
        return context.source.success(Component.literal("Successfully delete NameTag $id"))
    }

    private fun reloadNameTags(context: CommandContext<CommandSourceStack>): Int {
        val players = context.source.server.playerList.players
        for (player in players) {
            player.removeNametags()
        }
        CustomNameTags.readConfig(context.source.server)
        for (tag in CustomNameTags.getNametags()) {
            for (player in players) {
                player.addNametag(tag)
            }
        }
        return context.source.success(Component.literal("Successfully reloaded name tags"))
    }
}