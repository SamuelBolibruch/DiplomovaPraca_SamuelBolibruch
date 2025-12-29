package eu.mcomputing.mobv.diplomovapraca

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore // ⬅️ Nový import pre Firestore
import com.google.firebase.storage.storage
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository // ⬅️ Import tvojho nového repozitára

// ----------------------------------------------------
// 1. AUTH REPOZITÁR (už si mal)
// ----------------------------------------------------
val authRepository: AuthRepository by lazy {
    val firebaseAuth = Firebase.auth
    AuthRepository(firebaseAuth)
}

// ----------------------------------------------------
// 2. USER REPOZITÁR (Firestore)
// ----------------------------------------------------
val userRepository: UserRepository by lazy {
    // Používa Firebase.firestore na inicializáciu!
    val firebaseFirestore = Firebase.firestore
    UserRepository(firebaseFirestore)
}

val fileRepository: FileRepository by lazy {
    // Používame Firebase.storage
    FileRepository(Firebase.storage, Firebase.firestore)
}