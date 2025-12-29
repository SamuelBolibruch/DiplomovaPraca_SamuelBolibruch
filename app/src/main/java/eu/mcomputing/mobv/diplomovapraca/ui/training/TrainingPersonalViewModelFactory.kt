package eu.mcomputing.mobv.diplomovapraca.ui.training

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.mcomputing.mobv.diplomovapraca.data.repository.AuthRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.FileRepository
import eu.mcomputing.mobv.diplomovapraca.data.repository.UserRepository // Import UserRepository
import java.lang.IllegalArgumentException

class TrainingPersonalViewModelFactory(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository // Závislosť: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Kontrola, či trieda ViewModelu je TrainingPersonalViewModel
        if (modelClass.isAssignableFrom(TrainingPersonalViewModel::class.java)) {
            // Vytvorenie inštancie s odovzdaním všetkých štyroch závislostí
            return TrainingPersonalViewModel(
                application,
                authRepository,
                fileRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Neznáma trieda ViewModelu")
    }
}