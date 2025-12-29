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

class AdditionalSignupInfoFragment : Fragment(R.layout.fragment_additional_signup_info) {

    private val viewModel: SignupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editTextAge = view.findViewById<EditText>(R.id.editTextAge)
        val radioGroupGender = view.findViewById<RadioGroup>(R.id.radioGroupGender)
        val radioGroupHand = view.findViewById<RadioGroup>(R.id.radioGroupHand)
        val confirmButton = view.findViewById<Button>(R.id.confirmButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        editTextAge.addTextChangedListener {
            viewModel.age.value = it.toString().toIntOrNull()
        }

        radioGroupGender.setOnCheckedChangeListener { _, checkedId ->
            viewModel.gender.value = when (checkedId) {
                R.id.radioMale -> "male"
                R.id.radioFemale -> "female"
                else -> null
            }
        }

        radioGroupHand.setOnCheckedChangeListener { _, checkedId ->
            viewModel.hand.value = when (checkedId) {
                R.id.radioRightHand -> "right"
                R.id.radioLeftHand -> "left"
                else -> null
            }
        }

        confirmButton.setOnClickListener {
            viewModel.signup()
        }

        viewModel.signupState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SignupState.Idle -> {
                    progressBar.isVisible = false
                    confirmButton.isEnabled = true
                    confirmButton.text = getString(R.string.additional_info_button_confirm)
                    confirmButton.alpha = 1.0f
                }

                is SignupState.Loading -> {
                    progressBar.isVisible = true
                    confirmButton.isEnabled = false
                    confirmButton.text = ""
                    confirmButton.alpha = 0.8f
                }

                is SignupState.Success -> {
                    progressBar.isVisible = false
                    confirmButton.isEnabled = true
                    confirmButton.text = getString(R.string.additional_info_button_confirm)
                    confirmButton.alpha = 1.0f
                    Toast.makeText(requireContext(), "Signup completed successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_additionalSignupInfoFragment_to_trainingCommonFragment)
                    viewModel.clear()
                }

                is SignupState.Error -> {
                    progressBar.isVisible = false
                    confirmButton.isEnabled = true
                    confirmButton.text = getString(R.string.additional_info_button_confirm)
                    confirmButton.alpha = 1.0f

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}