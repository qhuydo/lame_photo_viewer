package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.BuildConfig
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.databinding.DialogChangePasswordBinding
import com.hcmus.clc18se.photos.databinding.FragmentSecretPhotoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class SecretPhotoFragment : BaseFragment() {
    private lateinit var binding: FragmentSecretPhotoBinding
    private val scope = CoroutineScope(Dispatchers.Default)
    private var checkPass = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = 300L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        showPasswordDialog()

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_secret_photo, container, false
        )

        return binding.root
    }

    private fun showData() {
        val cw = ContextWrapper(requireContext().applicationContext)
        val directory = cw.getDir("images", Context.MODE_PRIVATE)

        val list = ArrayList<Uri>()
        for (file in directory.listFiles()) {
            if (file != null) {
                list.add(Uri.fromFile(file))
            }
        }

        binding.listView.adapter = ArrayAdapter<Any?>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            list as List<Any?>
        )
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun getToolbarTitleRes(): Int = R.string.secret_photos_title

    private fun showPasswordDialog() {
        MaterialDialog(requireContext())
            .cancelable(false)
            .noAutoDismiss()
            .show {
                title(R.string.input_password)
                input(inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD, allowEmpty = true)
                getInputField().transformationMethod = PasswordTransformationMethod.getInstance()

                positiveButton(R.string.ok) {
                    // Validate the password
                    val text = getInputField().text.toString()
                    checkPassword(text)

                    if (checkPass) {
                        showData()
                        dismiss()
                        return@positiveButton
                    }
                    getInputField().error = getString(R.string.pass_incorrect)
                }

                neutralButton(R.string.change_pass) {
                    changePassword()
                }

                negativeButton {
                    dismiss()
                    requireActivity().onBackPressed()
                }

            }
    }

    private fun changePassword() = MaterialDialog(requireContext())
        .noAutoDismiss()
        .show {
            lifecycleOwner(this@SecretPhotoFragment)
            val binding = DialogChangePasswordBinding.inflate(layoutInflater)

            customView(view = binding.root)
            title(R.string.change_pass)

            val sharePreferences = requireContext().getSharedPreferences("pass", MODE_PRIVATE)
            val oldPass = sharePreferences.getString("pass", null)
            if (oldPass == null) {
                binding.oldPass.visibility = View.GONE
            }

            positiveButton {
                val oldPassFromInput = binding.oldPass.editableText.toString()
                val newPassFromInput = binding.newPass.editableText.toString()
                val confirmFromInput = binding.confirmPass.editableText.toString()

                if (oldPass != null && oldPass != oldPassFromInput) {
                    binding.oldPass.error = getString(R.string.pass_incorrect)
                    return@positiveButton
                }

                if (newPassFromInput != confirmFromInput) {
                    binding.newPass.error = getString(R.string.new_confirm_not_same)
                    return@positiveButton
                }

                if (BuildConfig.DEBUG && newPassFromInput != confirmFromInput) {
                    error("Assertion failed")
                }

                sharePreferences.edit()
                    .putString("pass", confirmFromInput)
                    .apply()

                Toast.makeText(
                    requireContext(),
                    getString(R.string.change_pass_success),
                    Toast.LENGTH_SHORT
                ).show()

                dismiss()
            }

            negativeButton { dismiss() }
        }

    private fun checkPassword(userInput: String) {
        val sharePreferences: SharedPreferences =
            requireContext().getSharedPreferences("pass", MODE_PRIVATE)

        val oldPass = sharePreferences.getString("pass", "")
        if (oldPass == userInput) {
            checkPass = true
        }
    }
}