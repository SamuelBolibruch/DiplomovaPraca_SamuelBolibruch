package eu.mcomputing.mobv.diplomovapraca.ui.training

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import eu.mcomputing.mobv.diplomovapraca.R
import eu.mcomputing.mobv.diplomovapraca.authRepository
import eu.mcomputing.mobv.diplomovapraca.behaBioAuthRepository
import eu.mcomputing.mobv.diplomovapraca.fileRepository
import eu.mcomputing.mobv.diplomovapraca.userRepository
import eu.mcomputing.mobv.diplomovapraca.utils.EditTextLogger
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import eu.mcomputing.mobv.diplomovapraca.utils.KeyboardLoggingHelper
import androidx.core.content.ContextCompat

class TrainingPersonalFragment : Fragment(R.layout.fragment_training_personal) {

    private val viewModel: TrainingPersonalViewModel by activityViewModels {
        TrainingPersonalViewModelFactory(
            requireActivity().application,
            authRepository,
            fileRepository,
            userRepository,
            behaBioAuthRepository
        )
    }
    private var attemptCount = 0
    private val requiredAttempts = 15

    private val minCharacters = 75
    private val minWords = 10
    private var userSentence: String = ""

    private var keystrokeLogger: EditTextLogger? = null
    private var editRepeatField: EditText? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectWrapper = view.findViewById<View>(R.id.selectWrapper)
        val trainingWrapper = view.findViewById<View>(R.id.trainingWrapper)

        val inputSentence = view.findViewById<EditText>(R.id.inputPersonalSentence)
        val btnSet = view.findViewById<Button>(R.id.buttonSetSentence)

        val textSentence = view.findViewById<TextView>(R.id.textPersonalSentence)
        val editRepeat = view.findViewById<EditText>(R.id.editTextRepeat)
        editRepeatField = editRepeat

        val defaultTextColor = editRepeat.currentTextColor
        val errorTextColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

        val statusText = view.findViewById<TextView>(R.id.textProgress)

        val nextButton = view.findViewById<Button>(R.id.buttonNext)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val normalButtonText = getString(R.string.training_button_next)

        fun renderProgressStatus() {
            statusText.text = getString(
                R.string.training_progress_format,
                attemptCount,
                requiredAttempts
            )
        }

        btnSet.setOnClickListener {
            val typed = inputSentence.text.toString().trim()

            val wordCount = if (typed.isEmpty()) 0 else typed.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val charCount = typed.length

            if (typed.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.training_toast_empty_sentence),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (charCount < minCharacters) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.training_toast_sentence_too_short, minCharacters, charCount),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (wordCount < minWords) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.training_toast_sentence_too_few_words, minWords, wordCount),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            userSentence = typed
            viewModel.saveUserSentence(userSentence)

            selectWrapper.visibility = View.GONE
            trainingWrapper.visibility = View.VISIBLE

            activity?.let {
                KeyboardLoggingHelper.bindToKeyboard(it, view)
            }

            textSentence.text = userSentence
            attemptCount = 0
            nextButton.isEnabled = false

            renderProgressStatus()

            val currentUid = authRepository.getCurrentUser()?.uid ?: "unknown"
            val initialRoundId = attemptCount + 1
            keystrokeLogger = EditTextLogger(
                context = requireContext(),
                userId = currentUid,
                roundId = initialRoundId,
                logFileName = "keystrokes_personal.csv"
            )
            editRepeatField?.let { keystrokeLogger?.attachTo(it) }

            editRepeat.apply {
                isEnabled = true
                isFocusable = true
                isFocusableInTouchMode = true
                isCursorVisible = true
                keyListener = android.text.method.TextKeyListener.getInstance()
                text.clear()
                requestFocus()
            }

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(btnSet.windowToken, 0)
        }

        editRepeat.addTextChangedListener { s ->

            if (userSentence.isBlank()) {
                editRepeat.setTextColor(defaultTextColor)
                return@addTextChangedListener
            }

            val typed = s?.toString() ?: ""

            val expectedPrefix = userSentence.take(typed.length)
            val isPrefixOk = typed == expectedPrefix

            editRepeat.setTextColor(if (isPrefixOk) defaultTextColor else errorTextColor)

            if (typed.trim() == userSentence) {

                attemptCount++

                editRepeat.post {
                    editRepeatField?.let { keystrokeLogger?.detachFrom(it) }

                    editRepeat.text.clear()
                    editRepeat.setTextColor(defaultTextColor)

                    if (attemptCount >= requiredAttempts) {
                        nextButton.isEnabled = true

                        editRepeat.isEnabled = false
                        editRepeat.isFocusable = false
                        editRepeat.isCursorVisible = false
                        editRepeat.keyListener = null
                    } else {
                        editRepeatField?.let { keystrokeLogger?.attachTo(it) }
                        keystrokeLogger?.setRoundId(attemptCount + 1)
                    }
                }

                renderProgressStatus()
            }
        }

        nextButton.setOnClickListener {
            viewModel.finishTraining()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->

            when (state) {

                is TrainingPersonalState.Idle -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = attemptCount >= requiredAttempts
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1f
                    renderProgressStatus()
                }

                is TrainingPersonalState.Uploading -> {
                    progressBar.isVisible = true
                    nextButton.isEnabled = false
                    nextButton.text = ""
                    nextButton.alpha = 0.8f
                    statusText.text = getString(R.string.training_personal_uploading_status)
                }

                is TrainingPersonalState.TrainingModels -> {
                    progressBar.isVisible = true
                    nextButton.isEnabled = false
                    nextButton.text = ""
                    nextButton.alpha = 0.8f
                    statusText.text = getString(R.string.training_personal_model_training_status)
                }

                is TrainingPersonalState.Success -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = true
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1f
                    statusText.text = getString(R.string.training_personal_model_training_success)

                    FileUtils.clearSensorLogs(requireContext())

                    findNavController().navigate(
                        R.id.action_trainingPersonalFragment_to_authenticationFragment
                    )

                    viewModel.clear()
                }

                is TrainingPersonalState.Error -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = true
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1f
                    statusText.text = state.message

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        editRepeatField?.let { keystrokeLogger?.detachFrom(it) }
        keystrokeLogger = null
        editRepeatField = null
    }
}