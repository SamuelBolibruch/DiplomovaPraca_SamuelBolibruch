package eu.mcomputing.mobv.diplomovapraca

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils // 🔥 Import FileUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 VOLANIE NA VYČISTENIE LOGOV
        // Vyprázdni obsah starých logov pri každom spustení aplikácie, ale zachová súbory aj ich hlavičky.
        FileUtils.truncateLogsDirectory(applicationContext)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }
}