package com.michaelzhou.composedemo.login

import android.app.Application
import com.michaelzhou.composedemo.db.UserDao
import com.michaelzhou.composedemo.db.UserEntity
import com.michaelzhou.composedemo.utils.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApp: Application = mock()
    private val mockUserDao: UserDao = mock()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(mockApp, mockUserDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onLoginClicked_correctCredentials_setsLoginSuccess() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("pass123"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("pass123")
        viewModel.onLoginClicked()
        assertEquals("a@b.com", viewModel.loginSuccess.value)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun onLoginClicked_wrongPassword_setsError() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("correct"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("wrong")
        viewModel.onLoginClicked()
        assertEquals("Invalid email or password", viewModel.loginError.value)
        assertNull(viewModel.loginSuccess.value)
    }

    @Test
    fun onLoginClicked_unregisteredEmail_setsError() = runTest {
        whenever(mockUserDao.findByEmail("nobody@test.com")).thenReturn(null)
        viewModel.onEmailChanged("nobody@test.com")
        viewModel.onPasswordChanged("any")
        viewModel.onLoginClicked()
        assertEquals("Invalid email or password", viewModel.loginError.value)
        assertNull(viewModel.loginSuccess.value)
    }

    @Test
    fun onLoginHandled_clearsLoginSuccess() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("pass"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("pass")
        viewModel.onLoginClicked()
        viewModel.onLoginHandled()
        assertNull(viewModel.loginSuccess.value)
    }
}
