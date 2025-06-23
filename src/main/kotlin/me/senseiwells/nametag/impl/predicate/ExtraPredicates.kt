package me.senseiwells.nametag.impl.predicate

import eu.pb4.predicate.api.PredicateRegistry

object ExtraPredicates {
    internal fun register() {
        PredicateRegistry.register(PlayerNamePredicate.ID, PlayerNamePredicate.CODEC)
        PredicateRegistry.register(ScoreboardTagPredicate.ID, ScoreboardTagPredicate.CODEC)
        PredicateRegistry.register(UUIDPredicate.ID, UUIDPredicate.CODEC)
    }
}