package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "FirebaseAuth initialization failed: ${e.message}")
            null
        }
    }
    private val credentialManager = CredentialManager.create(context)

    suspend fun registerWithEmailAndPassword(email: String, password: String): Result<AuthResult> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        return try {
            val result = fbAuth.createUserWithEmailAndPassword(email, password).await()
            try {
                result.user?.sendEmailVerification()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Email verification send warning: ${e.message}")
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error creating user with createUserWithEmailAndPassword: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthResult> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        return try {
            val result = fbAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): AuthResult? {
        return try {
            val hashedNonce = UUID.randomUUID().toString().let {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(it.toByteArray())
                val digest = md.digest()
                digest.joinToString("") { byte -> "%02x".format(byte) }
            }

            // Client ID usually from strings.xml or hardcoded for testing
            // Using a dummy one for now if not available, but real one is needed for CredentialManager
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val clientId = if (resId != 0) context.getString(resId) else "dummy_client_id_for_testing"

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setNonce(hashedNonce)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            val fbAuth = auth ?: return null
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                fbAuth.signInWithCredential(authCredential).await()
            } else {
                null
            }
        } catch (e: GetCredentialException) {
            Log.w("AuthRepository", "Google Sign In Failed (Expected in Virtual Preview): ${e.message}")
            null
        } catch (e: Exception) {
            Log.w("AuthRepository", "Google Sign In Failed (Expected in Virtual Preview): ${e.message}")
            null
        }
    }

    fun logout() {
        auth?.signOut()
    }

    fun getCurrentUser() = auth?.currentUser
}
