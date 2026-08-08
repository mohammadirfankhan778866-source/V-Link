package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

object FirebaseDiagnostics {
    private const val TAG = "FirebaseDiagnostics"

    fun runDiagnostics(context: Context): DiagnosticResult {
        Log.i(TAG, "Starting Firebase Connection Diagnostics...")
        val issues = mutableListOf<String>()
        var isFirebaseInitialized = false
        var loadedApplicationId: String? = null
        var loadedProjectId: String? = null
        var isAuthAvailable = false

        val expectedApplicationId = "com.aistudio.pulsechat.kxmpzq"
        val expectedProjectId = "v-link-b259e"
        val currentPackageName = context.packageName
        
        Log.i(TAG, "Current Android Namespace/Package Name: $currentPackageName")

        // 1. Check FirebaseApp initialization
        try {
            val app = FirebaseApp.getInstance()
            isFirebaseInitialized = true
            val options = app.options
            loadedApplicationId = options.applicationId
            loadedProjectId = options.projectId
            
            Log.i(TAG, "FirebaseApp is initialized successfully!")
            Log.i(TAG, "FirebaseOptions - Project ID: $loadedProjectId")
            Log.i(TAG, "FirebaseOptions - Application ID (Mobile SDK App ID): $loadedApplicationId")
            Log.i(TAG, "FirebaseOptions - API Key length: ${options.apiKey?.length ?: 0}")
            
            if (loadedProjectId != expectedProjectId) {
                issues.add("Loaded Project ID '$loadedProjectId' does not match expected '$expectedProjectId'")
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "FirebaseApp is NOT initialized! Error: ${e.message}")
            issues.add("FirebaseApp is not initialized: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking FirebaseApp options: ${e.message}")
            issues.add("Error checking FirebaseOptions: ${e.message}")
        }

        // 2. Verify FirebaseAuth availability
        if (isFirebaseInitialized) {
            try {
                val auth = FirebaseAuth.getInstance()
                isAuthAvailable = true
                Log.i(TAG, "FirebaseAuth instance retrieved successfully!")
                Log.i(TAG, "Current user is signed in: ${auth.currentUser != null}")
            } catch (e: Exception) {
                Log.e(TAG, "FirebaseAuth initialization failed: ${e.message}")
                issues.add("FirebaseAuth initialization failed: ${e.message}")
            }
        } else {
            issues.add("FirebaseAuth could not be checked because FirebaseApp is not initialized.")
        }

        val success = issues.isEmpty()
        if (success) {
            Log.i(TAG, "Firebase Connection Diagnostics: PASSED")
        } else {
            Log.w(TAG, "Firebase Connection Diagnostics: FAILED with ${issues.size} issue(s)")
            for (issue in issues) {
                Log.w(TAG, "- $issue")
            }
        }

        return DiagnosticResult(
            success = success,
            issues = issues,
            loadedApplicationId = loadedApplicationId,
            loadedProjectId = loadedProjectId,
            isFirebaseInitialized = isFirebaseInitialized,
            isAuthAvailable = isAuthAvailable
        )
    }

    data class DiagnosticResult(
        val success: Boolean,
        val issues: List<String>,
        val loadedApplicationId: String?,
        val loadedProjectId: String?,
        val isFirebaseInitialized: Boolean,
        val isAuthAvailable: Boolean
    )
}
