package eu.mcomputing.mobv.diplomovapraca.ui.verification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthenticationViewModel : ViewModel() {

    // Veta, ktorú používateľ musí opísať (dočasne natvrdo)
    val authSentence = "Please verify your identity."

    // Text napísaný používateľom
    val typedText = MutableLiveData<String>("")

    // Stav overenia
    private val _state = MutableLiveData<AuthenticationState>(AuthenticationState.Idle)
    val state: LiveData<AuthenticationState> get() = _state

    // 🔥 Simulácia overenia
    fun authenticateUser() {
        val input = typedText.value?.trim() ?: ""

        if (input != authSentence) {
            _state.value = AuthenticationState.Error("Sentence does not match")
            return
        }

        viewModelScope.launch {
            _state.value = AuthenticationState.Loading

            // 🕒 Simulácia sieťového volania / ML validácie
            delay(2000)

            // Simulovaný úspech
            _state.value = AuthenticationState.Success
        }
    }

    fun reset() {
        typedText.value = ""
        _state.value = AuthenticationState.Idle
    }
}

// ---------------------------------------------------------------------------
// 🔹 Stav overenia — V TOM ISTOM SÚBORE
// ---------------------------------------------------------------------------
sealed class AuthenticationState {
    object Idle : AuthenticationState()
    object Loading : AuthenticationState()
    object Success : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}
