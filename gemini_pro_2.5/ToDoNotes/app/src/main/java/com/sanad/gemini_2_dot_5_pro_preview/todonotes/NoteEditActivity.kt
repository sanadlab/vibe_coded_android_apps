package com.sanad.gemini_2_dot_5_pro_preview.todonotes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.sanad.gemini_2_dot_5_pro_preview.todonotes.databinding.ActivityNoteEditBinding // Import ViewBinding generated class
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.Executors

class NoteEditActivity : AppCompatActivity() {

    // Use view binding
    private lateinit var binding: ActivityNoteEditBinding
    private var currentNote: Note? = null // Make nullable to handle potential errors
    private var isNewNote = true
    private lateinit var markwon: Markwon
    private lateinit var markwonEditor: MarkwonEditor
    private var isPreviewMode = false

    // For checking unsaved changes on back press (optional)
    private var originalContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarEdit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        initializeMarkwon()

        // Get note from intent or create a new one
        // Use Kotlin's type-safe way to get Parcelable extra
        currentNote = intent.getParcelableExtra<Note>(EXTRA_NOTE)

        if (currentNote != null) {
            // Editing existing note
            isNewNote = false
            binding.editTextNoteContent.setText(currentNote?.content ?: "")
            originalContent = currentNote?.content ?: "" // Store original content
            supportActionBar?.title = "Edit Note"
        } else {
            // Creating new note
            isNewNote = true
            currentNote = Note(
                id = UUID.randomUUID().toString(),
                title = "", // Will be generated on save
                content = "",
                filePath = "", // Will be set on save
                lastModified = System.currentTimeMillis(),
                driveId = null
            )
            originalContent = ""
            supportActionBar?.title = "New Note"
        }

        // Apply Markwon syntax highlighting
        /*
        binding.editTextNoteContent.addTextChangedListener(
            MarkwonEditorTextWatcher.withPrecomputedFuture(
                markwonEditor,
                Executors.newCachedThreadPool() // Use a cached pool for parsing
            )
        )*/

