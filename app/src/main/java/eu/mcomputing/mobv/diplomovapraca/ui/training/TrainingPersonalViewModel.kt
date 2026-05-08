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
import eu.mcomputing.mobv.diplomovapraca.data.repository.BehaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import kotlinx.coroutines.launch

sealed class TrainingPersonalState {
    object Idle : TrainingPersonalState()
    object Uploading : TrainingPersonalState()
    object TrainingModels : TrainingPersonalState()
    object Success : TrainingPersonalState()
    data class Error(val message: String) : TrainingPersonalState()
}

class TrainingPersonalViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val behaBioAuthRepository: BehaBioAuthRepository
) : AndroidViewModel(application) {

    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    val personalSentence = MutableLiveData<String>()

    private val _state = MutableLiveData<TrainingPersonalState>(TrainingPersonalState.Idle)
    val state: LiveData<TrainingPersonalState> get() = _state

    private val filesToUpload = listOf(
        Pair(FileUtils.ACCELEROMETER_FILE_NAME, "accelerometer"),
        Pair(FileUtils.GYROSCOPE_FILE_NAME, "gyroscope"),
        Pair("keystrokes_personal.csv", "keystrokes_personal")
    )

    private var personalTrainingBatchId: String? = null
    private val uploadedPersonalTrainingFiles = mutableSetOf<String>()
    private var areModelsTrained = false

    fun saveUserSentence(sentence: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return

        viewModelScope.launch {
            when (val result = userRepository.savePersonalSentence(uid, sentence)) {
                is Result.Success -> {
                    personalSentence.postValue(sentence)
                }
                is Result.Error -> {
                    Log.e("TrainingPersonalVM", "Chyba pri ukladaní vety", result.exception)
                    _state.postValue(TrainingPersonalState.Error(getApplication<Application>().getString(R.string.training_personal_error_sentence_save)))
                }
            }
        }
    }

    fun finishTraining() {
        if (_state.value is TrainingPersonalState.Uploading || _state.value is TrainingPersonalState.TrainingModels) {
            return
        }

        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _state.value =
                TrainingPersonalState.Error(getApplication<Application>().getString(R.string.training_error_not_logged_in))
            return
        }

        viewModelScope.launch {
            try {
                val batchId = personalTrainingBatchId ?: System.currentTimeMillis().toString().also {
                    personalTrainingBatchId = it
                }

                if (uploadedPersonalTrainingFiles.size < filesToUpload.size) {
                    _state.value = TrainingPersonalState.Uploading
                    uploadRemainingFiles(uid = uid, batchId = batchId)
                }

                _state.value = TrainingPersonalState.TrainingModels

                if (!areModelsTrained) {
                    when (val registerResult = behaBioAuthRepository.register(uid)) {
                        is Result.Success -> {
                            areModelsTrained = true
                        }

                        is Result.Error -> {
                            Log.e("TrainingPersonalVM", "Chyba počas trénovania modelov.", registerResult.exception)
                            _state.value = TrainingPersonalState.Error(
                                getApplication<Application>().getString(R.string.training_personal_error_model_training_failed)
                            )
                            return@launch
                        }
                    }
                }

                when (val updateResult = userRepository.updateTrainingStatus(
                    uid = uid,
                    fieldName = "hasPersonalTraining",
                    value = true
                )) {
                    is Result.Success -> {
                        resetTrainingProgress()
                        _state.value = TrainingPersonalState.Success
                    }

                    is Result.Error -> {
                        Log.e("TrainingPersonalVM", "Chyba pri aktualizácii statusu tréningu.", updateResult.exception)
                        _state.value = TrainingPersonalState.Error(
                            getApplication<Application>().getString(R.string.training_personal_error_state_save_failed)
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("TrainingPersonalVM", "Chyba pri dokončovaní osobného tréningu.", e)
                _state.value = TrainingPersonalState.Error(
                    e.message ?: getApplication<Application>().getString(R.string.training_personal_error_finish_failed)
                )
            }
        }
    }

    private suspend fun uploadRemainingFiles(uid: String, batchId: String) {
        filesToUpload.forEach { (fileName, purpose) ->
            if (uploadedPersonalTrainingFiles.contains(fileName)) {
                return@forEach
            }

            val localFile = FileUtils.getLogFile(appContext, fileName)

            if (!localFile.exists() || localFile.length() <= 0) {
                throw IllegalStateException("Súbor $fileName neexistuje alebo je prázdny.")
            }

            when (
                val result = fileRepository.uploadFileAndSaveMetadata(
                    localFile = localFile,
                    uid = uid,
                    fileType = "personal_training",
                    filePurpose = purpose,
                    batchId = batchId
                )
            ) {
                is Result.Error -> throw result.exception
                is Result.Success -> {
                    FileUtils.truncateLogFile(appContext, fileName)
                    uploadedPersonalTrainingFiles.add(fileName)
                }
            }
        }
    }

    private fun resetTrainingProgress() {
        personalTrainingBatchId = null
        uploadedPersonalTrainingFiles.clear()
        areModelsTrained = false
    }

    fun clear() {
        personalSentence.value = ""
        _state.value = TrainingPersonalState.Idle
        resetTrainingProgress()
    }
}