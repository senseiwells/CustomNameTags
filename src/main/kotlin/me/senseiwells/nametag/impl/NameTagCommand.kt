package me.senseiwells.nametag.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.lucko.fabric.api.permissions.v0.Permissions
import me.senseiwells.nametag.impl.NameTagExtension.Companion.getNameTagExtension
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component

object NameTagCommand {
    private val TAG_ALREADY_EXISTS = SimpleCommandExceptionType(Component.literal("A NameTag with that id already exists!"))
    private val NO_TAG_EXISTS = SimpleCommandExceptionType(Component.literal("No NameTag with that id exists!"))

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("nametag").requires {
                Permissions.check(it, "customnametags.command.nametag", 2)
            }.then(
                Commands.literal("create").then(
                    Commands.argument("identifier", ResourceLocationArgument.id()).then(
                        Commands.argument("text", StringArgumentType.greedyString()).executes(this::createNameTag)
                    )
                )
            ).then(
                Commands.literal("delete").then(
                    Commands.argument("identifier", ResourceLocationArgument.id()).suggests { _, b ->
                        SharedSuggestionProvider.suggest(NameTagConfig.nametags.keys.map { it.toString() }, b)
                    }.executes(this::deleteNameTag)
                )
            ).then(
                Commands.literal("reload").executes(this::reloadNameTags)
            )
        )
    }

    private fun createNameTag(context: CommandContext<CommandSourceStack>): Int {
        val id = ResourceLocationArgument.getId(context, "identifier")
        val literal = StringArgumentType.getString(context, "text")

        if (NameTagConfig.nametags.containsKey(id)) {
            throw TAG_ALREADY_EXISTS.create()
        }

        val tag = PlaceholderNameTag(id, literal, 1, ShiftHeight.Medium, null, null)
        NameTagConfig.nametags[id] = tag
        for (player in context.source.server.playerList.players) {
            player.getNameTagExtension().addNameTag(tag)
        }
        NameTagConfig.save()
        context.source.sendSuccess(
            { Component.literal("Successfully create NameTag with id $id") },
            false
        )
        return 1
    }

    private fun deleteNameTag(context: CommandContext<CommandSourceStack>): Int {
        val id = ResourceLocationArgument.getId(context, "identifier")
        val tag = NameTagConfig.nametags.remove(id) ?: throw NO_TAG_EXISTS.create()
        for (player in context.source.server.playerList.players) {
            player.getNameTagExtension().removeNameTag(tag)
        }
        NameTagConfig.save()
        context.source.sendSuccess(
            { Component.literal("Successfully delete NameTag $id") },
            false
        )
        return 1
    }

    private fun reloadNameTags(context: CommandContext<CommandSourceStack>): Int {
        val players = context.source.server.playerList.players
        for (player in players) {
            player.getNameTagExtension().removeAllNameTags()
        }
        NameTagConfig.nametags.clear()
        NameTagConfig.read()
        for (tag in NameTagConfig.nametags.values) {
            for (player in players) {
                player.getNameTagExtension().addNameTag(tag)
            }
        }
        context.source.sendSuccess(
            { Component.literal("Successfully reloaded name tags") },
            false
        )
        return 1
    }
}