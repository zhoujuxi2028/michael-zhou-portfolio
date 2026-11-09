package com.michaelzhou.viewdemo.login

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.michaelzhou.viewdemo.db.UserDao
import com.michaelzhou.viewdemo.db.UserEntity
import com.michaelzhou.viewdemo.utils.sha256
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
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
    fun onLoginClicked_correctCredentials_postsSuccess() = runTest {
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
    fun onLoginClicked_wrongPassword_postsError() = runTest {
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
    fun onLoginClicked_unregisteredEmail_postsError() = runTest {
        whenever(mockUserDao.findByEmail("nobody@test.com")).thenReturn(null)
        viewModel.onEmailChanged("nobody@test.com")
        viewModel.onPasswordChanged("any")
        viewModel.onLoginClicked()
        assertEquals("Invalid email or password", viewModel.loginError.value)
        assertNull(viewModel.loginSuccess.value)
    }

    @Test
    fun onEmailChanged_bothFieldsFilled_enablesLogin() {
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("pass")
        assertEquals(true, viewModel.isLoginEnabled.value)
    }

    @Test
    fun onPasswordChanged_emptyEmail_disablesLogin() {
        viewModel.onPasswordChanged("pass")
        assertEquals(false, viewModel.isLoginEnabled.value)
    }
}
