package com.hcmus.clc18se.photos.fragments

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.hcmus.clc18se.photos.AbstractPhotosActivity
import com.hcmus.clc18se.photos.BuildConfig
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.MediaItemListAdapter
import com.hcmus.clc18se.photos.adapters.bindMediaListRecyclerView
import com.hcmus.clc18se.photos.adapters.visibleWhenEmpty
import com.hcmus.clc18se.photos.data.MediaItem
import com.hcmus.clc18se.photos.database.PhotosDatabase
import com.hcmus.clc18se.photos.databinding.DialogChangePasswordBinding
import com.hcmus.clc18se.photos.databinding.FragmentSecretPhotoBinding
import com.hcmus.clc18se.photos.utils.OnBackPressed
import com.hcmus.clc18se.photos.utils.getSpanCountForPhotoList
import com.hcmus.clc18se.photos.viewModels.PhotosViewModel
import com.hcmus.clc18se.photos.viewModels.PhotosViewModelFactory
import com.hcmus.clc18se.photos.viewModels.SecretPhotosViewModel
import com.hcmus.clc18se.photos.viewModels.SecretViewModelFactory

class SecretPhotoFragment : AbstractPhotoListFragment(R.menu.photo_list_menu), OnBackPressed {
    private lateinit var binding: FragmentSecretPhotoBinding
    private var checkPass = false

    private lateinit var photosViewModel: PhotosViewModel

    private val viewModel: SecretPhotosViewModel by activityViewModels {
        SecretViewModelFactory(requireActivity().application)
    }

    override val actionCallbacks = object : MediaItemListAdapter.ActionCallbacks {
        override fun onClick(mediaItem: MediaItem) {
            photosViewModel.startNavigatingToImageView(mediaItem)
        }

        override fun onSelectionChange() {
            invalidateCab()
        }
    }

