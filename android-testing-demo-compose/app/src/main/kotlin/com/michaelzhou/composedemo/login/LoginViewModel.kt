package com.michaelzhou.composedemo.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaelzhou.composedemo.db.AppDatabase
import com.michaelzhou.composedemo.utils.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getInstance(application).userDao()

    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")

    val isLoginEnabled: StateFlow<Boolean> = combine(_email, _password) { email, password ->
        email.isNotBlank() && password.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _loginSuccess = MutableStateFlow<String?>(null)
    val loginSuccess: StateFlow<String?> = _loginSuccess.asStateFlow()

    fun onEmailChanged(value: String) { _email.value = value }
    fun onPasswordChanged(value: String) { _password.value = value }

    fun onLoginClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.findByEmail(_email.value)
            if (user != null && user.passwordHash == sha256(_password.value)) {
                _loginError.value = null
                _loginSuccess.value = _email.value
            } else {
                _loginError.value = "Invalid email or password"
            }
        }
    }

    fun onLoginHandled() { _loginSuccess.value = null }
}
