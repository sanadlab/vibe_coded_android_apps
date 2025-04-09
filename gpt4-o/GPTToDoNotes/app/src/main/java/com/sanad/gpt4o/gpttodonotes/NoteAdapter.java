package com.sanad.gpt4o.gpttodonotes;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<File> noteFiles;

    public NoteAdapter(List<File> noteFiles) {
        this.noteFiles = noteFiles;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        File noteFile = noteFiles.get(position);
        holder.textView.setText(noteFile.getName());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), NoteDetailActivity.class);
            intent.putExtra("notePath", noteFile.getAbsolutePath());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return noteFiles.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}