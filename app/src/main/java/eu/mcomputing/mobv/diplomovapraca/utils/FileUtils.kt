package eu.mcomputing.mobv.diplomovapraca.utils

import android.content.Context
import java.io.File
import java.io.IOException

object FileUtils {

    private const val LOGS_DIR_NAME = "logs"

    const val ACCELEROMETER_FILE_NAME = "sensor_accelerometer.csv"
    const val GYROSCOPE_FILE_NAME = "sensor_gyroscope.csv"

    fun getLogsDirectory(context: Context): File {
        val filesDir = context.filesDir
        val logsDir = File(filesDir, LOGS_DIR_NAME)

        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }

        return logsDir
    }

    fun getLogFile(context: Context, fileName: String): File {
        val logsDir = getLogsDirectory(context)
        return File(logsDir, fileName)
    }

    fun truncateLogsDirectory(context: Context) {
        val logsDir = getLogsDirectory(context)
        val files = logsDir.listFiles()

        files?.forEach { file ->
            if (file.isFile) {
                truncateExistingLogFile(file)
            }
        }
    }

    fun truncateLogFile(context: Context, fileName: String) {
        val logFile = getLogFile(context, fileName)
        truncateExistingLogFile(logFile)
    }

    private fun truncateExistingLogFile(logFile: File) {
        if (logFile.exists() && logFile.length() > 0) {
            try {
                val header = logFile.bufferedReader().use { it.readLine() }
                if (header != null) {
                    logFile.writeText(header + "\n")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun clearSensorLogs(context: Context) {
        truncateLogFile(context, ACCELEROMETER_FILE_NAME)
        truncateLogFile(context, GYROSCOPE_FILE_NAME)
    }
}