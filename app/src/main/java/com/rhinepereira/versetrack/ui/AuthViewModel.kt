package com.rhinepereira.versetrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhinepereira.versetrack.data.AuthRepository
import com.rhinepereira.versetrack.data.SupabaseConfig
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val userId: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionStatusFlow().collect { status ->
                _authState.value = when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = authRepository.currentUserId
                        if (userId != null) AuthState.SignedIn(userId)
                        else AuthState.SignedOut
                    }
                    is SessionStatus.NotAuthenticated -> AuthState.SignedOut
                    is SessionStatus.LoadingFromStorage -> AuthState.Loading
                    is SessionStatus.NetworkError -> AuthState.SignedOut
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
