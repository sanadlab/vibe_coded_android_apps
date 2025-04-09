package com.sanad.gemini_2_dot_5_pro_preview.todonotes


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.sanad.gemini_2_dot_5_pro_preview.todonotes.databinding.ActivityMainBinding // View Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var googleSignInClient: GoogleSignInClient
    private var currentAccount: GoogleSignInAccount? = null

    private val TAG = "MainActivity"

    // Activity Result Launcher for Sign-In Intent
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.i(TAG, "Google Sign-In successful for: ${account?.email}")
                handleSignInSuccess(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed", e)
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
                handleSignOut() // Ensure clean state on failure
            }
        } else {
            Log.w(TAG, "Sign-in flow cancelled or failed. Result code: ${result.resultCode}")
            // Optional: Show a message if needed
        }
    }

    // Activity Result Launcher for Note Edit Activity
    private val editNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Note was potentially saved or deleted, reload the list
            Log.d(TAG, "Note edit completed, reloading notes.")
            loadNotes()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupGoogleSignIn()

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(this, NoteEditActivity::class.java)
            // Don't pass any NOTE_EXTRA for new note
            editNoteLauncher.launch(intent)
        }

        // Initial check for signed-in user
        currentAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (currentAccount != null && hasDriveScope(currentAccount!!)) {
            Log.d(TAG,"User already signed in with Drive scope.")
            DriveServiceHelper.initialize(this, currentAccount!!)
        } else {
            Log.d(TAG,"User not signed in or missing Drive scope.")
            // Optionally trigger sign-in automatically, or wait for user action
            DriveServiceHelper.reset()
        }
        invalidateOptionsMenu() // Update Sign-In/Out button text

        loadNotes() // Load notes on startup
    }

    override fun onResume() {
        super.onResume()
        // Reload notes in case they were changed externally or by NoteEditActivity
        // Only reload if the result launcher didn't already trigger it
        // This simple reload might be inefficient for many notes - consider observing changes
        // loadNotes() // Re-enabled simple reload
    }


    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter { note ->
            // Handle note click - launch NoteEditActivity for editing
            val intent = Intent(this, NoteEditActivity::class.java)
            intent.putExtra(NoteEditActivity.EXTRA_NOTE, note) // Pass the note object
            editNoteLauncher.launch(intent)
        }
        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
            // Optional: Add item decoration (dividers)
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // Request Drive scope needed for file operations
            .requestScopes(Scope(DriveScopes.DRIVE_FILE)) // Scope for files created/opened by the app
            // If you need broader access (use cautiously):
            // .requestScopes(Scope(DriveScopes.DRIVE_APPDATA)) // AppData folder
            // .requestScopes(Scope(DriveScopes.DRIVE)) // Full Drive access
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun loadNotes() {
        // Use coroutine to load files from disk off the main thread
        lifecycleScope.launch(Dispatchers.Main) { // Launch on Main, switch context inside
            binding.textViewEmpty.visibility = View.GONE // Hide empty text initially
            // Consider showing a progress indicator here if loading is slow
            val notes = withContext(Dispatchers.IO) { // Switch to IO thread for file access
                FileUtils.loadNotesFromDirectory(this@MainActivity)
            }
            noteAdapter.submitList(notes) // Update adapter on Main thread
            binding.textViewEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            Log.d(TAG, "Loaded ${notes.size} notes.")
        }
    }

    private fun handleSignInSuccess(account: GoogleSignInAccount?) {
        if (account == null) {
            Log.w(TAG,"handleSignInSuccess called with null account")
            handleSignOut()
            return
        }
        if (!hasDriveScope(account)) {
            Log.w(TAG,"Sign-in successful but DRIVE_FILE scope missing!")
            Toast.makeText(this, "Drive permission needed. Please sign in again.", Toast.LENGTH_LONG).show()
            // Force sign out to allow re-requesting scope
            googleSignInClient.signOut().addOnCompleteListener { handleSignOut() }
            return
        }
        currentAccount = account
        DriveServiceHelper.initialize(this, account)
        invalidateOptionsMenu() // Update UI (e.g., change button to "Sign Out")
        Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
    }

    private fun handleSignOut() {
        currentAccount = null
        DriveServiceHelper.reset()
        invalidateOptionsMenu() // Update UI
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
    }

    // Check if the necessary Drive scope was granted
    private fun hasDriveScope(account: GoogleSignInAccount): Boolean {
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
    }


    // --- Menu Handling ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val signInMenuItem = menu.findItem(R.id.action_sign_in_out)
        if (currentAccount != null) {
            signInMenuItem.title = "Sign Out"
        } else {
            signInMenuItem.title = "Sign In"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_in_out -> {
                if (currentAccount != null) {
                    // Sign Out
                    googleSignInClient.signOut().addOnCompleteListener {
                        handleSignOut()
                    }
                } else {
                    // Sign In
                    val signInIntent = googleSignInClient.signInIntent
                    signInLauncher.launch(signInIntent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}