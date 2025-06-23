package me.senseiwells.nametag.impl.predicate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import eu.pb4.predicate.api.AbstractPredicate
import eu.pb4.predicate.api.PredicateContext
import eu.pb4.predicate.api.PredicateResult
import net.minecraft.resources.ResourceLocation

class PlayerNamePredicate(val name: String): AbstractPredicate(ID, CODEC) {
    override fun test(context: PredicateContext): PredicateResult<*> {
        val player = context.player ?: return PredicateResult.ofFailure()
        return PredicateResult.ofBoolean(player.scoreboardName == this.name)
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.withDefaultNamespace("player_name")

        val CODEC: MapCodec<PlayerNamePredicate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter(PlayerNamePredicate::name)
            ).apply(instance, ::PlayerNamePredicate)
        }
    }
}