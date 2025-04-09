package com.sanad.gemini_2_dot_5_pro_preview.todonotes;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileUtils {

    private static final String TAG = "FileUtils";
    private static final String NOTES_DIR_NAME = "MyNotes"; // Subdirectory name

    // Get the dedicated notes directory within external files dir
    public static File getNotesDirectory(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null); // App-specific external storage
        if (externalFilesDir == null) {
            Log.e(TAG, "External storage not available or writable.");
            // Fallback to internal storage? Depends on requirements.
            // For simplicity, we'll assume external is available here.
            // You might want to return context.getFilesDir() as a fallback.
            return new File(context.getFilesDir(), NOTES_DIR_NAME); // Internal fallback example
        }
        File notesDir = new File(externalFilesDir, NOTES_DIR_NAME);
        if (!notesDir.exists()) {
            if (!notesDir.mkdirs()) {
                Log.e(TAG, "Failed to create notes directory: " + notesDir.getAbsolutePath());
            } else {
                Log.d(TAG, "Created notes directory: " + notesDir.getAbsolutePath());
            }
        }
        return notesDir;
    }

    public static String saveNoteToFile(Context context, String noteId, String content) {
        File notesDir = getNotesDirectory(context);
        // Use noteId as filename, ensure it's file-system safe if needed
        File noteFile = new File(notesDir, noteId + ".md"); // Save as Markdown

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(noteFile);
            fos.write(content.getBytes());
            Log.i(TAG, "Note saved successfully to: " + noteFile.getAbsolutePath());
            return noteFile.getAbsolutePath(); // Return the path where it was saved
        } catch (IOException e) {
            Log.e(TAG, "Error saving note to file: " + noteFile.getAbsolutePath(), e);
            return null; // Indicate failure
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public static String loadNoteContent(String filePath) {
        File noteFile = new File(filePath);
        if (!noteFile.exists()) {
            Log.w(TAG, "Note file not found: " + filePath);
            return null;
        }

        FileInputStream fis = null;
        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            fis = new FileInputStream(noteFile);
            reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            // Remove the last newline if the file wasn't empty
            if (stringBuilder.length() > 0) {
                stringBuilder.setLength(stringBuilder.length() - 1);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error loading note content from: " + filePath, e);
            return null;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { /* Ignore */ }
            }
            if (fis != null) {
                try { fis.close(); } catch (IOException e) { /* Ignore */ }
            }
        }
    }

    public static List<Note> loadNotesFromDirectory(Context context) {
        File notesDir = getNotesDirectory(context);
        List<Note> notes = new ArrayList<>();
        File[] files = notesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".md")); // Load only .md files

        if (files != null) {
            for (File file : files) {
                try {
                    String id = file.getName().replace(".md", ""); // Extract ID from filename
                    String content = loadNoteContent(file.getAbsolutePath()); // Load full content
                    if (content == null) continue; // Skip if loading failed

                    // Extract title (e.g., first line) - Crude example
                    String title = content.isEmpty() ? "Untitled" : content.split("\n", 2)[0];
                    if (title.length() > 50) title = title.substring(0, 50) + "..."; // Truncate long titles

                    Note note = new Note(
                            id,
                            title,
                            content, // Store full content for editing
                            file.getAbsolutePath(),
                            file.lastModified(),
                            null
                    );
                    notes.add(note);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing note file: " + file.getName(), e);
                }
            }
            // Sort notes by last modified date, newest first
            Collections.sort(notes, (n1, n2) -> Long.compare(n2.getLastModified(), n1.getLastModified()));
        } else {
            Log.w(TAG, "listFiles returned null for directory: " + notesDir.getAbsolutePath());
        }
        return notes;
    }

    public static boolean deleteNoteFile(String filePath) {
        File noteFile = new File(filePath);
        if (noteFile.exists()) {
            if (noteFile.delete()) {
                Log.i(TAG, "Deleted note file: " + filePath);
                return true;
            } else {
                Log.e(TAG, "Failed to delete note file: " + filePath);
                return false;
            }
        } else {
            Log.w(TAG, "Attempted to delete non-existent file: " + filePath);
            return false; // File didn't exist anyway
        }
    }
}