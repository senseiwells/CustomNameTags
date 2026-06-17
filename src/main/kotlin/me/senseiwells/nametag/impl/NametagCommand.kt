package me.senseiwells.nametag.impl

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.senseiwells.nametag.CustomNameTags
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.literal
import net.casual.arcade.commands.requiresPermission
import net.casual.arcade.commands.success
import net.casual.arcade.nametags.extensions.EntityNametagExtension.Companion.nametagExtension
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ComponentArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.PermissionLevel

object NametagCommand: CommandTree<CommandSourceStack> {
    private val TAG_ALREADY_EXISTS = SimpleCommandExceptionType(Component.literal("A nametag with that id already exists!"))
    private val NO_TAG_EXISTS = SimpleCommandExceptionType(Component.literal("No nametag with that id exists!"))

    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral("nametag") {
            requiresPermission(CustomNameTags.id("commands.nametag"), PermissionLevel.GAMEMASTERS)
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
            player.nametagExtension.add(tag)
        }
        CustomNameTags.writeConfig(context.source.server)
        return context.source.success(Component.literal("Successfully created nametag with id $id"))
    }

    private fun deleteNameTag(context: CommandContext<CommandSourceStack>): Int {
        val id = IdentifierArgument.getId(context, "identifier")
        val tag = CustomNameTags.removeNametag(id) ?: throw NO_TAG_EXISTS.create()
        for (player in context.source.server.playerList.players) {
            player.nametagExtension.add(tag)
        }
        CustomNameTags.writeConfig(context.source.server)
        return context.source.success(Component.literal("Successfully deleted nametag $id"))
    }

    private fun reloadNameTags(context: CommandContext<CommandSourceStack>): Int {
        val players = context.source.server.playerList.players
        for (player in players) {
            player.nametagExtension.removeAll()
        }
        CustomNameTags.readConfig(context.source.server)
        for (tag in CustomNameTags.getNametags()) {
            for (player in players) {
                player.nametagExtension.add(tag)
            }
        }
        return context.source.success(Component.literal("Successfully reloaded nametags"))
    }
}