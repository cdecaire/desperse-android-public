package app.desperse.core.auth

import app.desperse.data.model.User

sealed class AuthState {
    object NotReady : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
