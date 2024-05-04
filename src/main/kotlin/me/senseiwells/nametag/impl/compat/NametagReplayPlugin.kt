package me.senseiwells.nametag.impl.compat

// import me.senseiwells.nametag.impl.NameTagUtils.resendNameTagsTo
// import me.senseiwells.replay.api.ServerReplayPlugin
// import me.senseiwells.replay.chunk.ChunkRecorder
// import me.senseiwells.replay.player.PlayerRecorder

// class NametagReplayPlugin: ServerReplayPlugin {
//     override fun onPlayerReplayStart(recorder: PlayerRecorder) {
//         val player = recorder.getPlayerOrThrow()
//         for (other in recorder.server.playerList.players) {
//             // Players must at least be in the same dimension
//             if (player.level().dimension() == other.level().dimension()) {
//                 other.resendNameTagsTo(player, recorder::record)
//             }
//         }
//     }
//
//     override fun onChunkReplayStart(recorder: ChunkRecorder) {
//         // We can't really determine what nametags should be displayed
//         // to chunk recorders because they can't meet any predicates
//     }
// }