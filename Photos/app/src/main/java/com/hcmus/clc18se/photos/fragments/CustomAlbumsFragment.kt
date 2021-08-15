package com.hcmus.clc18se.photos.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.bindSampleAlbumListRecyclerView
import com.hcmus.clc18se.photos.data.Album
import com.hcmus.clc18se.photos.data.CustomAlbumInfo
import com.hcmus.clc18se.photos.databinding.FragmentCustomAlbumsBinding
import com.hcmus.clc18se.photos.utils.*
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModel
import com.hcmus.clc18se.photos.viewModels.CustomAlbumViewModelFactory
import kotlinx.coroutines.*

class CustomAlbumsFragment : AbstractAlbumFragment() {

    private lateinit var binding: FragmentCustomAlbumsBinding

    private val viewModel: CustomAlbumViewModel by activityViewModels {
        CustomAlbumViewModelFactory(requireActivity().application, database)
    }

    private lateinit var customAlbumAdapter: AlbumListAdapter

    private val onClickListener = object : AlbumListAdapter.AlbumListAdapterCallbacks {
        override fun onClick(album: Album) {
            viewModel.startNavigatingToPhotoList(album)
        }

        @SuppressLint("CheckResult")
        override fun onLongClick(album: Album) {
            val rename = 0
            val remove = 1


            MaterialDialog(requireContext()).show {
                listItems(R.array.custom_album_option) { _, index, _ ->
                    when (index) {
                        rename -> onActionRename(album)
                        remove -> onActionRemove(album)
                    }
                }
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        photosViewModel.clearData()
        binding = FragmentCustomAlbumsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.customAlbumViewModel = viewModel

        customAlbumAdapter = AlbumListAdapter(
                currentListItemView,
                currentListItemSize,
                allowLongClick = true,
                onClickListener
        )

        binding.fabAddAlbum.setOnClickListener { actionAddAlbum() }

        initRecyclerViews()
        initObservers()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun actionAddAlbum() = showChooseNameDialog { addNewAlbum(it) }

    private fun onActionRename(album: Album) {
        showChooseNameDialog {
            if (it.getInputField().error.isNullOrEmpty()) {
                val name = it.getInputField().text.toString().trim()

                if (album.customAlbumId != null) {
                    val newAlbum = CustomAlbumInfo(album.customAlbumId, name)
                    viewModel.updateAlbum(newAlbum)
                }
            }
        }
    }

    private fun onActionRemove(album: Album) {
        MaterialDialog(requireContext()).show {
            title(R.string.delete_warning_dialog_title)
            message(R.string.delete_warning_dialog_msg)
            positiveButton(R.string.yes) {
                val albumName = album.getName()
                if (album.customAlbumId != null && albumName != null) {
                    viewModel.removeCustomAlbum(CustomAlbumInfo(album.customAlbumId, albumName))
                }
            }
            negativeButton(R.string.no) {}
        }
    }

    @SuppressLint("CheckResult")
    private fun showChooseNameDialog(actionOfPositiveButton: (MaterialDialog) -> Unit) = MaterialDialog(requireContext()).show {
        lifecycleOwner(this@CustomAlbumsFragment)
        title(R.string.add_album_dialog_hint)
        input(
                allowEmpty = false,
                waitForPositiveButton = false
        ) { dialog, charSequence ->
            val inputFiled = dialog.getInputField()
            var isValid = true

            val name = charSequence.trim().toString()

            val nameExisted = runBlocking {
                async {
                    return@async viewModel.containsAlbumName(name)
                }.await()
            }

            if (nameExisted) {
                inputFiled.error = getString(R.string.add_album_dialog_error_name_existed)
                isValid = false
            }

            if (charSequence.isBlank()) {
                inputFiled.error = getString(R.string.add_album_dialog_error_blank_field)
                isValid = false
            }

            dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
        }
        negativeButton { cancel() }
        positiveButton(res = R.string.ok) { actionOfPositiveButton(it) }
    }

    private fun addNewAlbum(it: MaterialDialog) {
        if (it.getInputField().error.isNullOrEmpty()) {
            val name = it.getInputField().text.toString().trim()

            CoroutineScope(Dispatchers.IO).launch {
                val album = viewModel.insertNewAlbum(name)
                withContext(Dispatchers.Main) {
                    viewModel.startNavigatingToPhotoList(album)
                }
            }
        }
    }

    private fun initRecyclerViews() {
        binding.albumListLayout.apply {

            albumListRecyclerView.adapter = customAlbumAdapter

            val layoutManager = albumListRecyclerView.layoutManager as? GridLayoutManager
            layoutManager?.spanCount = getSpanCountForAlbumList(
                    resources,
                    currentListItemView,
                    currentListItemSize
            )
        }
    }

    private fun initObservers() {

        viewModel.albums.observe(viewLifecycleOwner) {
            if (it != null) {
                customAlbumAdapter.submitList(it)
            }
        }

        viewModel.navigateToPhotoList.observe(viewLifecycleOwner) { album ->
            if (album != null) {
                photosViewModel.setCustomAlbum(album)

                findNavController().navigate(
                        CustomAlbumsFragmentDirections.actionPageCustomAlbumsToPageCustomPhoto()
                )

                viewModel.doneNavigatingToPhotoList()
            }
        }
    }

    override fun refreshRecyclerView() {
        binding.apply {
            customAlbumAdapter = AlbumListAdapter(
                    currentListItemView,
                    currentListItemSize,
                    allowLongClick = true,
                    onClickListener
            )
            val recyclerView = albumListLayout.albumListRecyclerView
            val albumList = viewModel.albums

            recyclerView.adapter = customAlbumAdapter
            val layoutManager =
                    albumListLayout.albumListRecyclerView.layoutManager as? GridLayoutManager
            layoutManager?.spanCount = getSpanCountForAlbumList(
                    resources,
                    currentListItemView,
                    currentListItemSize
            )

            bindSampleAlbumListRecyclerView(recyclerView, albumList.value ?: listOf())

            customAlbumAdapter.notifyDataSetChanged()
        }
    }

    override fun getToolbarView(): Toolbar = binding.topAppBar2.fragmentToolBar

    override fun getAppbar(): AppBarLayout = binding.topAppBar2.fragmentAppBarLayout

    override fun getToolbarTitleRes(): Int = R.string.custom_album_title

    override fun getOptionMenuResId(): Int = R.menu.custom_album_menu

}