package eu.mcomputing.mobv.diplomovapraca.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository // ⬅️ DÔLEŽITÉ: Nový import
import java.lang.IllegalArgumentException

class SignupViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignupViewModel::class.java)) {
            // ⬇️ Vytvoríme ViewModel a odovzdáme obe závislosti ⬇️
            return SignupViewModel(authRepository, userRepository) as T
        }
        throw IllegalArgumentException("Neznáma trieda ViewModelu")
    }
}