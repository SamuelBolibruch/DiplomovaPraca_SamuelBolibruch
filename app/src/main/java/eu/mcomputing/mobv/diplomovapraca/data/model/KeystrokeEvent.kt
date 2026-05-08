package eu.mcomputing.mobv.diplomovapraca.data.model

data class KeystrokeEvent(
    val userId: String,
    val roundId: Int,
    val timestampUs: Long,
    val actionType: String,
    val keyChar: Char,
    val cursorPosition: Int,
    val inputContent: String
)