    override fun getCurrentViewModel(): PhotosViewModel = photosViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val navGraphViewModel: PhotosViewModel by navGraphViewModels(
            (requireActivity() as AbstractPhotosActivity).getNavGraphResId()
        ) {
            PhotosViewModelFactory(
                requireActivity().application,
                PhotosDatabase.getInstance(requireContext()).photosDatabaseDao
            )
        }
        this.photosViewModel = navGraphViewModel
    }

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
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_secret_photo, container, false
        )
        binding.lifecycleOwner = this
        binding.secretPhotoViewModel = viewModel

        photoListBinding = binding.photoListLayout

        initObservers()
        initRecyclerViews()
        setHasOptionsMenu(true)

        return binding.root
    }

    private fun initRecyclerViews() {

        adapter = MediaItemListAdapter(
            actionCallbacks,
            currentListItemView,
            currentListItemSize
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.photoListLayout.apply {

            photoListRecyclerView.adapter = adapter

            (photoListRecyclerView.layoutManager as? StaggeredGridLayoutManager)?.apply {
                spanCount = getSpanCountForPhotoList(
                    resources,
                    currentListItemView,
                    currentListItemSize
                )
            }
        }
    }

    private fun initObservers() {
        viewModel.isUnlocked.observe(viewLifecycleOwner) {
            if (!it) {
                checkPass = false
                showPasswordDialog()
            }
        }
        viewModel.mediaItems.observe(viewLifecycleOwner) {
            if (it != null) {
                adapter.filterAndSubmitList(it)
                binding.placeholder.root.visibleWhenEmpty(it)
            }
        }

//        viewModel.reloadDataRequest.observe(viewLifecycleOwner) {
//            if (it) {
//                if (viewModel.isUnlocked.value == true) {
//                    viewModel.unlock()
//                }
//                viewModel.doneRequestingLoadData()
//            }
//        }

        photosViewModel.deleteSucceed.observe(viewLifecycleOwner) {
            if (it == true) {
                if (viewModel.isUnlocked.value == true) {
                    viewModel.unlock()
                }
                viewModel.doneRequestingLoadData()
            }
        }

        photosViewModel.navigateToImageView.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem != null) {
                photosViewModel.loadDataFromOtherViewModel(viewModel)

                val idx = viewModel.mediaItems.value?.indexOf(mediaItem) ?: -1

                photosViewModel.setCurrentItemView(idx)
                findNavController().navigate(
                    SecretPhotoFragmentDirections.actionSecretPhotoFragmentToPhotoViewFragment(true)
                )

                photosViewModel.doneNavigatingToImageView()
            }
        }
    }

    private fun showPasswordDialog() = MaterialDialog(requireContext())
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
                    viewModel.unlock()
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

    private fun changePassword() = MaterialDialog(requireContext())
        .noAutoDismiss()
        .show {
            lifecycleOwner(this@SecretPhotoFragment)
            val binding = DialogChangePasswordBinding.inflate(layoutInflater)

            customView(view = binding.root)
            title(R.string.change_pass)

            val sharePreferences =
                requireContext().getSharedPreferences(getString(R.string.pass_key), MODE_PRIVATE)
            val oldPass = sharePreferences.getString(getString(R.string.pass_key), null)
            if (oldPass == null) {
                binding.oldPassLayout.visibility = View.GONE
            }

            positiveButton {
                if (validateAndChangePassword(
                        binding,
                        oldPass,
                        sharePreferences
                    )
                ) return@positiveButton
                dismiss()
            }

            negativeButton { dismiss() }
        }

    private fun validateAndChangePassword(
        binding: DialogChangePasswordBinding,
        oldPass: String?,
        sharePreferences: SharedPreferences
    ): Boolean {
        val oldPassFromInput = binding.oldPass.editableText.toString()
        val newPassFromInput = binding.newPass.editableText.toString()
        val confirmFromInput = binding.confirmPass.editableText.toString()

        if (oldPass != null && oldPass != oldPassFromInput) {
            binding.oldPass.error = getString(R.string.pass_incorrect)
            return true
        }

        if (newPassFromInput != confirmFromInput) {
            binding.newPass.error = getString(R.string.new_confirm_not_same)
            return true
        }

        if (BuildConfig.DEBUG && newPassFromInput != confirmFromInput) {
            error("Assertion failed")
        }

        sharePreferences.edit()
            .putString(getString(R.string.pass_key), confirmFromInput)
            .apply()

        Toast.makeText(
            requireContext(),
            getString(R.string.change_pass_success),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    private fun checkPassword(userInput: String) {
        val sharePreferences: SharedPreferences =
            requireContext().getSharedPreferences(getString(R.string.pass_key), MODE_PRIVATE)

        val oldPass = sharePreferences.getString(getString(R.string.pass_key), "")
        if (oldPass == userInput) {
            checkPass = true
        }
    }

    override fun getCabId(): Int = R.id.cab_stub2

    override fun refreshRecyclerView() {
        binding.apply {

            val recyclerView = photoListLayout.photoListRecyclerView
            val photoList = viewModel.mediaItems.value

            recyclerView.adapter = MediaItemListAdapter(
                actionCallbacks,
                currentListItemView,
                currentListItemSize
            ).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            (recyclerView.layoutManager as? StaggeredGridLayoutManager)?.spanCount =
                getSpanCountForPhotoList(resources, currentListItemView, currentListItemSize)

            bindMediaListRecyclerView(recyclerView, photoList)
            // adapter.notifyDataSetChanged()
        }
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun getToolbarTitleRes(): Int = R.string.secret_photos_title

    override fun onPrepareCabMenu(menu: Menu) {
        super.onPrepareCabMenu(menu)
        listOf(
                menu.findItem(R.id.action_add_to_secret_album),
                menu.findItem(R.id.action_remove_secret_album),
                menu.findItem(R.id.action_add_to_custom_album),
                menu.findItem(R.id.action_add_to_favourite),
                menu.findItem(R.id.action_multiple_delete),
        ).forEach { item -> item?.isVisible = false }
        menu.findItem(R.id.action_multiple_delete_secret)?.isVisible = true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        viewModel.preventLock()
        super.onConfigurationChanged(newConfig)
    }

    override fun onDetach() {
        viewModel.lock()
        super.onDetach()
    }
}