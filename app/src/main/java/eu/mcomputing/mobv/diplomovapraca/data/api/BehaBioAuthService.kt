package eu.mcomputing.mobv.diplomovapraca.data.api

import eu.mcomputing.mobv.diplomovapraca.data.model.BehaBioAuthRequest
import eu.mcomputing.mobv.diplomovapraca.data.model.BehaBioAuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BehaBioAuthService {

    @POST("authenticate")
    suspend fun authenticate(
        @Body request: BehaBioAuthRequest
    ): BehaBioAuthResponse
}