package eu.mcomputing.mobv.diplomovapraca.data.repository

import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.model.FileMetadata
import kotlinx.coroutines.tasks.await
import java.io.File
import java.lang.IllegalArgumentException

class FileRepository(private val storage: FirebaseStorage, private val db: FirebaseFirestore) {

    private val METADATA_COLLECTION = "file_metadata"
    private val STORAGE_BASE_PATH = "csv_uploads"

    suspend fun uploadFileAndSaveMetadata(
        localFile: File,
        uid: String,
        fileType: String,
        filePurpose: String,
        batchId: String,
        sentenceType: String? = null
    ): Result<Unit> {

        if (uid.isBlank() || batchId.isBlank() || !localFile.exists()) {
            return Result.Error(IllegalArgumentException("Chýba UID, BatchId alebo súbor neexistuje."))
        }

        return try {
            val remotePath = if (fileType == "authentication" && !sentenceType.isNullOrBlank()) {
                "$STORAGE_BASE_PATH/$uid/$fileType/$sentenceType/$batchId/${localFile.name}"
            } else {
                "$STORAGE_BASE_PATH/$uid/$fileType/$batchId/${localFile.name}"
            }
            val storageRef = storage.reference.child(remotePath)

            storageRef.putFile(localFile.toUri()).await()

            val metadata = FileMetadata(
                userId = uid,
                fileName = localFile.name,
                storagePath = remotePath,
                fileType = fileType,
                filePurpose = filePurpose,
                sentenceType = if (fileType == "authentication") sentenceType else null
            )

            db.collection(METADATA_COLLECTION)
                .add(metadata)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}