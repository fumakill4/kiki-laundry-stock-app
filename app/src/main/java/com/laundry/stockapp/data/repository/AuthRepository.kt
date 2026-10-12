package com.laundry.stockapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.laundry.stockapp.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCol = db.collection("users")

    private val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
    private val LOGGED_IN_USER_ROLE = stringPreferencesKey("logged_in_user_role")
    private val LOGGED_IN_USER_NAME = stringPreferencesKey("logged_in_user_name")
    private val LOGGED_IN_USER_EMAIL = stringPreferencesKey("logged_in_user_email")

    val loggedInUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        auth.currentUser?.uid?.takeIf { it == preferences[LOGGED_IN_USER_ID] }
    }
    val loggedInUserRole: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_ROLE] }
    val loggedInUserName: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_NAME] }
    val loggedInUserEmail: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_EMAIL] }

    suspend fun login(email: String, password: String): User {
        logout()
        try {
            val principal = auth.signInWithEmailAndPassword(email.trim(), password).await().user
                ?: throw Exception("Email atau sandi salah")
            val claims = principal.getIdToken(true).await().claims
            if (claims["jclAdmin"] != true) throw Exception("Akun belum disetujui administrator")
            val profile = usersCol.document(principal.uid).get().await().toObject(User::class.java)
            val role = if (claims["jclMaster"] == true) "Master App" else "Admin"
            val user = User(id = principal.uid, name = profile?.name ?: principal.displayName.orEmpty(),
                email = principal.email.orEmpty(), phone = profile?.phone.orEmpty(), role = role)
            saveSession(user.id, role, user.name.orEmpty(), user.email.orEmpty())
            return user
        } catch (error: Exception) {
            logout()
            throw error
        }
    }

    suspend fun register(user: User): User {
        // Account creation never grants inventory or administrator access.
        val principal = auth.createUserWithEmailAndPassword(user.email.orEmpty().trim(), user.passwordHash.orEmpty()).await().user
            ?: throw Exception("Pendaftaran gagal")
        try {
            principal.sendEmailVerification().await()
            usersCol.document(principal.uid).set(mapOf(
                "name" to user.name.orEmpty(), "email" to principal.email.orEmpty(),
                "phone" to user.phone.orEmpty(), "role" to "Pending"
            )).await()
        } finally {
            logout()
        }
        throw Exception("Akun dibuat. Verifikasi email dan minta persetujuan administrator sebelum masuk.")
    }

    suspend fun logout() {
        auth.signOut()
        context.dataStore.edit { preferences ->
            preferences.remove(LOGGED_IN_USER_ID)
            preferences.remove(LOGGED_IN_USER_ROLE)
            preferences.remove(LOGGED_IN_USER_NAME)
            preferences.remove(LOGGED_IN_USER_EMAIL)
        }
    }

    suspend fun updateMasterPasswordInCloud(password: String) {
        // This PIN is a local convenience lock, not a cloud authentication credential.
        settingsRepository.setMasterPasswordHash(hashString(password))
    }

    private suspend fun saveSession(userId: String, role: String, name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[LOGGED_IN_USER_ID] = userId
            preferences[LOGGED_IN_USER_ROLE] = role
            preferences[LOGGED_IN_USER_NAME] = name
            preferences[LOGGED_IN_USER_EMAIL] = email
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
