package com.cristobalcariqueo.defensadedeudores.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Signed-in user, or null. */
data class AuthUser(val id: String, val email: String?)

interface AuthRepository {
    /** Emits on every session change; null while signed out. */
    fun observeUser(): Flow<AuthUser?>

    /** Current user or null -- non-suspending snapshot for the sync engine. */
    fun currentUser(): AuthUser?

    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signOut()
}

/**
 * Email/password only for now -- Google lands once the OAuth client (SHA-1)
 * is registered in Google Cloud Console + the Supabase provider (README).
 * Session is persisted by auth-kt, so being offline never forces a re-auth.
 */
class AuthRepositoryImpl(private val client: SupabaseClient) : AuthRepository {

    override fun observeUser(): Flow<AuthUser?> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated ->
                status.session.user?.let { AuthUser(id = it.id, email = it.email) }
            else -> null
        }
    }

    override fun currentUser(): AuthUser? =
        client.auth.currentUserOrNull()?.let { AuthUser(id = it.id, email = it.email) }

    override suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }
}
