package eu.mcomputing.mobv.diplomovapraca.ui.verification

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.BehaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val behaBioAuthRepository: BehaBioAuthRepository
) : ViewModel() {

    val authSentence = "Dnes je vonku pekne, idem von so psom na dvor a budem tam asi hodinu, bude to fajn."
    val typedText = MutableLiveData("")

    private val _personalSentence = MutableLiveData("")
    val personalSentence: LiveData<String> get() = _personalSentence

    private val _state = MutableLiveData<AuthenticationState>(AuthenticationState.Idle)
    val state: LiveData<AuthenticationState> get() = _state

    fun loadPersonalSentence() {
        val uid = authRepository.getCurrentUser()?.uid

        if (uid.isNullOrBlank()) {
            _state.value = AuthenticationState.Error("Používateľ nie je prihlásený.")
            return
        }

        viewModelScope.launch {
            when (val result = userRepository.getUserProfile(uid)) {
                is Result.Success -> {
                    val sentence = result.data.personalSentence

                    if (sentence.isNullOrBlank()) {
                        _state.value = AuthenticationState.Error("Osobná veta nebola nájdená.")
                    } else {
                        _personalSentence.value = sentence
                    }
                }

                is Result.Error -> {
                    _state.value = AuthenticationState.Error(
                        result.exception.message ?: "Nepodarilo sa načítať profil používateľa."
                    )
                }
            }
        }
    }

    fun authenticateUser(
        specificKeystrokeFileName: String,
        fileType: String,
        targetSentence: String,
        sentenceType: String
    ) {
        val tag = "AuthenticationVM"

        val input = typedText.value?.trim() ?: ""
        val trimmedTarget = targetSentence.trim()

        Log.d(tag, "authenticateUser() called")
        Log.d(tag, "Input text: '$input'")
        Log.d(tag, "Target text: '$trimmedTarget'")
        Log.d(tag, "fileType=$fileType, sentenceType=$sentenceType, keystrokeFile=$specificKeystrokeFileName")

        if (trimmedTarget.isBlank()) {
            Log.e(tag, "Target sentence is blank")
            _state.value = AuthenticationState.Error("Cieľová veta nie je načítaná.")
            return
        }

        if (input != trimmedTarget) {
            Log.e(tag, "Input does not match target sentence")
            _state.value = AuthenticationState.Error("Sentence does not match")
            return
        }

        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            Log.e(tag, "User is not logged in, uid is null")
            _state.value = AuthenticationState.Error("Používateľ nie je prihlásený.")
            return
        }

        Log.d(tag, "Authenticated Firebase user uid=$uid")

        val batchId = System.currentTimeMillis().toString()
        Log.d(tag, "Generated batchId=$batchId")

        val filesToUpload = listOf(
            Pair(FileUtils.ACCELEROMETER_FILE_NAME, "accelerometer"),
            Pair(FileUtils.GYROSCOPE_FILE_NAME, "gyroscope"),
            Pair(specificKeystrokeFileName, "keystrokes")
        )

        viewModelScope.launch {
            _state.value = AuthenticationState.Loading
            Log.d(tag, "State changed to Loading")

            try {
                Log.d(tag, "Starting file upload sequence, files count=${filesToUpload.size}")

                filesToUpload.forEach { (fileName, purpose) ->
                    val localFile = FileUtils.getLogFile(application.applicationContext, fileName)

                    Log.d(
                        tag,
                        "Checking file: name=$fileName, purpose=$purpose, path=${localFile.absolutePath}, exists=${localFile.exists()}, size=${if (localFile.exists()) localFile.length() else -1}"
                    )

                    if (localFile.exists() && localFile.length() > 0) {
                        Log.d(tag, "Uploading file: $fileName")

                        when (
                            val result = fileRepository.uploadFileAndSaveMetadata(
                                localFile = localFile,
                                uid = uid,
                                fileType = fileType,
                                filePurpose = purpose,
                                batchId = batchId,
                                sentenceType = sentenceType
                            )
                        ) {
                            is Result.Success -> {
                                Log.d(tag, "Upload success for file=$fileName, truncating local file")
                                FileUtils.truncateLogFile(application.applicationContext, fileName)
                                Log.d(tag, "File truncated: $fileName")
                            }

                            is Result.Error -> {
                                Log.e(tag, "Upload failed for file=$fileName", result.exception)
                                throw result.exception
                            }
                        }
                    } else {
                        Log.e(tag, "File missing or empty: $fileName")
                        throw Exception("Súbor $fileName neexistuje alebo je prázdny.")
                    }
                }

                Log.d(tag, "All files uploaded successfully, calling authentication API")
                Log.d(tag, "Calling authenticate(uid=$uid, authType=$sentenceType)")

                when (val authApiResult = behaBioAuthRepository.authenticate(uid, sentenceType)) {
                    is Result.Success -> {
                        Log.d(tag, "Authentication API success")
                        Log.d(tag, "API raw status=${authApiResult.data.status}")
                        Log.d(tag, "API result user_id=${authApiResult.data.result.user_id}")
                        Log.d(tag, "API result auth_type=${authApiResult.data.result.auth_type}")
                        Log.d(tag, "API result threshold=${authApiResult.data.result.threshold}")
                        Log.d(tag, "API result probability_genuine=${authApiResult.data.result.probability_genuine}")
                        Log.d(tag, "API result accepted=${authApiResult.data.result.accepted}")
                        Log.d(tag, "API result decision=${authApiResult.data.result.decision}")

                        val decision = authApiResult.data.result.decision.trim().uppercase()
                        Log.d(tag, "Normalized decision=$decision")

                        typedText.postValue("")
                        Log.d(tag, "typedText cleared after API response")

                        if (decision == "ACCEPT") {
                            Log.d(tag, "Authentication accepted, setting Success state")
                            _state.value = AuthenticationState.Success
                        } else {
                            Log.e(tag, "Authentication rejected by API")
                            _state.value = AuthenticationState.Error("Authentication rejected")
                        }
                    }

                    is Result.Error -> {
                        Log.e(tag, "Authentication API call failed", authApiResult.exception)
                        typedText.postValue("")
                        Log.d(tag, "typedText cleared after API error")
                        throw authApiResult.exception
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "authenticateUser() failed", e)
                typedText.postValue("")
                Log.d(tag, "typedText cleared in catch block")
                _state.value = AuthenticationState.Error(
                    e.message ?: "Authentication upload failed"
                )
            }
        }
    }

    fun reset() {
        typedText.value = ""
        _state.value = AuthenticationState.Idle
    }
}

sealed class AuthenticationState {
    object Idle : AuthenticationState()
    object Loading : AuthenticationState()
    object Success : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}