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
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import kotlinx.coroutines.launch

// 🔥 Stavové objekty rovnaké ako pri logine
sealed class TrainingPersonalState {
    object Idle : TrainingPersonalState()
    object Loading : TrainingPersonalState()
    object Success : TrainingPersonalState()
    data class Error(val message: String) : TrainingPersonalState()
}

class TrainingPersonalViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    // Pre ľahší prístup ku kontextu pri I/O operáciách
    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    // Veta, ktorú si používateľ vybral
    val personalSentence = MutableLiveData<String>()

    // stav UI (Idle, Loading, Success, Error)
    private val _state = MutableLiveData<TrainingPersonalState>(TrainingPersonalState.Idle)
    val state: LiveData<TrainingPersonalState> get() = _state

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
        if (_state.value is TrainingPersonalState.Loading) {
            return
        }

        _state.value = TrainingPersonalState.Loading

        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _state.value =
                TrainingPersonalState.Error("Používateľ nie je prihlásený. Prosím, prihláste sa znova.")
            return
        }

        val batchId = System.currentTimeMillis().toString()

        // Súbory, ktoré nahrávame pre osobný tréning: Senzory + Keystrokes_personal
        val filesToUpload = listOf(
            Pair(FileUtils.ACCELEROMETER_FILE_NAME, "accelerometer"),
            Pair(FileUtils.GYROSCOPE_FILE_NAME, "gyroscope"),
            Pair("keystrokes_personal.csv", "keystrokes_personal") // Názov súboru z Fragmentu
        )

        viewModelScope.launch {
            try {
                // 1. Nahrávanie všetkých troch súborov
                filesToUpload.forEach { (fileName, purpose) ->
                    val localFile = FileUtils.getLogFile(appContext, fileName)

                    if (localFile.exists() && localFile.length() > 0) {
                        when (val result = fileRepository.uploadFileAndSaveMetadata(
                            localFile = localFile,
                            uid = uid,
                            fileType = "personal_training", // Typ tréningu
                            filePurpose = purpose,
                            batchId = batchId
                        )) {
                            is Result.Error -> throw result.exception
                            is Result.Success -> {
                                Log.i("UPLOAD_PERSONAL", "Súbor $fileName úspešne nahraný.")
                                // Vymazanie súboru po úspešnom nahraní
                                FileUtils.truncateLogFile(appContext, fileName)
                            }
                        }
                    } else {
                        Log.w("UPLOAD_SKIP", "Súbor $fileName je prázdny, preskočené.")
                    }
                }

                // 2. AKTUALIZÁCIA STAVU POUŽÍVATEĽA VO FIRESTORE (hasPersonalTraining = true)
                when (val updateResult = userRepository.updateTrainingStatus(
                    uid = uid,
                    fieldName = "hasPersonalTraining",
                    value = true
                )) {
                    is Result.Success -> {
                        Log.i("FIREBASE_UPDATE", "✅ hasPersonalTraining úspešne aktualizovaný na TRUE.")
                        _state.value = TrainingPersonalState.Success
                    }
                    is Result.Error -> {
                        Log.e("FIREBASE_UPDATE", "Chyba pri aktualizácii statusu tréningu.", updateResult.exception)
                        _state.value = TrainingPersonalState.Error("Nahrávanie OK, ale chyba pri aktualizácii statusu používateľa.")
                    }
                }

            } catch (e: Exception) {
                Log.e("UPLOAD_ERROR", "Chyba pri nahrávaní dávky $batchId", e)
                _state.value = TrainingPersonalState.Error("Nahrávanie dát zlyhalo: ${e.message}")
            }
        }
    }


    fun clear() {
        personalSentence.value = "" // Nastavíme na null, nie na prázdny reťazec
        _state.value = TrainingPersonalState.Idle
        // Súbory sa vymazávajú po úspešnom upload/vo Fragmente po úspechu.
    }
}