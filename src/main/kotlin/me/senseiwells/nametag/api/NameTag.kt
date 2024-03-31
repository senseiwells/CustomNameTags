package me.senseiwells.nametag.api

import me.senseiwells.nametag.impl.NameTagUtils
import me.senseiwells.nametag.impl.PlaceholderNameTag
import me.senseiwells.nametag.impl.ShiftHeight
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/**
 * This interface represents a custom player name tag.
 *
 * You can add any implementation of this class using
 * [NameTagUtils.addNameTag], and remove it using
 * [NameTagUtils.removeNameTag].
 *
 * @see PlaceholderNameTag
 */
interface NameTag {
    /**
     * How often the name tag should be updated, in ticks.
     */
    val updateInterval: Int

    /**
     * Whether the name tag should be visible through walls (when not sneaking).
     */
    val visibleThroughWalls: Boolean
        get() = true

    /**
     * This gets the component that will be displayed as the name tag
     * for the given [player].
     *
     * @param player The player the nametag is for.
     * @return The component to display.
     */
    fun getComponent(player: ServerPlayer): Component

    /**
     * This gets the height shift of the name tag.
     * You should change this depending on the height
     * of your nametag, by default, it should be [ShiftHeight.Medium].
     *
     * @return The height shift of the nametag.
     */
    fun getShift(): ShiftHeight

    /**
     * This method determines whether the [observee]'s nametag
     * should be visible to the [observer].
     *
     * @param observer The player observing the nametag.
     * @param observee The player whose nametag is being observed.
     * @return Whether the nametag should be visible.
     */
    fun isObservable(observee: ServerPlayer, observer: ServerPlayer): Boolean
}