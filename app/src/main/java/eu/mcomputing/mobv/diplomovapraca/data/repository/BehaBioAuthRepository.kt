package eu.mcomputing.mobv.diplomovapraca.data.repository

import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.api.BehaBioAuthService
import eu.mcomputing.mobv.diplomovapraca.data.model.BehaBioAuthRequest
import eu.mcomputing.mobv.diplomovapraca.data.model.BehaBioAuthResponse
import eu.mcomputing.mobv.diplomovapraca.data.model.RegisterRequest
import eu.mcomputing.mobv.diplomovapraca.data.model.RegisterResponse

class BehaBioAuthRepository(
    private val behaBioAuthService: BehaBioAuthService
) {

    suspend fun authenticate(
        uid: String,
        authType: String
    ): Result<BehaBioAuthResponse> {
        return try {
            val response = behaBioAuthService.authenticate(
                BehaBioAuthRequest(
                    uid = uid,
                    auth_type = authType
                )
            )

            if (response.status == "ok") {
                Result.Success(response)
            } else {
                Result.Error(Exception("Authentication failed: ${response.status}"))
            }

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun register(uid: String): Result<RegisterResponse> {
        return try {
            val response = behaBioAuthService.register(
                RegisterRequest(uid = uid)
            )

            if (response.status == "ok") {
                Result.Success(response)
            } else {
                Result.Error(Exception("Register failed: ${response.message}"))
            }

        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}