package com.sanad.gpt4o.gpttodonotes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NoteListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private List<File> noteFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        recyclerView = findViewById(R.id.recyclerViewNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadNotes();
    }

    private void loadNotes() {
        File folder = new File(getExternalFilesDir(null), "Notes");
        if (!folder.exists() || !folder.isDirectory()) {
            Toast.makeText(this, "No notes found.", Toast.LENGTH_SHORT).show();
            return;
        }

        noteFiles = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".txt")) {  // Only add text files
                    noteFiles.add(file);
                }
            }
        }

        adapter = new NoteAdapter(noteFiles);
        recyclerView.setAdapter(adapter);
    }
}