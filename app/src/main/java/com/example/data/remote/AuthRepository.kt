package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun logDebug(tag: String, msg: String) {
    if (BuildConfig.DEBUG) Log.d(tag, msg)
}

private fun logError(tag: String, msg: String, e: Throwable? = null) {
    if (BuildConfig.DEBUG) {
        if (e != null) Log.e(tag, msg, e) else Log.e(tag, msg)
    }
}

class AuthRepository {

    private val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

    suspend fun signInWithGoogle(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var activityContext = context
            while (activityContext is android.content.ContextWrapper) {
                if (activityContext is android.app.Activity) break
                activityContext = activityContext.baseContext
            }

            val credentialManager = CredentialManager.create(activityContext)

            val rawNonce = java.util.UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.joinToString("") { "%02x".format(it) }

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
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

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken

                SupabaseConfig.client.auth.signInWith(IDToken) {
                    idToken = googleIdToken
                    provider = Google
                    nonce = rawNonce
                }

                logDebug("AuthRepository", "Login con Supabase exitoso")
                Result.success(Unit)
            } else {
                logError("AuthRepository", "Credencial no reconocida o no es de Google")
                Result.failure(Exception("Credencial no reconocida"))
            }

        } catch (e: GetCredentialException) {
            logError("AuthRepository", "Error en Credential Manager", e)
            Result.failure(e)
        } catch (e: Exception) {
            logError("AuthRepository", "Error de autenticación", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            SupabaseConfig.client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            logError("AuthRepository", "Error al cerrar sesión", e)
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        val session = SupabaseConfig.client.auth.currentSessionOrNull()
        return session != null && session.user?.identities?.any { it.provider == "google" } == true
    }
}
