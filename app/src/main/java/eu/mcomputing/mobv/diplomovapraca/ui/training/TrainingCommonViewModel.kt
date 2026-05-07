package eu.mcomputing.mobv.diplomovapraca.ui.training

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.diplomovapraca.R
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import kotlinx.coroutines.launch

class TrainingCommonViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository // ✅ Pridaná závislosť pre aktualizáciu profilu
) : AndroidViewModel(application) {

    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    val attemptCount = MutableLiveData(0)
    val requiredAttempts = 15

    private val _state = MutableLiveData<TrainingCommonState>(TrainingCommonState.Idle)
    val state: LiveData<TrainingCommonState> get() = _state

    fun sendTrainingData(
        specificKeystrokeFileName: String,
        fileType: String
    ) {
        _state.value = TrainingCommonState.Loading

        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _state.value =
                TrainingCommonState.Error(getApplication<Application>().getString(R.string.training_error_not_logged_in))
            return
        }

        val batchId = System.currentTimeMillis().toString()

        val filesToUpload = listOf(
            Pair(FileUtils.ACCELEROMETER_FILE_NAME, "accelerometer"),
            Pair(FileUtils.GYROSCOPE_FILE_NAME, "gyroscope"),
            Pair(specificKeystrokeFileName, "keystrokes")
        )

        viewModelScope.launch {
            try {
                // 1. Nahrávanie všetkých troch súborov v cykle
                filesToUpload.forEach { (fileName, purpose) ->
                    val localFile = FileUtils.getLogFile(appContext, fileName)

                    if (localFile.exists() && localFile.length() > 0) {
                        when (val result = fileRepository.uploadFileAndSaveMetadata(
                            localFile = localFile,
                            uid = uid,
                            fileType = fileType,
                            filePurpose = purpose,
                            batchId = batchId
                        )) {
                            is Result.Error -> throw result.exception
                            is Result.Success -> {
                                Log.i("UPLOAD", "Súbor $fileName úspešne nahraný.")
                                FileUtils.truncateLogFile(appContext, fileName)
                            }
                        }
                    } else {
                        Log.w("UPLOAD_SKIP", "Súbor $fileName je prázdny, preskočené.")
                    }
                }

                // 2. AKTUALIZÁCIA STAVU POUŽÍVATEĽA VO FIRESTORE (hasCommonTraining = true)
                when (val updateResult = userRepository.updateTrainingStatus(
                    uid = uid,
                    fieldName = "hasCommonTraining",
                    value = true
                )) {
                    is Result.Success -> {
                        Log.i("FIREBASE_UPDATE", "✅ hasCommonTraining úspešne aktualizovaný na TRUE.")
                        _state.value = TrainingCommonState.Success
                    }
                    is Result.Error -> {
                        Log.e("FIREBASE_UPDATE", "Chyba pri aktualizácii statusu tréningu.", updateResult.exception)
                        _state.value = TrainingCommonState.Error(getApplication<Application>().getString(R.string.training_error_status_update))
                    }
                }

            } catch (e: Exception) {
                Log.e("UPLOAD_ERROR", "Chyba pri nahrávaní dávky $batchId", e)
                _state.value = TrainingCommonState.Error(getApplication<Application>().getString(R.string.training_error_upload_failed))
            }
        }
    }

    fun reset() {
        attemptCount.value = 0
        _state.value = TrainingCommonState.Idle
    }
}

sealed class TrainingCommonState {
    object Idle : TrainingCommonState()
    object Loading : TrainingCommonState()
    object Success : TrainingCommonState()
    data class Error(val message: String) : TrainingCommonState()
}