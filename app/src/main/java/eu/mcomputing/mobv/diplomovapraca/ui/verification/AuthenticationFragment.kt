package eu.mcomputing.mobv.diplomovapraca.ui.verification

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.mcomputing.mobv.diplomovapraca.R
import eu.mcomputing.mobv.diplomovapraca.authRepository
import eu.mcomputing.mobv.diplomovapraca.behaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.fileRepository
import eu.mcomputing.mobv.diplomovapraca.userRepository
import eu.mcomputing.mobv.diplomovapraca.utils.EditTextLogger
import eu.mcomputing.mobv.diplomovapraca.utils.KeyboardLoggingHelper

class AuthenticationFragment : Fragment(R.layout.fragment_authentication) {

    private val viewModel: AuthenticationViewModel by activityViewModels {
        AuthenticationViewModelFactory(
            requireActivity().application,
            authRepository,
            fileRepository,
            userRepository,
            behaBioAuthRepository
        )
    }

    private var keystrokeLogger: EditTextLogger? = null
    private var inputField: EditText? = null
    private var selectedSentence: String = ""
    private var isPersonalMode: Boolean = false
    private var resultDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            KeyboardLoggingHelper.bindToKeyboard(it, view)
        }

        val generalSentenceText = view.findViewById<TextView>(R.id.textAuthGeneral)
        val personalSentenceText = view.findViewById<TextView>(R.id.textAuthSentence)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupSentenceType)
        val radioGeneral = view.findViewById<RadioButton>(R.id.radioGeneralSentence)

        inputField = view.findViewById(R.id.editTextAuth)
        val buttonSubmit = view.findViewById<Button>(R.id.buttonSubmitAuth)
        val buttonLogout = view.findViewById<Button>(R.id.buttonLogout)
        val statusText = view.findViewById<TextView>(R.id.textAuthStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val defaultTextColor = inputField?.currentTextColor ?: 0
        val errorTextColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

        val normalButtonText = getString(R.string.auth_button)
        val keystrokeFileName = "keystrokes_authentication.csv"
        val fileType = "authentication"

        fun renderSubmitAvailability() {
            val isExactMatch = viewModel.typedText.value == selectedSentence && selectedSentence.isNotBlank()
            buttonSubmit.isEnabled = isExactMatch
            buttonSubmit.alpha = if (isExactMatch) 1.0f else 0.55f
            statusText.text = if (isExactMatch) getString(R.string.auth_ready) else ""
        }

        fun resetInputState() {
            inputField?.setText("")
            inputField?.setTextColor(defaultTextColor)
            inputField?.isEnabled = true
            renderSubmitAvailability()

            inputField?.let { editText ->
                keystrokeLogger?.detachFrom(editText)
                keystrokeLogger?.setRoundId(1)
                keystrokeLogger?.attachTo(editText)
            }
        }

        fun showAuthenticationResultDialog(titleResId: Int, message: String) {
            resultDialog?.dismiss()

            resultDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleResId)
                .setMessage(message)
                .setPositiveButton(R.string.auth_result_dialog_ok) { dialog, _ ->
                    dialog.dismiss()
                    viewModel.reset()
                }
                .setCancelable(false)
                .create().also { dialog ->
                    dialog.setOnDismissListener {
                        if (resultDialog === dialog) {
                            resultDialog = null
                        }
                    }
                }

            resultDialog?.show()
        }

        generalSentenceText.text = viewModel.authSentence
        renderSubmitAvailability()

        // Default výber = všeobecná veta
        radioGeneral.isChecked = true
        isPersonalMode = false
        selectedSentence = viewModel.authSentence
        generalSentenceText.isVisible = true
        personalSentenceText.isVisible = false

        viewModel.loadPersonalSentence()

        viewModel.personalSentence.observe(viewLifecycleOwner) { sentence ->
            personalSentenceText.text = sentence

            if (isPersonalMode) {
                selectedSentence = sentence
            }
        }

        val currentUid = authRepository.getCurrentUser()?.uid ?: "unknown"
        val initialRoundId = 1

        keystrokeLogger = EditTextLogger(
            context = requireContext(),
            userId = currentUid,
            roundId = initialRoundId,
            logFileName = keystrokeFileName
        )

        inputField?.let { keystrokeLogger?.attachTo(it) }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            inputField?.setText("")
            inputField?.setTextColor(defaultTextColor)
            inputField?.isEnabled = true
            viewModel.typedText.value = ""

            when (checkedId) {
                R.id.radioGeneralSentence -> {
                    isPersonalMode = false
                    selectedSentence = viewModel.authSentence

                    generalSentenceText.isVisible = true
                    personalSentenceText.isVisible = false
                }

                R.id.radioPersonalSentence -> {
                    isPersonalMode = true
                    selectedSentence = viewModel.personalSentence.value ?: ""

                    generalSentenceText.isVisible = false
                    personalSentenceText.isVisible = true
                }
            }

            renderSubmitAvailability()
        }

        inputField?.addTextChangedListener { s ->
            val typed = s?.toString() ?: ""
            viewModel.typedText.value = typed

            val expectedPrefix = selectedSentence.take(typed.length)
            val isPrefixOk = typed == expectedPrefix

            inputField?.setTextColor(if (isPrefixOk) defaultTextColor else errorTextColor)
        }

        viewModel.typedText.observe(viewLifecycleOwner) { typed ->
            renderSubmitAvailability()
        }

        buttonSubmit.setOnClickListener {
            inputField?.let { keystrokeLogger?.detachFrom(it) }

            viewModel.authenticateUser(
                specificKeystrokeFileName = keystrokeFileName,
                fileType = fileType,
                targetSentence = selectedSentence,
                sentenceType = if (isPersonalMode) "personal" else "general"
            )
        }

        buttonLogout.setOnClickListener {
            resultDialog?.dismiss()
            inputField?.let { keystrokeLogger?.detachFrom(it) }
            viewModel.logout()
            findNavController().navigate(R.id.action_authenticationFragment_to_loginFragment)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {

                is AuthenticationState.Idle -> {
                    progressBar.isVisible = false
                    buttonSubmit.text = normalButtonText
                    renderSubmitAvailability()
                }

                is AuthenticationState.Loading -> {
                    progressBar.isVisible = true
                    buttonSubmit.isEnabled = false
                    buttonSubmit.text = ""
                    buttonSubmit.alpha = 0.8f
                    statusText.text = ""
                    inputField?.isEnabled = false
                }

                is AuthenticationState.Success -> {
                    progressBar.isVisible = false
                    buttonSubmit.isEnabled = false
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f

                    resetInputState()
                    showAuthenticationResultDialog(
                        titleResId = R.string.auth_result_dialog_title_success,
                        message = state.message
                    )
                }

                is AuthenticationState.Error -> {
                    progressBar.isVisible = false
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f

                    resetInputState()
                    showAuthenticationResultDialog(
                        titleResId = R.string.auth_result_dialog_title_error,
                        message = state.message
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resultDialog?.dismiss()
        resultDialog = null
        inputField?.let { keystrokeLogger?.detachFrom(it) }
        keystrokeLogger = null
        inputField = null
    }
}