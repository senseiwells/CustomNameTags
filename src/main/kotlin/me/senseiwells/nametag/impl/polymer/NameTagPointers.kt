package me.senseiwells.nametag.impl.polymer

data class NameTagPointers(
    var previous: NameTagElement? = null,
    var next: NameTagElement? = null
)