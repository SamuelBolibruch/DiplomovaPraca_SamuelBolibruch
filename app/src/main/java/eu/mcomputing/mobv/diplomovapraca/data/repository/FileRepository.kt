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

    /**
     * Nahrá jeden lokálny súbor do Firebase Storage a uloží jeho metadáta do Firestore.
     * Cesta v Storage obsahuje unikátne batchId, aby sa zabránilo prepisovaniu.
     */
    suspend fun uploadFileAndSaveMetadata(
        localFile: File,
        uid: String,
        fileType: String,
        filePurpose: String,
        batchId: String // Unikátne ID pre celú dávku (napr. timestamp)
    ): Result<Unit> {

        if (uid.isBlank() || batchId.isBlank() || !localFile.exists()) {
            return Result.Error(IllegalArgumentException("Chýba UID, BatchId alebo súbor neexistuje."))
        }

        return try {
            // 1. Definovanie UNIKÁTNEJ cesty v Storage
            // Path: csv_uploads/{UID}/{fileType}/{BATCH_ID}/{file_name.csv}
            val remotePath = "$STORAGE_BASE_PATH/$uid/$fileType/$batchId/${localFile.name}"
            val storageRef = storage.reference.child(remotePath)

            // 2. Upload súboru
            storageRef.putFile(localFile.toUri()).await()

            // 3. Vytvorenie metadát
            val metadata = FileMetadata(
                userId = uid,
                fileName = localFile.name,
                storagePath = remotePath, // Uložíme celú unikátnu cestu
                fileType = fileType,
                filePurpose = filePurpose
            )

            // 4. Uloženie metadát do Firestore
            db.collection(METADATA_COLLECTION)
                .add(metadata)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}