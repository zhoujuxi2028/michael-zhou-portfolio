package com.michaelzhou.viewdemo.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    private var email = ""
    private var password = ""

    private val _isLoginEnabled = MutableLiveData(false)
    val isLoginEnabled: LiveData<Boolean> = _isLoginEnabled

    fun onEmailChanged(value: String) {
        email = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }

    fun onPasswordChanged(value: String) {
        password = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }
}
