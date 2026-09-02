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

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        return try {
            fbAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            Log.w("AuthRepository", "No Firebase user found for email: $email")
            Result.failure(Exception("No account found with email '$email' in authentication records."))
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Log.w("AuthRepository", "Invalid email format: $email")
            Result.failure(Exception("The email address is improperly formatted."))
        } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
            Log.w("AuthRepository", "Too many reset requests for email: $email")
            Result.failure(Exception("Too many reset attempts. Please wait a few moments and try again."))
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            Log.w("AuthRepository", "Network error sending reset email: ${e.message}")
            Result.failure(Exception("Network error. Please check your internet connection."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error sending password reset email: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<AuthResult> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase Authentication is not available"))
        return try {
            val hashedNonce = UUID.randomUUID().toString().let {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(it.toByteArray())
                val digest = md.digest()
                digest.joinToString("") { byte -> "%02x".format(byte) }
            }

            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val clientId = if (resId != 0) {
                try { context.getString(resId) } catch (e: Exception) { "303440318642-bdprd7ggohirjtsf3rf4am36mfo3v3pm.apps.googleusercontent.com" }
            } else {
                "303440318642-bdprd7ggohirjtsf3rf4am36mfo3v3pm.apps.googleusercontent.com"
            }

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
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = fbAuth.signInWithCredential(authCredential).await()
                Result.success(authResult)
            } else {
                Result.failure(Exception("Unsupported credential type received from Google."))
            }
        } catch (e: GetCredentialException) {
            Log.w("AuthRepository", "Google Sign In Exception: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.w("AuthRepository", "Google Sign In Exception: ${e.message}")
            Result.failure(e)
        }
    }

    fun logout() {
        auth?.signOut()
    }

    fun getCurrentUser() = auth?.currentUser
}
