package com.sanad.gpt4o.gpttodonotes;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class NoteDetailActivity extends AppCompatActivity {

    private TextView noteContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        noteContent = findViewById(R.id.noteContent);

        String notePath = getIntent().getStringExtra("notePath");
        if (notePath != null) {
            loadNoteContent(notePath);
        }
    }

    private void loadNoteContent(String notePath) {
        File noteFile = new File(notePath);
        StringBuilder content = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(noteFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        noteContent.setText(content.toString());
    }
}