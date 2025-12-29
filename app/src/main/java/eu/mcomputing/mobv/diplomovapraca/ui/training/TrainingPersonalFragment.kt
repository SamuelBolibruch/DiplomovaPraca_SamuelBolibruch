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
import eu.mcomputing.mobv.diplomovapraca.fileRepository
import eu.mcomputing.mobv.diplomovapraca.userRepository
import eu.mcomputing.mobv.diplomovapraca.utils.EditTextLogger
import eu.mcomputing.mobv.diplomovapraca.utils.FileUtils
import eu.mcomputing.mobv.diplomovapraca.utils.KeyboardLoggingHelper

class TrainingPersonalFragment : Fragment(R.layout.fragment_training_personal) {

    private val viewModel: TrainingPersonalViewModel by activityViewModels {
        TrainingPersonalViewModelFactory(
            requireActivity().application, // Odosielame Application Context
            authRepository,                // Odosielame Singleton AuthRepository
            fileRepository,                 // Odosielame Singleton FileRepository
            userRepository
        )
    }
    private var attemptCount = 0
    private val requiredAttempts = 1

    private val minCharacters = 40
    private val minWords = 5
    private var userSentence: String = ""

    private var keystrokeLogger: EditTextLogger? = null
    private var editRepeatField: EditText? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pôvodný kód bol odtiaľto ODSTRÁNENÝ a presunutý do btnSet.setOnClickListener
        // activity?.let {
        //     KeyboardLoggingHelper.bindToKeyboard(it, view)
        // }

        val selectWrapper = view.findViewById<View>(R.id.selectWrapper)
        val trainingWrapper = view.findViewById<View>(R.id.trainingWrapper)

        val inputSentence = view.findViewById<EditText>(R.id.inputPersonalSentence)
        val btnSet = view.findViewById<Button>(R.id.buttonSetSentence)

        val textSentence = view.findViewById<TextView>(R.id.textPersonalSentence)
        val editRepeat = view.findViewById<EditText>(R.id.editTextRepeat)
        editRepeatField = editRepeat
        val statusText = view.findViewById<TextView>(R.id.textProgress)

        val nextButton = view.findViewById<Button>(R.id.buttonNext)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val normalButtonText = getString(R.string.training_button_next)

        // STEP 1: User chooses personal sentence
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
                    "Veta je príliš krátka (min. $minCharacters znakov). Aktuálne: $charCount",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (wordCount < minWords) {
                Toast.makeText(
                    requireContext(),
                    "Veta musí mať aspoň $minWords slová. Aktuálne: $wordCount",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // ⬇️ PRIDANÉ: Uloženie vety do Firestore cez ViewModel
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

            statusText.text = getString(
                R.string.training_progress_format,
                attemptCount,
                requiredAttempts
            )

            // INICIALIZÁCIA A PRIPOJENIE LOGGERU
            // ⬇️ ÚPRAVA: Použijeme reálne UID z authRepository namiesto "1"
            val currentUid = authRepository.getCurrentUser()?.uid ?: "unknown"
            val initialRoundId = attemptCount + 1
            keystrokeLogger = EditTextLogger(
                context = requireContext(),
                userId = currentUid,
                roundId = initialRoundId,
                logFileName = "keystrokes_personal.csv"
            )
            editRepeatField?.let { keystrokeLogger?.attachTo(it) }

            // Reset field
            editRepeat.apply {
                isEnabled = true
                isFocusable = true
                isFocusableInTouchMode = true
                isCursorVisible = true
                keyListener = android.text.method.TextKeyListener.getInstance()
                text.clear()
                requestFocus() // Dobré pridať, aby kurzor hneď skočil do poľa
            }

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(btnSet.windowToken, 0)
        }

        // STEP 2: Train repetition
        editRepeat.addTextChangedListener { s ->
            if (s.toString().trim() == userSentence) {

                attemptCount++

                Toast.makeText(
                    requireContext(),
                    getString(R.string.training_toast_attempt_saved, attemptCount, requiredAttempts),
                    Toast.LENGTH_SHORT
                ).show()

                // Logika odpojenia/vymazania/pripojenia
                editRepeat.post {
                    // 1. Odpojenie loggeru pred vymazaním
                    editRepeatField?.let { keystrokeLogger?.detachFrom(it) }

                    // 2. Vymazanie textu
                    editRepeat.text.clear()

                    if (attemptCount >= requiredAttempts) {
                        // POSLEDNÉ KOLO: ZAKONČENIE TRÉNINGU
                        nextButton.isEnabled = true

                        editRepeat.isEnabled = false
                        editRepeat.isFocusable = false
                        editRepeat.isCursorVisible = false
                        editRepeat.keyListener = null

                        // Logger zostáva odpojený
                    } else {
                        // OSTATNÉ KOLÁ: PRÍPRAVA NA ĎALŠIE
                        // 3. Opätovné pripojenie pre nové kolo
                        editRepeatField?.let { keystrokeLogger?.attachTo(it) }

                        // 4. Nastavenie ID pre budúci pokus
                        keystrokeLogger?.setRoundId(attemptCount + 1)
                    }
                }

                statusText.text = getString(
                    R.string.training_progress_format,
                    attemptCount,
                    requiredAttempts
                )
            }
        }

        // STEP 3: NEXT → send from ViewModel
        nextButton.setOnClickListener {
            viewModel.finishTraining()
        }

        // STEP 4: Observe state
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {

                is TrainingPersonalState.Idle -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = attemptCount >= requiredAttempts
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1f
                }

                is TrainingPersonalState.Loading -> {
                    progressBar.isVisible = true
                    nextButton.isEnabled = false
                    nextButton.text = ""
                    nextButton.alpha = 0.8f
                }

                is TrainingPersonalState.Success -> {
                    progressBar.isVisible = false
                    nextButton.isEnabled = true
                    nextButton.text = normalButtonText
                    nextButton.alpha = 1f

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

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Zabezpečenie odpojenia loggeru pri zničení view
    override fun onDestroyView() {
        super.onDestroyView()
        editRepeatField?.let { keystrokeLogger?.detachFrom(it) }
        keystrokeLogger = null
        editRepeatField = null
    }
}