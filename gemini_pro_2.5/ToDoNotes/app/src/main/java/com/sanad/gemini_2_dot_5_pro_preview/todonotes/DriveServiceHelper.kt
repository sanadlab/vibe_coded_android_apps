package com.sanad.gemini_2_dot_5_pro_preview.todonotes

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import com.google.api.client.http.InputStreamContent


object DriveServiceHelper {
    private const val TAG = "DriveServiceHelper"
    private var driveService: Drive? = null

    // Initialize the Drive service using the signed-in account
    fun initialize(context: Context, account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE) // Scope for app-created files
        ).setBackOff(ExponentialBackOff()) // Recommended for handling transient errors
        credential.selectedAccount = account.account // Use the specific signed-in account

        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name)) // Your app name
            .build()
        Log.i(TAG, "Drive Service Initialized")
    }

    fun isInitialized(): Boolean = driveService != null

    fun reset() {
        driveService = null // Reset when signed out
        Log.i(TAG,"Drive Service Reset")
    }

    // --- Upload Function (Basic Example) ---
    // Needs to run on a background thread (using Coroutines here)
    suspend fun uploadFile(
        localFilePath: String,
        fileName: String, // e.g., noteId + ".md"
        mimeType: String = "text/markdown"
    ): String? = withContext(Dispatchers.IO) { // Switch to IO dispatcher for network/file ops
        val service = driveService ?: run {
            Log.e(TAG, "Drive service not initialized before upload attempt.")
            return@withContext null
        }

        val fileMetadata = File().apply {
            name = fileName
            // Optionally set parents to upload to a specific folder
            // parents = listOf("FOLDER_ID")
        }

        val localFile = java.io.File(localFilePath)
        if (!localFile.exists()) {
            Log.e(TAG, "Local file does not exist: $localFilePath")
            return@withContext null
        }

        val mediaContent = InputStreamContent(mimeType, FileInputStream(localFile))

        try {
            val uploadedFile: File = service.files().create(fileMetadata, mediaContent)
                .setFields("id") // Request only the file ID back
                .execute()
            Log.i(TAG, "File uploaded successfully. Drive ID: ${uploadedFile.id}")
            return@withContext uploadedFile.id // Return the Drive File ID
        } catch (e: IOException) {
            Log.e(TAG, "Error uploading file to Drive", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Drive upload", e)
            return@withContext null
        }
    }

    // --- TODO: Implement other Drive functions as needed ---
    // suspend fun updateFile(driveFileId: String, localFilePath: String, ...)
    // suspend fun downloadFile(driveFileId: String, ...)
    // suspend fun listFiles(...)
    // suspend fun findOrCreateAppFolder(...)

}