package eu.mcomputing.mobv.diplomovapraca.ui.verification

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import eu.mcomputing.mobv.diplomovapraca.R

class AuthenticationFragment : Fragment(R.layout.fragment_authentication) {

    private val viewModel: AuthenticationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sentenceText = view.findViewById<TextView>(R.id.textAuthSentence)
        val inputField = view.findViewById<EditText>(R.id.editTextAuth)
        val buttonSubmit = view.findViewById<Button>(R.id.buttonSubmitAuth)
        val statusText = view.findViewById<TextView>(R.id.textAuthStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        // ✨ Text do buttonu
        val normalButtonText = getString(R.string.auth_button)

        // 🔹 Nastavíme vetu
        sentenceText.text = viewModel.authSentence
        buttonSubmit.isEnabled = false

        // 🔹 Input → VM
        inputField.addTextChangedListener { s ->
            viewModel.typedText.value = s.toString().trim()
        }

        // 🔹 Reakcia na text
        viewModel.typedText.observe(viewLifecycleOwner) { typed ->
            if (typed == viewModel.authSentence) {
                buttonSubmit.isEnabled = true
                statusText.text = getString(R.string.auth_ready)
            } else {
                buttonSubmit.isEnabled = false
                statusText.text = ""
            }
        }

        // 🔹 Submit click
        buttonSubmit.setOnClickListener {
            viewModel.authenticateUser()
        }

        // ---------------------------------------
        // 🔥 EXACT SAME STATE LOGIC AS LOGIN
        // ---------------------------------------
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {

                is AuthenticationState.Idle -> {
                    progressBar.isVisible = false
                    buttonSubmit.isEnabled = true
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f
                }

                is AuthenticationState.Loading -> {
                    progressBar.isVisible = true
                    buttonSubmit.isEnabled = false
                    buttonSubmit.text = ""
                    buttonSubmit.alpha = 0.8f
                    statusText.text = ""  // skry status text
                }

                is AuthenticationState.Success -> {
                    progressBar.isVisible = false
                    buttonSubmit.isEnabled = true
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f

                    Toast.makeText(requireContext(), "Authentication successful!", Toast.LENGTH_SHORT).show()

                    viewModel.reset()
                }

                is AuthenticationState.Error -> {
                    progressBar.isVisible = false
                    buttonSubmit.isEnabled = true
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
