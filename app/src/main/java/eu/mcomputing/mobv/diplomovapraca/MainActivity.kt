package eu.mcomputing.mobv.diplomovapraca

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils // 🔥 Import FileUtils
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 VOLANIE NA VYMAZANIE LOGOV
        // Vymaže všetky staré logy pri každom spustení aplikácie.
        FileUtils.clearLogsDirectory(applicationContext)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                max(systemBars.bottom, imeInsets.bottom)
            )
            insets
        }
    }
}