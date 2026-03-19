package eu.mcomputing.mobv.diplomovapraca.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FileMetadata(
    @JvmField
    val userId: String = "",

    @JvmField
    val fileName: String = "",

    @JvmField
    val storagePath: String = "",

    @JvmField
    val fileType: String = "",

    @JvmField
    val filePurpose: String = "",

    @JvmField
    val sentenceType: String? = null,

    @JvmField @ServerTimestamp
    val uploadDate: Date? = null
)