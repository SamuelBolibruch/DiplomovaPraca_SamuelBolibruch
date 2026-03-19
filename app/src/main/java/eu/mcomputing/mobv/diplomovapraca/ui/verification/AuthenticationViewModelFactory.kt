package eu.mcomputing.mobv.diplomovapraca.ui.verification

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.BehaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import java.lang.IllegalArgumentException

class AuthenticationViewModelFactory(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val behaBioAuthRepository: BehaBioAuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthenticationViewModel::class.java)) {

            return AuthenticationViewModel(
                application,
                authRepository,
                fileRepository,
                userRepository,
                behaBioAuthRepository
            ) as T
        }
        throw IllegalArgumentException("Neznáma trieda ViewModelu")
    }
}