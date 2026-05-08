package eu.mcomputing.mobv.diplomovapraca.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    private val USERS_COLLECTION = "users"

    suspend fun createUserProfile(user: User): Result<Unit> {
        if (user.uid.isBlank()) {
            return Result.Error(IllegalArgumentException("UID používateľa je prázdne!"))
        }

        return try {
            db.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(user)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)

            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("Profil používateľa nebol nájdený vo Firestore."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateTrainingStatus(uid: String, fieldName: String, value: Boolean): Result<Unit> {
        if (uid.isBlank()) {
            return Result.Error(IllegalArgumentException("UID používateľa je prázdne!"))
        }

        return try {
            val updateMap = mapOf(fieldName to value)

            db.collection(USERS_COLLECTION)
                .document(uid)
                .update(updateMap)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun savePersonalSentence(uid: String, sentence: String): Result<Unit> {
        if (uid.isBlank()) {
            return Result.Error(IllegalArgumentException("UID používateľa je prázdne!"))
        }

        return try {
            val updateMap = mapOf("personalSentence" to sentence)

            db.collection(USERS_COLLECTION)
                .document(uid)
                .update(updateMap)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}