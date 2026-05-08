package eu.mcomputing.mobv.diplomovapraca.ui.auth

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import kotlinx.coroutines.launch
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.model.User

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> get() = _loginState

    private fun validateInputs(): Boolean {
        val mail = email.value
        val pass = password.value

        if (mail.isNullOrBlank() || pass.isNullOrBlank()) {
            _loginState.value = LoginState.Error("Prosím, vyplňte obe polia.")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            _loginState.value = LoginState.Error("Prosím, zadajte platnú emailovú adresu.")
            return false
        }

        if (pass.length < 6) {
            _loginState.value = LoginState.Error("Heslo musí mať aspoň 6 znakov.")
            return false
        }

        return true
    }

    fun login() {
        if (!validateInputs()) return

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val emailValue = email.value!!
            val passwordValue = password.value!!

            when (val authResult = authRepository.signIn(emailValue, passwordValue)) {

                is Result.Success -> {
                    val firebaseUser = authResult.data

                    when (val userResult = userRepository.getUserProfile(firebaseUser.uid)) {

                        is Result.Success -> {
                            val userProfile = userResult.data

                            if (userProfile.hasCommonTraining == false) {
                                _loginState.value = LoginState.NavCommonTraining
                            } else if (userProfile.hasPersonalTraining == false) {
                                _loginState.value = LoginState.NavPersonalTraining
                            } else {
                                _loginState.value = LoginState.NavHome
                            }
                        }

                        is Result.Error -> {
                            Log.e("LOGIN_ERROR", "Profil nebol nájdený vo Firestore.", userResult.exception)
                            authRepository.signOut()
                            _loginState.value = LoginState.Error("Profil používateľa nebol nájdený. Kontaktujte podporu.")
                        }
                    }
                }

                is Result.Error -> {
                    Log.e("LOGIN_ERROR", "Chyba pri prihlasovaní.", authResult.exception)
                    _loginState.value = LoginState.Error(
                        "Prihlásenie zlyhalo. Skontrolujte email a heslo."
                    )
                }
            }
        }
    }

    fun clear() {
        email.value = ""
        password.value = ""
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Error(val message: String) : LoginState()
    object NavHome : LoginState()
    object NavCommonTraining : LoginState()
    object NavPersonalTraining : LoginState()
}