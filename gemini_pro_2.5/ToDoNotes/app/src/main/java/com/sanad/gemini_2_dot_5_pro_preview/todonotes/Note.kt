package com.sanad.gemini_2_dot_5_pro_preview.todonotes


import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Make sure kotlin-parcelize plugin is enabled if needed

@Parcelize // To easily pass between activities
data class Note(
    val id: String, // Unique identifier (e.g., timestamp or UUID)
    var title: String,
    var content: String,
    var filePath: String, // Full path to the saved file
    var lastModified: Long,
    var driveId: String? = null // Optional: Google Drive File ID for updates
) : Parcelable