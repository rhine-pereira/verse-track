package com.rhinepereira.versetrack.data

import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository {
    private val auth = SupabaseConfig.client.gotrue

    val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    fun sessionStatusFlow() = auth.sessionStatus

    suspend fun signOut() {
        auth.logout()
    }
}
