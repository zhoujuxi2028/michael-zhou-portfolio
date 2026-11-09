package com.michaelzhou.viewdemo.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.michaelzhou.viewdemo.db.AppDatabase
import com.michaelzhou.viewdemo.db.UserDao
import com.michaelzhou.viewdemo.utils.sha256
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application,
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AppDatabase.getInstance(application).userDao()
    )

    private var email = ""
    private var password = ""

    private val _isLoginEnabled = MutableLiveData(false)
    val isLoginEnabled: LiveData<Boolean> = _isLoginEnabled

    private val _loginError = MutableLiveData<String?>(null)
    val loginError: LiveData<String?> = _loginError

    private val _loginSuccess = MutableLiveData<String?>()
    val loginSuccess: LiveData<String?> = _loginSuccess

    fun onEmailChanged(value: String) {
        email = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }

    fun onPasswordChanged(value: String) {
        password = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }

    fun onLoginClicked() {
        viewModelScope.launch(ioDispatcher) {
            val user = userDao.findByEmail(email)
            if (user != null && user.passwordHash == sha256(password)) {
                _loginError.postValue(null)
                _loginSuccess.postValue(email)
            } else {
                _loginError.postValue("Invalid email or password")
            }
        }
    }
}
