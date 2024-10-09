package me.senseiwells.nametag.impl.entity

import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3

class NameTagInteractionBypass(private val player: ServerPlayer): ServerboundInteractPacket.Handler {
    override fun onInteraction(hand: InteractionHand) {
        val item = this.player.getItemInHand(hand)
        this.player.gameMode.useItem(this.player, this.player.level(), item, hand)
    }

    override fun onInteraction(hand: InteractionHand, interactionLocation: Vec3) {
        this.onInteraction(hand)
    }

    override fun onAttack() {

    }
}