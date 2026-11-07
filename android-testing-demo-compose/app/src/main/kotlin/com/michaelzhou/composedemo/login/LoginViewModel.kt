package com.michaelzhou.composedemo.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LoginViewModel : ViewModel() {
    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")

    val isLoginEnabled: StateFlow<Boolean> = combine(_email, _password) { email, password ->
        email.isNotBlank() && password.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onEmailChanged(value: String) { _email.value = value }
    fun onPasswordChanged(value: String) { _password.value = value }
}
