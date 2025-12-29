package eu.mcomputing.mobv.diplomovapraca.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import kotlinx.coroutines.launch
import eu.mcomputing.mobv.diplomovapraca.data.Result
import eu.mcomputing.mobv.diplomovapraca.data.model.User
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository

class SignupViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val passwordConfirm = MutableLiveData<String>()
    val age = MutableLiveData<Int?>()
    val gender = MutableLiveData<String?>()
    val hand = MutableLiveData<String?>()

    private val _signupState = MutableLiveData<SignupState>(SignupState.Idle)
    val signupState: LiveData<SignupState> get() = _signupState

    private fun validateInputs(): Boolean {
        if (email.value.isNullOrBlank() || password.value.isNullOrBlank() || passwordConfirm.value.isNullOrBlank() || age.value == null || gender.value.isNullOrBlank() || hand.value.isNullOrBlank()) {
            _signupState.value = SignupState.Error("Prosím, vyplňte všetky požadované polia.")
            return false
        }

        if (password.value != passwordConfirm.value) {
            _signupState.value = SignupState.Error("Heslá sa nezhodujú.")
            return false
        }

        return true
    }

    fun validateFirstStepInputs(): Boolean {
        val mail = email.value
        val pass = password.value
        val confirm = passwordConfirm.value

        if (mail.isNullOrBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            _signupState.value = SignupState.Error("Prosím, zadajte platnú emailovú adresu.")
            return false
        }

        if (pass.isNullOrBlank() || pass.length < 6) {
            _signupState.value = SignupState.Error("Heslo musí mať aspoň 6 znakov.")
            return false
        }

        if (pass != confirm) {
            _signupState.value = SignupState.Error("Heslá sa nezhodujú.")
            return false
        }

        return true
    }

    // Funkcia attemptFirstStep je odstránená. Teraz bude navigácia prebiehať priamo z Fragmentu.

    fun signup() {
        if (!validateInputs()) return

        _signupState.value = SignupState.Loading

        viewModelScope.launch {
            val emailValue = email.value!!
            val passwordValue = password.value!!

            // KROK 1: Autentifikácia
            when (val authResult = authRepository.register(emailValue, passwordValue)) {

                is Result.Success -> {
                    val firebaseUser = authResult.data

                    val initialUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: emailValue,
                        age = age.value,
                        gender = gender.value,
                        dominantHand = hand.value
                    )

                    // KROK 2: Uloženie profilu do Firestore
                    when (val userResult = userRepository.createUserProfile(initialUser)) {

                        is Result.Success -> {
                            Log.i("FIREBASE_REGISTRATION", "✅ Registrácia dokončená. Profil uložený.")
                            _signupState.value = SignupState.Success
                        }

                        is Result.Error -> {
                            Log.e("FIRESTORE_ERROR", "Chyba pri ukladaní profilu:", userResult.exception)
                            _signupState.value = SignupState.Error(
                                "Registrácia úspešná, ale chyba pri ukladaní profilu: ${userResult.exception.message}"
                            )
                        }
                    }
                }

                is Result.Error -> {
                    _signupState.value = SignupState.Error(
                        authResult.exception.message ?: "Neznáma chyba registrácie."
                    )
                }
            }
        }
    }

    fun resetSignupState() {
        _signupState.value = SignupState.Idle
    }

    fun clear() {
        email.value = ""
        password.value = ""
        passwordConfirm.value = ""
        age.value = null
        gender.value = null
        hand.value = null
        resetSignupState()
    }
}

// Musíme odstrániť aj stav FirstStepSuccess
sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    object Success : SignupState()
    data class Error(val message: String) : SignupState()
}