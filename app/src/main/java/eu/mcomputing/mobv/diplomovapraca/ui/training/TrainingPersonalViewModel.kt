package eu.mcomputing.mobv.diplomovapraca.ui.training

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.BehaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import kotlinx.coroutines.launch

// 🔥 Stavové objekty rovnaké ako pri logine
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

    // Pre ľahší prístup ku kontextu pri I/O operáciách
    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    // Veta, ktorú si používateľ vybral
    val personalSentence = MutableLiveData<String>()

    // stav UI (Idle, Loading, Success, Error)
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
            // Použijeme metódu z UserRepository na uloženie vety
            when (val result = userRepository.savePersonalSentence(uid, sentence)) {
                is Result.Success -> {
                    Log.i("DATABASE", "Osobná veta úspešne uložená k používateľovi.")
                    personalSentence.postValue(sentence)
                }
                is Result.Error -> {
                    Log.e("DATABASE", "Chyba pri ukladaní vety", result.exception)
                    _state.postValue(TrainingPersonalState.Error("Nepodarilo sa uložiť vetu do profilu."))
                }
            }
        }
    }

    // 🧠 Funkcia volaná po dokončení X pokusov
    fun finishTraining() {
        if (_state.value is TrainingPersonalState.Uploading || _state.value is TrainingPersonalState.TrainingModels) {
            return
        }

        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _state.value =
                TrainingPersonalState.Error("Používateľ nie je prihlásený. Prosím, prihláste sa znova.")
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
                            Log.i("MODEL_TRAINING", "✅ Modely boli úspešne dotrénované pre UID=$uid")
                        }

                        is Result.Error -> {
                            Log.e("MODEL_TRAINING", "Chyba počas trénovania modelov.", registerResult.exception)
                            _state.value = TrainingPersonalState.Error(
                                "Trénovanie modelov zlyhalo. Skúste to prosím znova."
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
                        Log.i("FIREBASE_UPDATE", "✅ hasPersonalTraining úspešne aktualizovaný na TRUE.")
                        resetTrainingProgress()
                        _state.value = TrainingPersonalState.Success
                    }

                    is Result.Error -> {
                        Log.e("FIREBASE_UPDATE", "Chyba pri aktualizácii statusu tréningu.", updateResult.exception)
                        _state.value = TrainingPersonalState.Error(
                            "Modely sú dotrénované, ale nepodarilo sa uložiť stav používateľa. Skúste to znova."
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("UPLOAD_ERROR", "Chyba pri dokončovaní osobného tréningu.", e)
                _state.value = TrainingPersonalState.Error(
                    e.message ?: "Počas dokončovania osobného tréningu nastala chyba."
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
                    Log.i("UPLOAD_PERSONAL", "Súbor $fileName úspešne nahraný.")
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
        personalSentence.value = "" // Nastavíme na null, nie na prázdny reťazec
        _state.value = TrainingPersonalState.Idle
        resetTrainingProgress()
    }
}