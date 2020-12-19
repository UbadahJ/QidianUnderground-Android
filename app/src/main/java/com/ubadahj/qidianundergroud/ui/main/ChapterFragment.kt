package com.ubadahj.qidianundergroud.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.ubadahj.qidianundergroud.R
import com.ubadahj.qidianundergroud.databinding.ChapterFragmentBinding
import com.ubadahj.qidianundergroud.models.ChapterGroup
import com.ubadahj.qidianundergroud.models.Resource
import com.ubadahj.qidianundergroud.ui.adapters.ChapterContentAdapter
import com.ubadahj.qidianundergroud.ui.adapters.FastScrollAdapter
import com.ubadahj.qidianundergroud.ui.adapters.MenuAdapter
import com.ubadahj.qidianundergroud.ui.adapters.items.ChapterContentItem
import com.ubadahj.qidianundergroud.ui.dialog.MenuDialog
import com.ubadahj.qidianundergroud.ui.models.MenuDialogItem

class ChapterFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var binding: ChapterFragmentBinding? = null
    private var menu: MenuDialog = MenuDialog(
            MenuAdapter().apply {
                submitList(listOf(MenuDialogItem("Loading", R.drawable.pulse)))
            }
    )

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = ChapterFragmentBinding.inflate(inflater, container, false)
        viewModel.selectedChapter?.apply {
            init(this)
        }
        return binding?.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu -> {
                menu.show(requireActivity().supportFragmentManager, null)
                true
            }
            else -> false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.simple_menu, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init(chapters: ChapterGroup) {
        setHasOptionsMenu(true)
        binding?.apply {
            (requireActivity() as AppCompatActivity).apply {
                setSupportActionBar(toolbar.appbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            errorButton.setOnClickListener { getChapterContents(chapters, true) }
        }
        getChapterContents(chapters)
    }

    private fun getChapterContents(chapters: ChapterGroup, refresh: Boolean = false) =
            viewModel.getChapterContents(requireContext(), chapters, refresh)
                    .observe(viewLifecycleOwner, {
                        binding?.apply {
                            when (it) {
                                is Resource.Success -> {
                                    chapterRecyclerView.adapter = getAdapter(it.data!!)
                                    updateMenu(it.data)
                                    toolbar.appbar.title = it.data[0].chapterName
                                    progressBar.visibility = View.GONE
                                    errorGroup.visibility = View.GONE
                                }
                                is Resource.Loading -> {
                                    toolbar.appbar.title = "Loading"
                                    progressBar.visibility = View.VISIBLE
                                    errorGroup.visibility = View.GONE
                                }
                                is Resource.Error -> {
                                    toolbar.appbar.title = "Error"
                                    menu.adapter.submitList(
                                            listOf(MenuDialogItem("Error", R.drawable.unlink))
                                    )
                                    errorGroup.visibility = View.VISIBLE
                                    progressBar.visibility = View.GONE
                                }
                            }
                        }
                    })

    private fun getAdapter(items: List<ChapterContentItem>) =
            FastScrollAdapter<ChapterContentItem>()
                    .wrap(FastAdapter.with(ChapterContentAdapter(items)).apply {
                        onTouchListener = { _, _, _, item, _ ->
                            binding?.toolbar?.appbar?.title = item.chapterName
                            true
                        }
                    })

    private fun updateMenu(items: List<ChapterContentItem>) {
        menu.adapter.submitList(items.map { MenuDialogItem(it.chapterName.toString()) })
        menu.onClick = { _, pos, _ ->
            binding?.apply {
                chapterRecyclerView.layoutManager?.let {
                    (it as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
                }
                toolbar.appbar.title = menu.adapter.currentList[pos].text
            }
            menu.dismiss()
        }
    }
}