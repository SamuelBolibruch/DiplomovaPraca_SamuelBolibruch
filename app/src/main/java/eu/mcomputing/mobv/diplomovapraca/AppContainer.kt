package eu.mcomputing.mobv.diplomovapraca

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import eu.mcomputing.mobv.diplomovapraca.data.api.BehaBioAuthService
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.BehaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val authRepository: AuthRepository by lazy {
    val firebaseAuth = Firebase.auth
    AuthRepository(firebaseAuth)
}

val userRepository: UserRepository by lazy {
    val firebaseFirestore = Firebase.firestore
    UserRepository(firebaseFirestore)
}

val fileRepository: FileRepository by lazy {
    FileRepository(Firebase.storage, Firebase.firestore)
}

private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()
}

private val retrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl("https://XXXX.ngrok-free.app/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

private val behaBioAuthService: BehaBioAuthService by lazy {
    retrofit.create(BehaBioAuthService::class.java)
}

val behaBioAuthRepository: BehaBioAuthRepository by lazy {
    BehaBioAuthRepository(behaBioAuthService)
}