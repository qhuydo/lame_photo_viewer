package com.hcmus.clc18se.photos.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        showDialog()

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_secret_photo, container, false
        )

        return binding.root
    }

    private fun showData(){
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

    @SuppressLint("CommitPrefEdits")
    private fun showDialog() {
        val li = LayoutInflater.from(context)
        val promptsView: View = li.inflate(R.layout.dialog_input_password, null)
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        alertDialogBuilder.setView(promptsView)
        val userInput = promptsView.findViewById<View>(R.id.user_password_input) as EditText

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok),
                        DialogInterface.OnClickListener { dialog, id ->
                            val user_text = userInput.text.toString()
                            checkPassword(user_text)
                            if (checkPass) {
                                showData()
                                dialog.dismiss()
                            } else {
                                val message = getString(R.string.pass_incorrect)
                                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                                builder.setCancelable(false)
                                builder.setTitle(getString(R.string.failed))
                                builder.setMessage(message)
                                builder.setNegativeButton(getString(R.string.cancel)) { dialog, id ->
                                    run {
                                        dialog.dismiss()
                                        requireActivity().onBackPressed()
                                    }
                                }
                                builder.setPositiveButton(getString(R.string.retry), { dialog, id -> showDialog() })
                                builder.create().show()
                            }
                        })
                .setNegativeButton(getString(R.string.cancel)) { dialog, id ->
                    run {
                        dialog.dismiss()
                        requireActivity().onBackPressed()
                    }
                }
                .setNeutralButton(getString(R.string.change_pass)) { dialog, id ->
                    run {
                        val sharePreferences: SharedPreferences = requireContext().getSharedPreferences("pass",MODE_PRIVATE)
                        val oldPass = sharePreferences.getString("pass",null)
                        val alertDialog = AlertDialog.Builder(requireContext())
                        alertDialog.setCancelable(false)
                        alertDialog.setTitle(getString(R.string.change_pass))
                        val oldPassInput = EditText(requireContext())
                        val newPassInput = EditText(requireContext())
                        val confirmPass = EditText(requireContext())


                        oldPassInput.transformationMethod = PasswordTransformationMethod.getInstance()
                        newPassInput.transformationMethod = PasswordTransformationMethod.getInstance()
                        confirmPass.transformationMethod = PasswordTransformationMethod.getInstance()

                        oldPassInput.hint = getString(R.string.old_pass)
                        newPassInput.hint = getString(R.string.new_pass)
                        confirmPass.hint = getString(R.string.confirm_pass)
                        val ll = LinearLayout(requireContext())
                        ll.orientation = LinearLayout.VERTICAL
                        if (oldPass != null) {
                            ll.addView(oldPassInput)
                        }
                        ll.addView(newPassInput)
                        ll.addView(confirmPass)
                        alertDialog.setView(ll)
                        alertDialog.setPositiveButton(getString(R.string.ok)
                        ) { dialog, id ->
                            run {
                                if (oldPass != null) {
                                    if (oldPass.compareTo(oldPassInput.text.toString()) == 0)
                                    {
                                        if (newPassInput.text.toString().compareTo(confirmPass.text.toString()) == 0) {
                                            sharePreferences.edit().putString("pass", newPassInput.text.toString()).apply()
                                            showData()
                                            dialog.dismiss()
                                            Toast.makeText(requireContext(),getString(R.string.change_pass_success),Toast.LENGTH_SHORT).show()
                                        }
                                        else{
                                            Toast.makeText(requireContext(),getString(R.string.new_confirm_not_same),Toast.LENGTH_SHORT).show()
                                            showDialog()
                                        }
                                    }
                                    else
                                    {
                                        Toast.makeText(requireContext(),getString(R.string.pass_incorrect),Toast.LENGTH_SHORT).show()
                                        showDialog()
                                    }
                                }
                                else{
                                    if (newPassInput.text.toString().compareTo(confirmPass.text.toString()) == 0) {
                                        sharePreferences.edit().putString("pass", newPassInput.text.toString()).apply()
                                        showData()
                                        dialog.dismiss()
                                        Toast.makeText(requireContext(),getString(R.string.change_pass_success),Toast.LENGTH_SHORT).show()
                                    }
                                    else{
                                        Toast.makeText(requireContext(),getString(R.string.new_confirm_not_same),Toast.LENGTH_SHORT).show()
                                        showDialog()
                                    }
                                }
                            }
                        }
                        alertDialog.setNegativeButton(getString(R.string.cancel)
                        ) { dialog, id -> showDialog() }

                        val alert11 = alertDialog.create()
                        alert11.show()
                    }
                }

        // create alert dialog
        val alertDialog: AlertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()
    }

    private fun checkPassword(user_input: String){
        val sharePreferences: SharedPreferences = requireContext().getSharedPreferences("pass",MODE_PRIVATE)
        val oldPass = sharePreferences.getString("pass","")
        if (oldPass?.compareTo(user_input) == 0) {
            checkPass = true
        }
    }
}