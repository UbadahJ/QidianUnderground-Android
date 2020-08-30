package com.ubadahj.qidianundergroud.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.ubadahj.qidianundergroud.R
import com.ubadahj.qidianundergroud.api.Api
import com.ubadahj.qidianundergroud.database.Database
import com.ubadahj.qidianundergroud.database.DatabaseInstance
import com.ubadahj.qidianundergroud.databinding.BookFragmentBinding
import com.ubadahj.qidianundergroud.databinding.ChapterItemBinding
import com.ubadahj.qidianundergroud.models.Book
import com.ubadahj.qidianundergroud.models.ChapterGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketException

class BookFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var binding: BookFragmentBinding? = null
    private lateinit var groups: List<ChapterGroup>
    private lateinit var database: Database

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BookFragmentBinding.inflate(inflater, container, false)
        viewModel.getSelectedBook().observe(viewLifecycleOwner) {
            if (it != null) init(it)
        }
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = DatabaseInstance.getInstance(requireContext())
        viewModel.getSelectedBook().value?.apply {
            init(this)
        }
    }

    private fun init(book: Book) {
        var snackbar: Snackbar? = null
        binding?.apply {
            snackbar = Snackbar.make(root, R.string.error_refreshing, Snackbar.LENGTH_SHORT)
            header.text = book.name
            lastUpdated.text = if (book.status)
                "${resources.getString(R.string.chapter)}: Completed"
            else
                "${resources.getString(R.string.last_updated)}: ${book.formattedLastUpdated}"
            chapterListView.layoutManager = GridLayoutManager(requireContext(), 2)
            libraryButton.setOnClickListener { database.add(book) }
            if (database.get().contains(book))
                libraryButton.visibility = View.GONE
        }
        GlobalScope.launch(Dispatchers.Main) {
            try {
                groups = Api.getChapters(book, true)
                binding?.chapterListView?.adapter = ChapterListingAdapter(groups) {
                    CustomTabsIntent.Builder()
                        .build()
                        .launchUrl(requireContext(), it.link.toUri())
                }
            } catch (e: SocketException) {
                snackbar?.show()
            } catch (e: IOException) {
                snackbar?.show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    class ChapterListingAdapter(
        private val groups: List<ChapterGroup>,
        private val onClick: (ChapterGroup) -> Unit
    ) : RecyclerView.Adapter<ChapterListingAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ChapterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding, groups, onClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.root.text = groups[position].text
        }

        override fun getItemCount(): Int = groups.size

        class ViewHolder(
            val binding: ChapterItemBinding,
            groups: List<ChapterGroup>,
            onClick: (ChapterGroup) -> Unit
        ) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener { onClick(groups[adapterPosition]) }
            }
        }
    }

}