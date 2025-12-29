package eu.mcomputing.mobv.diplomovapraca.utils

import android.app.Activity
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.behametrics.logger.Logger

object KeyboardLoggingHelper {

    fun bindToKeyboard(
        activity: Activity,
        rootView: View,
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // 🔥 PRIDANÁ LOGIKA PRE START
            if (imeVisible && imeHeight > 0) {
                // Klávesnica je viditeľná, spustiť globálne logovanie
                Log.d("KEYBOARD", "Keyboard VISIBLE → starting logger")
                Logger.start(activity)
            } else {
                // Klávesnica je skrytá, zastaviť globálne logovanie
                Log.d("KEYBOARD", "Keyboard HIDDEN → stopping logger")
                Logger.stop(activity)
            }

            insets
        }
    }
}