        //binding.editTextNoteContent.addTextChangedListener(MarkwonEditorTextWatcher.create(markwonEditor))
        binding.editTextNoteContent.addTextChangedListener(
            MarkwonEditorTextWatcher.withProcess(markwonEditor)
        )

    }

    private fun initializeMarkwon() {
        markwon = Markwon.builder(this)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(this))
            // Add other Markwon plugins if needed
            .build()

        markwonEditor = MarkwonEditor.builder(markwon).build()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_note, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val uploadItem = menu.findItem(R.id.action_upload_drive)
        val deleteItem = menu.findItem(R.id.action_delete)
        val previewItem = menu.findItem(R.id.action_preview)

        // Enable/disable Drive upload based on sign-in state
        val account = GoogleSignIn.getLastSignedInAccount(this)
        uploadItem?.isEnabled = account != null && DriveServiceHelper.isInitialized() // Use Kotlin object accessor

        // Show delete only for existing notes
        deleteItem?.isVisible = !isNewNote

        // Toggle Preview/Edit title and icon
        if (isPreviewMode) {
            previewItem?.title = "Edit"
            previewItem?.setIcon(android.R.drawable.ic_menu_edit)
        } else {
            previewItem?.title = "Preview"
            previewItem?.setIcon(android.R.drawable.ic_menu_view)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveNote()
                true
            }
            R.id.action_preview -> {
                togglePreviewMode()
                true
            }
            R.id.action_upload_drive -> {
                uploadToDrive()
                true
            }
            R.id.action_delete -> {
                confirmAndDeleteNote()
                true
            }
            android.R.id.home -> {
                // Handle Up button press
                onBackPressedDispatcher.onBackPressed() // Use recommended way
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun togglePreviewMode() {
        isPreviewMode = !isPreviewMode
        if (isPreviewMode) {
            val markdownContent = binding.editTextNoteContent.text.toString()
            markwon.setMarkdown(binding.textViewMarkdownPreview, markdownContent)
            binding.editTextNoteContent.visibility = View.GONE
            binding.textViewMarkdownPreview.visibility = View.VISIBLE
        } else {
            binding.editTextNoteContent.visibility = View.VISIBLE
            binding.textViewMarkdownPreview.visibility = View.GONE
        }
        invalidateOptionsMenu() // Update menu item
    }

    private fun saveNote() {
        val content = binding.editTextNoteContent.text.toString()
        val noteToSave = currentNote ?: return // Safety check, should not be null here

        // Basic validation
        if (content.trim().isEmpty() && isNewNote) {
            Toast.makeText(this, "Note is empty, not saved.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        // Update note object
        noteToSave.content = content
        noteToSave.lastModified = System.currentTimeMillis()

        // Generate title
        val lines = content.trim().split("\n", limit = 2)
        var title = if (lines.isNotEmpty() && lines[0].isNotBlank()) lines[0].trim() else "Untitled"
        if (title.length > 50) {
            title = title.substring(0, 50)
        }
        noteToSave.title = title

        // Save using lifecycleScope (automatically cancels if Activity is destroyed)
        lifecycleScope.launch { // Launch on Main thread by default
            val savedPath = withContext(Dispatchers.IO) { // Switch to background thread for file IO
                FileUtils.saveNoteToFile(this@NoteEditActivity, noteToSave.id, content)
            }

            // Back on Main thread implicitly after withContext
            if (savedPath != null) {
                noteToSave.filePath = savedPath // Update path
                Toast.makeText(this@NoteEditActivity, "Note saved", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK) // Indicate success
                finish()
            } else {
                Toast.makeText(this@NoteEditActivity, "Error saving note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmAndDeleteNote() {
        if (isNewNote || currentNote == null) return

        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteNote() } // Use lambda
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteNote() {
        val noteToDelete = currentNote ?: return // Safety check
        val filePath = noteToDelete.filePath
        val driveId = noteToDelete.driveId

        lifecycleScope.launch { // Launch on Main
            val deletedLocal = withContext(Dispatchers.IO) { // Switch to IO
                if (filePath.isNotBlank()) {
                    FileUtils.deleteNoteFile(filePath)
                } else {
                    Log.w(TAG, "File path was blank, cannot delete local file for note ID: ${noteToDelete.id}")
                    false // Consider it not deleted if path is invalid
                }
            }

            var deletedRemote = false
            if (driveId != null && DriveServiceHelper.isInitialized()) {
                try {
                    withContext(Dispatchers.IO) { // IO for network call
                        // Assuming DriveServiceHelper has a suspend fun or handles threading internally
                        // For demonstration, assuming direct call that blocks or is suspend
                        // DriveServiceHelper.deleteFile(driveId) // Replace with actual call
                        Log.w(TAG, "Drive delete functionality not fully implemented in this example.")
                        // Simulate success/failure for now
                        // deletedRemote = true
                    }
                    Log.i(TAG, "Simulated deletion from Drive: $driveId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete note from Drive: $driveId", e)
                    // Show error message related to Drive deletion failure
                    Toast.makeText(this@NoteEditActivity, "Failed to delete from Drive", Toast.LENGTH_SHORT).show()
                }
            }

            // Back on Main
            if (deletedLocal) {
                Toast.makeText(this@NoteEditActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK) // Indicate change
                finish()
            } else {
                Toast.makeText(this@NoteEditActivity, "Failed to delete note file", Toast.LENGTH_SHORT).show()
                // Don't finish if local delete failed
            }
        }
    }

    private fun uploadToDrive() {
        val noteToUpload = currentNote ?: return // Safety check

        if (!DriveServiceHelper.isInitialized()) {
            Toast.makeText(this, "Please sign in to Google Drive first.", Toast.LENGTH_LONG).show()
            return
        }

        if (noteToUpload.filePath.isBlank()) {
            Toast.makeText(this, "Please save the note locally before uploading.", Toast.LENGTH_SHORT).show()
            // Consider calling saveNote() first and then triggering upload if successful
            return
        }

        // Show progress indicator
        Toast.makeText(this, "Uploading to Drive...", Toast.LENGTH_SHORT).show()
        // Disable button, show progress bar etc.

        lifecycleScope.launch { // Launch on Main
            val uploadedDriveId = try {
                // DriveServiceHelper.uploadFile should be a suspend function or handle its own threading
                DriveServiceHelper.uploadFile(
                    noteToUpload.filePath,
                    noteToUpload.id + ".md" // Filename on Drive
                ) // Returns driveId or null
            } catch (e: Exception) {
                Log.e(TAG, "Exception during Drive upload call", e)
                null // Indicate failure
            }


            // Back on Main implicitly after suspend function completes
            if (uploadedDriveId != null) {
                noteToUpload.driveId = uploadedDriveId // Store the ID
                // TODO: Persist this driveId association locally (DB, File, etc.)
                Log.i(TAG, "Successfully uploaded. Drive ID: $uploadedDriveId")
                Toast.makeText(this@NoteEditActivity, "Uploaded to Google Drive", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@NoteEditActivity, "Failed to upload to Google Drive", Toast.LENGTH_LONG).show()
            }
            // Hide progress indicator
        }
    }

    // Handle back press with unsaved changes check
    override fun onBackPressed() {
        val currentContent = binding.editTextNoteContent.text.toString()
        if (currentNote?.content != currentContent && originalContent != currentContent) { // Check against note object AND original state
            AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Do you want to discard them?")
                .setPositiveButton("Discard") { _, _ ->
                    setResult(Activity.RESULT_CANCELED) // Indicate no save
                    super.onBackPressed() // Call original back press
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            setResult(Activity.RESULT_CANCELED) // Indicate no save/delete action triggered by user
            super.onBackPressed() // Default behavior if no changes
        }
    }


    // Companion object for constants like EXTRA_NOTE
    companion object {
        const val EXTRA_NOTE = "com.yourpackage.notesapp.EXTRA_NOTE"
        private const val TAG = "NoteEditActivity" // Tag specific to this class
    }
}