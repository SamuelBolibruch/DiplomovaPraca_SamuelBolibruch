package eu.mcomputing.mobv.diplomovapraca.data.model

data class BehaBioAuthResponse(
    val status: String,
    val result: BehaBioAuthResult
)

data class BehaBioAuthResult(
    val user_id: String,
    val auth_type: String,
    val model_path: String,
    val vector_path: String,
    val threshold: Double,
    val probability_genuine: Double,
    val accepted: Boolean,
    val decision: String
)