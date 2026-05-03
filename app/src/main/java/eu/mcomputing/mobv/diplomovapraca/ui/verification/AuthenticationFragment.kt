package eu.mcomputing.mobv.diplomovapraca.ui.verification

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            KeyboardLoggingHelper.bindToKeyboard(it, view)
        }

        val generalSentenceText = view.findViewById<TextView>(R.id.textAuthGeneral)
        val personalSentenceText = view.findViewById<TextView>(R.id.textAuthSentence)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupSentenceType)
        val radioGeneral = view.findViewById<RadioButton>(R.id.radioGeneralSentence)
        val radioPersonal = view.findViewById<RadioButton>(R.id.radioPersonalSentence)

        inputField = view.findViewById(R.id.editTextAuth)
        val buttonSubmit = view.findViewById<Button>(R.id.buttonSubmitAuth)
        val statusText = view.findViewById<TextView>(R.id.textAuthStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val defaultTextColor = inputField?.currentTextColor ?: 0
        val errorTextColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

        val normalButtonText = getString(R.string.auth_button)
        val keystrokeFileName = "keystrokes_authentication.csv"
        val fileType = "authentication"

        generalSentenceText.text = viewModel.authSentence
        buttonSubmit.isEnabled = false

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
            statusText.text = ""
            buttonSubmit.isEnabled = false
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
        }

        inputField?.addTextChangedListener { s ->
            val typed = s?.toString() ?: ""
            viewModel.typedText.value = typed

            val expectedPrefix = selectedSentence.take(typed.length)
            val isPrefixOk = typed == expectedPrefix

            inputField?.setTextColor(if (isPrefixOk) defaultTextColor else errorTextColor)
        }

        viewModel.typedText.observe(viewLifecycleOwner) { typed ->
            if (typed == selectedSentence && selectedSentence.isNotBlank()) {
                buttonSubmit.isEnabled = true
                statusText.text = getString(R.string.auth_ready)
            } else {
                buttonSubmit.isEnabled = false
                statusText.text = ""
            }
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

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {

                is AuthenticationState.Idle -> {
                    progressBar.isVisible = false
                    buttonSubmit.isEnabled =
                        viewModel.typedText.value == selectedSentence && selectedSentence.isNotBlank()
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f
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
                    buttonSubmit.isEnabled = true
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f

                    Toast.makeText(requireContext(), "Authentication successful!", Toast.LENGTH_SHORT).show()

                    inputField?.setText("")
                    inputField?.setTextColor(defaultTextColor)
                    inputField?.isEnabled = true

                    inputField?.let {
                        keystrokeLogger?.detachFrom(it)
                        keystrokeLogger?.setRoundId(1)
                        keystrokeLogger?.attachTo(it)
                    }

                    viewModel.reset()
                }

                is AuthenticationState.Error -> {
                    progressBar.isVisible = false
                    buttonSubmit.text = normalButtonText
                    buttonSubmit.alpha = 1.0f

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()

                    inputField?.setText("")
                    inputField?.setTextColor(defaultTextColor)
                    inputField?.isEnabled = true
                    buttonSubmit.isEnabled = false
                    statusText.text = ""

                    inputField?.let { editText ->
                        keystrokeLogger?.detachFrom(editText)
                        keystrokeLogger?.setRoundId(1)
                        keystrokeLogger?.attachTo(editText)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        inputField?.let { keystrokeLogger?.detachFrom(it) }
        keystrokeLogger = null
        inputField = null
    }
}