package eu.mcomputing.mobv.diplomovapraca.ui.verification

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import eu.mcomputing.mobv.diplomovapraca.R
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.BehaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val behaBioAuthRepository: BehaBioAuthRepository
) : ViewModel() {

    private var authenticationJob: Job? = null

    val authSentence = application.getString(R.string.training_common_sentence_default)
    val typedText = MutableLiveData("")

    private val _personalSentence = MutableLiveData("")
    val personalSentence: LiveData<String> get() = _personalSentence

    private val _state = MutableLiveData<AuthenticationState>(AuthenticationState.Idle)
    val state: LiveData<AuthenticationState> get() = _state

    fun loadPersonalSentence() {
        val uid = authRepository.getCurrentUser()?.uid

        if (uid.isNullOrBlank()) {
            _state.value = AuthenticationState.Error(application.getString(R.string.auth_error_user_not_logged_in))
            return
        }

        viewModelScope.launch {
            when (val result = userRepository.getUserProfile(uid)) {
                is Result.Success -> {
                    val sentence = result.data.personalSentence

                    if (sentence.isBlank()) {
                        _state.value = AuthenticationState.Error(application.getString(R.string.auth_error_personal_sentence_not_found))
                    } else {
                        _personalSentence.value = sentence
                    }
                }

                is Result.Error -> {
                    _state.value = AuthenticationState.Error(application.getString(R.string.auth_error_profile_load_failed))
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

        if (trimmedTarget.isBlank()) {
            _state.value = AuthenticationState.Error(application.getString(R.string.auth_error_target_sentence_missing))
            return
        }

        if (input != trimmedTarget) {
            _state.value = AuthenticationState.Error(application.getString(R.string.auth_text_mismatch))
            return
        }

        val uid = authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _state.value = AuthenticationState.Error(application.getString(R.string.auth_error_user_not_logged_in))
            return
        }

        val batchId = System.currentTimeMillis().toString()

        val filesToUpload = listOf(
            Pair(FileUtils.ACCELEROMETER_FILE_NAME, "accelerometer"),
            Pair(FileUtils.GYROSCOPE_FILE_NAME, "gyroscope"),
            Pair(specificKeystrokeFileName, "keystrokes")
        )

        authenticationJob?.cancel()
        authenticationJob = viewModelScope.launch {
            _state.value = AuthenticationState.Loading

            try {
                filesToUpload.forEach { (fileName, purpose) ->
                    val localFile = FileUtils.getLogFile(application.applicationContext, fileName)

                    if (localFile.exists() && localFile.length() > 0) {
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
                                FileUtils.truncateLogFile(application.applicationContext, fileName)
                            }

                            is Result.Error -> {
                                Log.e(tag, "Upload failed for file=$fileName", result.exception)
                                throw result.exception
                            }
                        }
                    } else {
                        throw Exception("Súbor $fileName neexistuje alebo je prázdny.")
                    }
                }

                when (val authApiResult = behaBioAuthRepository.authenticate(uid, sentenceType)) {
                    is Result.Success -> {
                        val decision = authApiResult.data.result.decision.trim().uppercase()

                        typedText.postValue("")

                        if (decision == "ACCEPT") {
                            _state.value = AuthenticationState.Success(
                                application.getString(R.string.auth_result_message_success)
                            )
                        } else {
                            _state.value = AuthenticationState.Error(
                                application.getString(R.string.auth_result_message_rejected)
                            )
                        }
                    }

                    is Result.Error -> {
                        Log.e(tag, "Authentication API call failed", authApiResult.exception)
                        typedText.postValue("")
                        _state.value = AuthenticationState.Error(
                            application.getString(R.string.auth_result_message_server_error)
                        )
                    }
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "authenticateUser() failed", e)
                typedText.postValue("")
                _state.value = AuthenticationState.Error(
                    e.message ?: application.getString(R.string.auth_error_upload_failed)
                )
            } finally {
                if (authenticationJob?.isActive != true) {
                    authenticationJob = null
                }
            }
        }
    }

    fun logout() {
        authenticationJob?.cancel()
        authenticationJob = null

        authRepository.signOut()
        FileUtils.truncateLogsDirectory(application.applicationContext)

        typedText.value = ""
        _personalSentence.value = ""
        _state.value = AuthenticationState.Idle
    }

    fun reset() {
        typedText.value = ""
        _state.value = AuthenticationState.Idle
    }

    override fun onCleared() {
        authenticationJob?.cancel()
        authenticationJob = null
        super.onCleared()
    }
}

sealed class AuthenticationState {
    object Idle : AuthenticationState()
    object Loading : AuthenticationState()
    data class Success(val message: String) : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}