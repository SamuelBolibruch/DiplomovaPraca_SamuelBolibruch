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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ----------------------------------------------------
// 1. AUTH REPOZITÁR
// ----------------------------------------------------
val authRepository: AuthRepository by lazy {
    val firebaseAuth = Firebase.auth
    AuthRepository(firebaseAuth)
}

// ----------------------------------------------------
// 2. USER REPOZITÁR
// ----------------------------------------------------
val userRepository: UserRepository by lazy {
    val firebaseFirestore = Firebase.firestore
    UserRepository(firebaseFirestore)
}

// ----------------------------------------------------
// 3. FILE REPOZITÁR
// ----------------------------------------------------
val fileRepository: FileRepository by lazy {
    FileRepository(Firebase.storage, Firebase.firestore)
}

// ----------------------------------------------------
// 4. RETROFIT
// ----------------------------------------------------
private val retrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl("https://murmurlessly-strawless-tina.ngrok-free.dev/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

// ----------------------------------------------------
// 5. API SERVICE
// ----------------------------------------------------
private val behaBioAuthService: BehaBioAuthService by lazy {
    retrofit.create(BehaBioAuthService::class.java)
}

// ----------------------------------------------------
// 6. BEHAVIORAL BIOMETRICS AUTH REPOZITÁR
// ----------------------------------------------------
val behaBioAuthRepository: BehaBioAuthRepository by lazy {
    BehaBioAuthRepository(behaBioAuthService)
}