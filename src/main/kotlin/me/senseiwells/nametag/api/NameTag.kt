package me.senseiwells.nametag.api

import me.senseiwells.nametag.impl.ShiftHeight
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

interface NameTag {
    val updateInterval: Int

    fun getComponent(player: ServerPlayer): Component

    fun getShift(): ShiftHeight

    fun isObservable(observee: ServerPlayer, observer: ServerPlayer): Boolean
}