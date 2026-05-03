package eu.mcomputing.mobv.diplomovapraca.data.api

import eu.mcomputing.mobv.diplomovapraca.data.model.BehaBioAuthRequest
import eu.mcomputing.mobv.diplomovapraca.data.model.BehaBioAuthResponse
import eu.mcomputing.mobv.diplomovapraca.data.model.RegisterRequest
import eu.mcomputing.mobv.diplomovapraca.data.model.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BehaBioAuthService {

    @POST("authenticate")
    suspend fun authenticate(
        @Body request: BehaBioAuthRequest
    ): BehaBioAuthResponse

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse
}