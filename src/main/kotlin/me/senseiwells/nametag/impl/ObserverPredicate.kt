package me.senseiwells.nametag.impl

import net.minecraft.server.level.ServerPlayer

fun interface ObserverPredicate {
    fun observable(observee: ServerPlayer, observer: ServerPlayer): Boolean
}