package eu.mcomputing.mobv.diplomovapraca.data.model

data class KeystrokeEvent(
    val userId: String,
    val roundId: Int,
    val timestampUs: Long, // Čas v mikrosekundách
    val actionType: String, // "Insert" alebo "Delete"
    val keyChar: Char,
    val cursorPosition: Int,
    val inputContent: String
)