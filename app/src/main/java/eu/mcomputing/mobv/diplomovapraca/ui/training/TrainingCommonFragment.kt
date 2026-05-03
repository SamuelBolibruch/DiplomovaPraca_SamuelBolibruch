package eu.mcomputing.mobv.diplomovapraca.ui.training

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
import eu.mcomputing.mobv.diplomovapraca.fileRepository
import eu.mcomputing.mobv.diplomovapraca.userRepository
import eu.mcomputing.mobv.diplomovapraca.utils.EditTextLogger
import eu.mcomputing.mobv.diplomovapraca.utils.KeyboardLoggingHelper
import androidx.core.content.ContextCompat

class TrainingCommonFragment : Fragment(R.layout.fragment_training_common) {

    private val viewModel: TrainingCommonViewModel by activityViewModels {
        TrainingCommonViewModelFactory(
            requireActivity().application, // Odosielame Application Context
            authRepository,                // Odosielame Singleton AuthRepository
            fileRepository,                 // Odosielame Singleton FileRepository
            userRepository
        )
    }
    private val commonSentence = "Dnes je vonku pekne, idem von so psom na dvor a budem tam asi hodinu, bude to fajn."

    private var keystrokeLogger: EditTextLogger? = null
    private var inputField: EditText? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            KeyboardLoggingHelper.bindToKeyboard(it, view)
        }

        val sentenceText = view.findViewById<TextView>(R.id.textSentence)
        inputField = view.findViewById<EditText>(R.id.editTextTyped)

        val defaultTextColor = inputField?.currentTextColor ?: 0
        val errorTextColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

        val statusText = view.findViewById<TextView>(R.id.textProgress)
        val nextButton = view.findViewById<Button>(R.id.buttonNext)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val normalButtonText = getString(R.string.training_button_continue)
        val keystrokeFileName = "keystrokes_common.csv" // Názov súboru pre tento tréning
        val fileType = "common_training" // Typ tréningu pre metadáta

        fun renderProgressStatus() {
            statusText.text = getString(
                R.string.training_progress_format,
                viewModel.attemptCount.value ?: 0,
                viewModel.requiredAttempts
            )
        }

        sentenceText.text = commonSentence
        nextButton.isEnabled = false

        val initialRoundId = (viewModel.attemptCount.value ?: 0) + 1
        keystrokeLogger = EditTextLogger(context = requireContext(), userId = "1", roundId = initialRoundId, logFileName = keystrokeFileName)
        inputField?.let { keystrokeLogger?.attachTo(it) }

        renderProgressStatus()

        inputField?.addTextChangedListener { s ->
            val typed = s?.toString() ?: ""

            // ✅ prefix check (rovnaká dĺžka ako typed)
            val expectedPrefix = commonSentence.take(typed.length)
            val isPrefixOk = typed == expectedPrefix

            // ✅ farba podľa chyby
            inputField?.setTextColor(if (isPrefixOk) defaultTextColor else errorTextColor)

            // pôvodná logika: posun kola iba pri presnej zhode celej vety
            if (typed.trim() == commonSentence) {

                val newCount = (viewModel.attemptCount.value ?: 0) + 1
                viewModel.attemptCount.value = newCount

                if (newCount >= viewModel.requiredAttempts) {
                    inputField?.post {
                        nextButton.isEnabled = true

                        inputField?.isEnabled = false
                        inputField?.isFocusable = false
                        inputField?.isCursorVisible = false
                        inputField?.keyListener = null

                        inputField?.let { keystrokeLogger?.detachFrom(it) }
                    }
                } else {
                    inputField?.post {
                        inputField?.let { editText ->
                            keystrokeLogger?.detachFrom(editText)
                            editText.text?.clear()
                            editText.setTextColor(defaultTextColor)
                            keystrokeLogger?.attachTo(editText)
                            keystrokeLogger?.setRoundId(newCount + 1)
                        }
                    }
                }

                renderProgressStatus()
            }
        }
        nextButton.setOnClickListener {
            // ✅ OPRAVENÉ: Volanie funkcie s povinnými parametrami
            viewModel.sendTrainingData(
                specificKeystrokeFileName = keystrokeFileName,
                fileType = fileType
            )
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {

                is TrainingCommonState.Idle -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = viewModel.attemptCount.value == viewModel.requiredAttempts
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1.0f
                    renderProgressStatus()
                }

                is TrainingCommonState.Loading -> {
                    progressBar.isVisible = true
                    nextButton.isEnabled = false
                    nextButton.text = ""
                    nextButton.alpha = 0.8f
                    statusText.text = getString(R.string.training_common_uploading_status)
                }

                is TrainingCommonState.Success -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = true
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1.0f

                    findNavController().navigate(
                        R.id.action_trainingCommonFragment_to_trainingPersonalFragment
                    )

                    viewModel.reset()
                }

                is TrainingCommonState.Error -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = true
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1.0f
                    statusText.text = state.message

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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