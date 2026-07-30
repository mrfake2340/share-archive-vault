package com.sharearchivevault.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.sharearchivevault.R
import com.sharearchivevault.databinding.FragmentMediaBinding
import com.sharearchivevault.model.MediaItem
import kotlinx.coroutines.launch

/**
 * Reusable fragment for the Photos tab and the Videos tab.
 * Controlled by [ARG_IS_VIDEO] argument.
 */
class MediaFragment : Fragment() {

    companion object {
        private const val ARG_IS_VIDEO = "arg_is_video"

        fun newInstance(isVideo: Boolean): MediaFragment {
            return MediaFragment().apply {
                arguments = Bundle().also { it.putBoolean(ARG_IS_VIDEO, isVideo) }
            }
        }
    }

    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: MediaAdapter
    private var isVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isVideo = arguments?.getBoolean(ARG_IS_VIDEO, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSelectAllButton()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = MediaAdapter { item -> viewModel.toggleSelection(item) }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@MediaFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSelectAllButton() {
        binding.btnSelectAll.setOnClickListener {
            val current = (viewModel.state.value as? ExtractionState.Success) ?: return@setOnClickListener
            val items = if (isVideo) current.videos else current.photos
            val allSelected = items.all { it.isSelected }
            viewModel.setAllSelected(items, !allSelected)
            updateSelectAllLabel(items)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is ExtractionState.Success -> {
                            val items = if (isVideo) state.videos else state.photos
                            showContent(items)
                        }
                        else -> showEmpty()
                    }
                }
            }
        }
    }

    private fun showContent(items: List<MediaItem>) {
        if (items.isEmpty()) {
            showEmpty()
            return
        }
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.btnSelectAll.visibility = View.VISIBLE
        adapter.submitList(items.toList())
        updateSelectAllLabel(items)
    }

    private fun showEmpty() {
        binding.emptyView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.btnSelectAll.visibility = View.GONE
        val label = if (isVideo) getString(R.string.no_videos) else getString(R.string.no_photos)
        binding.tvEmpty.text = label
    }

    private fun updateSelectAllLabel(items: List<MediaItem>) {
        val allSelected = items.isNotEmpty() && items.all { it.isSelected }
        binding.btnSelectAll.text =
            if (allSelected) getString(R.string.deselect_all) else getString(R.string.select_all)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
