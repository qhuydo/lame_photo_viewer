package com.hcmus.clc18se.photos.fragments

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
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.clc18se.photos.R
import com.hcmus.clc18se.photos.adapters.AlbumListAdapter
import com.hcmus.clc18se.photos.adapters.bindSampleAlbumListRecyclerView
import com.hcmus.clc18se.photos.database.PhotosDatabase
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

    private val onClickListener = AlbumListAdapter.OnClickListener {
        viewModel.startNavigatingToPhotoList(it)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomAlbumsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.customAlbumViewModel = viewModel

        customAlbumAdapter = AlbumListAdapter(
                currentListItemView,
                currentListItemSize,
                onClickListener
        )

        binding.fabAddAlbum.setOnClickListener { addAlbum() }

        initRecyclerViews()
        initObservers()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    // TODO: refactor me
    private fun addAlbum() = MaterialDialog(requireContext()).show {
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
        positiveButton(res = R.string.ok) {
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