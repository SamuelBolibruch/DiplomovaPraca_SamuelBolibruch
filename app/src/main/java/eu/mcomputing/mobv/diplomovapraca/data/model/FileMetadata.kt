package eu.mcomputing.mobv.diplomovapraca.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FileMetadata(
    // Kto súbor nahral (UID je kľúč pre vyhľadávanie)
    @JvmField
    val userId: String = "",

    // Názov súboru (napr. sensor_accelerometer.csv)
    @JvmField
    val fileName: String = "",

    // Kde je súbor uložený v Storage
    @JvmField
    val storagePath: String = "",

    // Typ tréningu/dáta (common, personal, verification)
    @JvmField
    val fileType: String = "",

    // Typ senzora/účel (accelerometer, gyroscope, keystrokes)
    @JvmField
    val filePurpose: String = "",

    // Dátum a čas nahrávania (generuje Firebase)
    @JvmField @ServerTimestamp
    val uploadDate: Date? = null
)