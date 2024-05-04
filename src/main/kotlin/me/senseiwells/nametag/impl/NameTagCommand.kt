package me.senseiwells.nametag.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.lucko.fabric.api.permissions.v0.Permissions
import me.senseiwells.nametag.CustomNameTags
import me.senseiwells.nametag.impl.NameTagUtils.addNameTag
import me.senseiwells.nametag.impl.NameTagUtils.removeAllNameTags
import me.senseiwells.nametag.impl.NameTagUtils.removeNameTag
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ComponentArgument
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component

object NameTagCommand {
    private val TAG_ALREADY_EXISTS = SimpleCommandExceptionType(Component.literal("A NameTag with that id already exists!"))
    private val NO_TAG_EXISTS = SimpleCommandExceptionType(Component.literal("No NameTag with that id exists!"))

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, context: CommandBuildContext) {
        dispatcher.register(
            Commands.literal("nametag").requires {
                Permissions.check(it, "customnametags.command.nametag", 2)
            }.then(
                Commands.literal("create").then(
                    Commands.argument("identifier", ResourceLocationArgument.id()).then(
                        Commands.argument("text", ComponentArgument.textComponent(context)).executes(this::createNameTag)
                    )
                )
            ).then(
                Commands.literal("delete").then(
                    Commands.argument("identifier", ResourceLocationArgument.id()).suggests { _, b ->
                        SharedSuggestionProvider.suggest(CustomNameTags.config.nametags.keys.map { it.toString() }, b)
                    }.executes(this::deleteNameTag)
                )
            ).then(
                Commands.literal("reload").executes(this::reloadNameTags)
            )
        )
    }

    private fun createNameTag(context: CommandContext<CommandSourceStack>): Int {
        val id = ResourceLocationArgument.getId(context, "identifier")
        val literal = ComponentArgument.getComponent(context, "text")

        if (CustomNameTags.config.nametags.containsKey(id)) {
            throw TAG_ALREADY_EXISTS.create()
        }

        val tag = PlaceholderNameTag(id, literal)
        CustomNameTags.config.nametags[id] = tag
        for (player in context.source.server.playerList.players) {
            player.addNameTag(tag)
        }
        NameTagConfig.write(CustomNameTags.config)
        context.source.sendSuccess(
            { Component.literal("Successfully create NameTag with id $id") },
            false
        )
        return 1
    }

    private fun deleteNameTag(context: CommandContext<CommandSourceStack>): Int {
        val id = ResourceLocationArgument.getId(context, "identifier")
        val tag = CustomNameTags.config.nametags.remove(id) ?: throw NO_TAG_EXISTS.create()
        for (player in context.source.server.playerList.players) {
            player.removeNameTag(tag)
        }
        NameTagConfig.write(CustomNameTags.config)
        context.source.sendSuccess(
            { Component.literal("Successfully delete NameTag $id") },
            false
        )
        return 1
    }

    private fun reloadNameTags(context: CommandContext<CommandSourceStack>): Int {
        val players = context.source.server.playerList.players
        for (player in players) {
            player.removeAllNameTags()
        }
        CustomNameTags.config = NameTagConfig.read()
        for (tag in CustomNameTags.config.nametags.values) {
            for (player in players) {
                player.addNameTag(tag)
            }
        }
        context.source.sendSuccess(
            { Component.literal("Successfully reloaded name tags") },
            false
        )
        return 1
    }
}