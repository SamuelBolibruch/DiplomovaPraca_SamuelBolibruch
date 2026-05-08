package eu.mcomputing.mobv.diplomovapraca.ui.training

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository
import java.lang.IllegalArgumentException

class TrainingCommonViewModelFactory(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainingCommonViewModel::class.java)) {
            return TrainingCommonViewModel(
                application,
                authRepository,
                fileRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Neznáma trieda ViewModelu")
    }
}