package eu.mcomputing.mobv.diplomovapraca.utils

import android.app.Activity
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

            if (imeVisible && imeHeight > 0) {
                Logger.start(activity)
            } else {
                Logger.stop(activity)
            }

            insets
        }
    }
}