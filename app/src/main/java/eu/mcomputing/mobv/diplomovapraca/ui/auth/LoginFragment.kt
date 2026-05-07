package eu.mcomputing.mobv.diplomovapraca.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import eu.mcomputing.mobv.diplomovapraca.R
import eu.mcomputing.mobv.diplomovapraca.authRepository
import eu.mcomputing.mobv.diplomovapraca.userRepository

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by activityViewModels {
        LoginViewModelFactory(authRepository, userRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<EditText>(R.id.email)
        val passwordInput = view.findViewById<EditText>(R.id.password)
        val loginButton = view.findViewById<Button>(R.id.submitButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val textRegister = view.findViewById<TextView>(R.id.textRegister)

        emailInput.addTextChangedListener { viewModel.email.value = it.toString() }
        passwordInput.addTextChangedListener { viewModel.password.value = it.toString() }

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

        loginButton.setOnClickListener {
            viewModel.login()
        }

        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Idle -> {
                    progressBar.isVisible = false
                    loginButton.isEnabled = true
                    loginButton.text = getString(R.string.login_button_submit)
                    loginButton.alpha = 1.0f
                }

                is LoginState.Loading -> {
                    progressBar.isVisible = true
                    loginButton.isEnabled = false
                    loginButton.text = ""
                    loginButton.alpha = 0.8f
                }

                is LoginState.NavHome -> {
                    Toast.makeText(requireContext(), getString(R.string.login_toast_success), Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.action_loginFragment_to_authenticationFragment
                    )
                    viewModel.clear()
                }

                is LoginState.NavCommonTraining -> {
                    Toast.makeText(requireContext(), getString(R.string.login_toast_finish_common_training), Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.action_loginFragment_to_trainingCommonFragment
                    )
                    viewModel.clear()
                }

                is LoginState.NavPersonalTraining -> {
                    Toast.makeText(requireContext(), getString(R.string.login_toast_finish_personal_training), Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.action_loginFragment_to_trainingPersonalFragment
                    )
                    viewModel.clear()
                }

                is LoginState.Error -> {
                    progressBar.isVisible = false
                    loginButton.isEnabled = true
                    loginButton.text = getString(R.string.login_button_submit)
                    loginButton.alpha = 1.0f

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        textRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }
}