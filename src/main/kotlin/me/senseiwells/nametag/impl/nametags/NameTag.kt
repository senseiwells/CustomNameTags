package me.senseiwells.nametag.impl.nametags

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

interface NameTag {
    val updateInterval: Int

    fun getComponent(player: ServerPlayer): Component

    fun isObservable(observee: ServerPlayer, observer: ServerPlayer): Boolean
}