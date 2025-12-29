package eu.mcomputing.mobv.diplomovapraca.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import eu.mcomputing.mobv.diplomovapraca.data.Result

class AuthRepository(private val auth: FirebaseAuth) {

    /**
     * Zaregistruje nového používateľa s emailom a heslom.
     * Vráti Result.Success s FirebaseUser alebo Result.Error s výnimkou.
     */
    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("Registrácia zlyhala."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Prihlási existujúceho používateľa s emailom a heslom.
     * Vráti Result.Success s FirebaseUser alebo Result.Error s výnimkou.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("Prihlásenie zlyhalo."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Vráti aktuálne prihláseného používateľa (alebo null, ak nikto nie je prihlásený).
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}