package eu.mcomputing.mobv.diplomovapraca.utils

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Utility funkcie pre prácu so súborovým systémom Android aplikácie.
 */
object FileUtils {

    private const val LOGS_DIR_NAME = "logs"

    const val ACCELEROMETER_FILE_NAME = "sensor_accelerometer.csv"
    const val GYROSCOPE_FILE_NAME = "sensor_gyroscope.csv"

    /**
     * Vráti objekt File reprezentujúci adresár 'logs' v internom úložisku aplikácie
     * a zaistí, že tento adresár existuje.
     */
    fun getLogsDirectory(context: Context): File {
        val filesDir = context.filesDir
        val logsDir = File(filesDir, LOGS_DIR_NAME)

        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }

        return logsDir
    }

    /**
     * Vráti objekt File reprezentujúci súbor v adresári 'logs'
     * s dynamickým názvom. Zároveň zaistí, že adresár 'logs' existuje.
     *
     * @param context Kontext aplikácie.
     * @param fileName Požadovaný názov súboru (napr. "long_sentence.csv").
     */
    fun getLogFile(context: Context, fileName: String): File {
        val logsDir = getLogsDirectory(context)
        val logFile = File(logsDir, fileName)

        return logFile
    }

    /**
     * Vymaže všetky súbory, ktoré sa nachádzajú v adresári 'logs'.
     */
    fun clearLogsDirectory(context: Context) {
        val logsDir = getLogsDirectory(context)
        val files = logsDir.listFiles()

        files?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
    }

    /**
     * Vynuluje obsah CSV súboru: zachová iba prvý riadok (hlavičku).
     *
     * @param context Kontext aplikácie.
     * @param fileName Názov súboru, ktorý sa má vynulovať (napr. "sensor_accelerometer.csv").
     */
    fun truncateLogFile(context: Context, fileName: String) {
        val logFile = getLogFile(context, fileName)

        if (logFile.exists() && logFile.length() > 0) {
            try {
                // 1. Prečítanie hlavičky (prvého riadku)
                val header = logFile.bufferedReader().use { it.readLine() }

                if (header != null) {
                    // 2. Prepísanie celého súboru iba hlavičkou
                    // writeText() prepíše existujúci obsah
                    logFile.writeText(header + "\n")
                }
            } catch (e: IOException) {
                // Spracovanie chyby pri čítaní/zápise
                e.printStackTrace()
            }
        }
    }

    /**
     * Vynuluje obsah oboch špecifických súborov senzorov.
     */
    fun clearSensorLogs(context: Context) {
        truncateLogFile(context, ACCELEROMETER_FILE_NAME)
        truncateLogFile(context, GYROSCOPE_FILE_NAME)
    }
}