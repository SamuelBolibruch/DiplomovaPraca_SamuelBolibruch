package eu.mcomputing.mobv.diplomovapraca.data.model

data class User(
    @JvmField
    val uid: String = "",

    @JvmField
    val email: String = "",

    @JvmField
    val age: Int? = null,

    @JvmField
    val gender: String? = null,

    @JvmField
    val dominantHand: String? = null,

    @JvmField
    val personalSentence: String = "",

    @JvmField
    val hasCommonTraining: Boolean = false,

    @JvmField
    val hasPersonalTraining: Boolean = false,
)