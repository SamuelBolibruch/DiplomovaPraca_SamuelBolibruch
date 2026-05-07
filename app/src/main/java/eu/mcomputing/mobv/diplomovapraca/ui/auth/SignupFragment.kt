package eu.mcomputing.mobv.diplomovapraca.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import eu.mcomputing.mobv.diplomovapraca.R
import eu.mcomputing.mobv.diplomovapraca.authRepository
import eu.mcomputing.mobv.diplomovapraca.userRepository

class SignupFragment : Fragment(R.layout.fragment_signup) {

    private val viewModel: SignupViewModel by activityViewModels {
        SignupViewModelFactory(requireActivity().application, authRepository, userRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = view.findViewById<EditText>(R.id.editTextPassword)
        val passwordConfirmInput = view.findViewById<EditText>(R.id.editTextPasswordConfirm)
        val textLogin = view.findViewById<TextView>(R.id.textLogin)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        emailInput.addTextChangedListener { viewModel.email.value = it.toString() }
        passwordInput.addTextChangedListener { viewModel.password.value = it.toString() }
        passwordConfirmInput.addTextChangedListener { viewModel.passwordConfirm.value = it.toString() }

        viewModel.email.observe(viewLifecycleOwner) { value ->
            if (emailInput.text.toString() != value) {
                emailInput.setText(value)
                emailInput.setSelection(value?.length ?: 0)
            }
        }

        viewModel.password.observe(viewLifecycleOwner) { value ->
            if (passwordInput.text.toString() != value) {
                passwordInput.setText(value)
                passwordInput.setSelection(value?.length ?: 0)
            }
        }

        viewModel.passwordConfirm.observe(viewLifecycleOwner) { value ->
            if (passwordConfirmInput.text.toString() != value) {
                passwordConfirmInput.setText(value)
                passwordConfirmInput.setSelection(value?.length ?: 0)
            }
        }

        submitButton.setOnClickListener {
            // ✅ AK SÚ DÁTA LOKÁLNE VALIDNÉ (email/heslo)...
            if (viewModel.validateFirstStepInputs()) {
                // ...tak hneď prejdeme na ďalší fragment pre zber dodatočných dát.
                findNavController().navigate(
                    R.id.action_signupFragment_to_additionalSignupInfoFragment
                )
            }
            // Ak validácia zlyhá, validateFirstStepInputs() nastaví SignupState.Error, ktorý sa zobrazí nižšie.
        }

        viewModel.signupState.observe(viewLifecycleOwner) { state ->
            when (state) {

                is SignupState.Idle -> {
                    progressBar.isVisible = false
                    submitButton.isEnabled = true
                    submitButton.text = getString(R.string.signup_button_register)
                    submitButton.alpha = 1.0f
                }

                is SignupState.Loading -> {
                    // Pre tento fragment nepotrebujeme stav Loading, lebo sa nič nenačítava.
                    // Ale ponecháme logiku Idle/Error
                    progressBar.isVisible = false
                    submitButton.isEnabled = true
                }

                // ⬇️ Tieto stavy sa v tomto fragmente nespracovávajú ⬇️
                is SignupState.Success -> {
                    // Ignorujeme, spracováva ich AdditionalSignupInfoFragment
                }

                is SignupState.Error -> {
                    progressBar.isVisible = false
                    submitButton.isEnabled = true
                    submitButton.text = getString(R.string.signup_button_register)
                    submitButton.alpha = 1.0f
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        textLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { /* disabled */ }
            })
    }
}