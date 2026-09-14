package me.senseiwells.nametag.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.senseiwells.nametag.CustomNameTags
import me.senseiwells.nametag.impl.config.NametagDefinition
import me.senseiwells.nametag.impl.config.PlayerSelector
import net.casual.arcade.commands.CommandTree
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.arguments.MappedArgument
import net.casual.arcade.commands.literal
import net.casual.arcade.commands.requiresPermission
import net.casual.arcade.commands.success
import net.casual.arcade.commands.suggests
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.server.players.NameAndId

object NametagCommand: CommandTree<CommandSourceStack> {
    private val ALREADY_EXISTS = DynamicCommandExceptionType { Component.literal("A nametag with id '$it' already exists!") }
    private val NO_SUCH_NAMETAG = DynamicCommandExceptionType { Component.literal("No nametag with id '$it' exists!") }
    private val NOTHING_TO_RESET = SimpleCommandExceptionType(Component.literal("Those players have no nametag overrides!"))

    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral("nametag") {
            requiresPermission(CustomNameTags.id("commands.nametag"), PermissionLevel.GAMEMASTERS)
            literal("create") {
                argument("id", StringArgumentType.string()) {
                    argument("text", StringArgumentType.greedyString()) {
                        executes(::create)
                    }
                }
            }
            literal("edit") {
                argument("id", StringArgumentType.string()) {
                    suggests { _ -> idSuggestions(NametagManager.config.globalNametagIds()) }
                    literal("text") {
                        argument("text", StringArgumentType.greedyString()) {
                            executes(::editText)
                        }
                    }
                    literal("for") {
                        argument("who", MappedArgument.mapped(PlayerSelector.Serializer.SHORTHANDS)) {
                            executes(::editFor)
                        }
                    }
                    literal("priority") {
                        argument("priority", IntegerArgumentType.integer()) {
                            executes(::editPriority)
                        }
                    }
                }
            }
            literal("delete") {
                argument("id", StringArgumentType.string()) {
                    suggests { _ -> idSuggestions(NametagManager.config.globalNametagIds()) }
                    executes(::delete)
                }
            }
            literal("list") {
                executes(::list)
            }
            literal("player") {
                argument("players", GameProfileArgument.gameProfile()) {
                    literal("give") {
                        argument("id", StringArgumentType.string()) {
                            suggests { _ -> idSuggestions(NametagManager.config.allNametagIds()) }
                            executes(::give)
                        }
                    }
                    literal("revoke") {
                        argument("id", StringArgumentType.string()) {
                            suggests { _ -> idSuggestions(NametagManager.config.allNametagIds()) }
                            executes(::revoke)
                        }
                    }
                    literal("set") {
                        argument("id", StringArgumentType.string()) {
                            suggests { _ -> idSuggestions(NametagManager.config.allNametagIds()) }
                            argument("text", StringArgumentType.greedyString()) {
                                executes(::setText)
                            }
                        }
                    }
                    literal("reset") {
                        executes(::resetAll)
                        argument("id", StringArgumentType.string()) {
                            suggests { _ -> idSuggestions(NametagManager.config.allNametagIds()) }
                            executes(::reset)
                        }
                    }
                }
            }
            literal("reload") {
                executes(::reload)
            }
        }
    }

    private fun create(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val text = StringArgumentType.getString(context, "text")
        if (NametagManager.hasDefinition(id)) {
            throw ALREADY_EXISTS.create(id)
        }
        this.setDefinitionAndSave(context, id, NametagDefinition(text = Component.literal(text)))
        return context.source.success(Component.literal("Successfully created nametag '$id'"))
    }

    private fun editText(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val text = StringArgumentType.getString(context, "text")
        val definition = NametagManager.getDefinition(id) ?: throw NO_SUCH_NAMETAG.create(id)
        this.setDefinitionAndSave(context, id, definition.copy(text = Component.literal(text)))
        return context.source.success(Component.literal("Successfully updated the text of nametag '$id'"))
    }

    private fun editFor(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val selector = MappedArgument.getMapped<PlayerSelector>(context, "who")
        val definition = NametagManager.getDefinition(id) ?: throw NO_SUCH_NAMETAG.create(id)
        this.setDefinitionAndSave(context, id, definition.copy(observees = selector))
        return context.source.success(Component.literal("Successfully updated the for selector of nametag '$id'"))
    }

    private fun editPriority(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val priority = IntegerArgumentType.getInteger(context, "priority")
        val definition = NametagManager.getDefinition(id) ?: throw NO_SUCH_NAMETAG.create(id)
        this.setDefinitionAndSave(context, id, definition.copy(priority = priority))
        return context.source.success(Component.literal("Successfully updated the priority of nametag '$id'"))
    }

    private fun delete(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        NametagManager.removeDefinition(id) ?: throw NO_SUCH_NAMETAG.create(id)
        NametagManager.save()
        NametagManager.refreshAll(context.source.server)
        return context.source.success(Component.literal("Successfully deleted nametag '$id'"))
    }

    private fun list(context: CommandContext<CommandSourceStack>): Int {
        val nametags = NametagManager.config.nametags
        if (nametags.isEmpty()) {
            return context.source.success(Component.literal("There are no nametags defined"))
        }
        val component = Component.literal("Nametags:")
        for ((id, definition) in nametags) {
            component.append(Component.literal("\n - $id: ")).append(definition.text)
        }
        return context.source.success(component)
    }

    private fun give(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val profiles = GameProfileArgument.getGameProfiles(context, "players")
        if (!NametagManager.hasDefinition(id) && profiles.none { NametagManager.getOverride(it, id) != null }) {
            throw NO_SUCH_NAMETAG.create(id)
        }
        for (profile in profiles) {
            NametagManager.updateOverride(profile, id) { it.copy(enabled = true) }
        }
        return this.finishOverride(context, profiles, "Successfully gave nametag '$id' to ${describe(profiles)}")
    }

    private fun revoke(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val profiles = GameProfileArgument.getGameProfiles(context, "players")
        for (profile in profiles) {
            NametagManager.updateOverride(profile, id) { it.copy(enabled = false) }
        }
        return this.finishOverride(context, profiles, "Successfully revoked nametag '$id' from ${describe(profiles)}")
    }

    private fun setText(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val text = StringArgumentType.getString(context, "text")
        val profiles = GameProfileArgument.getGameProfiles(context, "players")
        for (profile in profiles) {
            NametagManager.updateOverride(profile, id) { it.copy(text = Component.literal(text)) }
        }
        return this.finishOverride(context, profiles, "Successfully set nametag '$id' for ${describe(profiles)}")
    }

    private fun reset(context: CommandContext<CommandSourceStack>): Int {
        val id = StringArgumentType.getString(context, "id")
        val profiles = GameProfileArgument.getGameProfiles(context, "players")
        var removed = 0
        for (profile in profiles) {
            if (NametagManager.removeOverride(profile, id) != null) {
                removed++
            }
        }
        if (removed == 0) {
            throw NOTHING_TO_RESET.create()
        }
        return this.finishOverride(context, profiles, "Successfully reset nametag '$id' for ${describe(profiles)}")
    }

    private fun resetAll(context: CommandContext<CommandSourceStack>): Int {
        val profiles = GameProfileArgument.getGameProfiles(context, "players")
        var removed = 0
        for (profile in profiles) {
            if (NametagManager.removeOverrides(profile) != null) {
                removed++
            }
        }
        if (removed == 0) {
            throw NOTHING_TO_RESET.create()
        }
        return this.finishOverride(context, profiles, "Successfully reset all nametags for ${describe(profiles)}")
    }

    private fun reload(context: CommandContext<CommandSourceStack>): Int {
        NametagManager.reload(context.source.server)
        return context.source.success(Component.literal("Successfully reloaded nametags"))
    }

    private fun setDefinitionAndSave(context: CommandContext<CommandSourceStack>, id: String, definition: NametagDefinition) {
        NametagManager.setDefinition(id, definition)
        NametagManager.save()
        NametagManager.refreshAll(context.source.server)
    }

    private fun finishOverride(
        context: CommandContext<CommandSourceStack>,
        profiles: Collection<NameAndId>,
        message: String
    ): Int {
        NametagManager.save()
        for (profile in profiles) {
            NametagManager.refresh(context.source.server, profile)
        }
        return context.source.success(Component.literal(message))
    }

    private fun idSuggestions(ids: Collection<String>): List<String> {
        return ids.map(StringArgumentType::escapeIfRequired)
    }


    private fun describe(profiles: Collection<NameAndId>): String {
        return profiles.singleOrNull()?.name ?: "${profiles.size} players"
    }
}