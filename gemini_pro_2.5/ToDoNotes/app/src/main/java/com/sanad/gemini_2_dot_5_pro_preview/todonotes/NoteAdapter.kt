package com.sanad.gemini_2_dot_5_pro_preview.todonotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sanad.gemini_2_dot_5_pro_preview.todonotes.databinding.ListItemNoteBinding // Import ViewBinding
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(private val onClickListener: (Note) -> Unit) :
    ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ListItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note, dateFormat)
        holder.itemView.setOnClickListener { onClickListener(note) }
    }

    class NoteViewHolder(private val binding: ListItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note, dateFormat: SimpleDateFormat) {
            binding.textViewNoteTitle.text = note.title
            binding.textViewNoteDate.text = dateFormat.format(Date(note.lastModified))
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            // Compare relevant fields for content changes
            return oldItem.title == newItem.title &&
                    oldItem.lastModified == newItem.lastModified // Simple check
        }
    }
}