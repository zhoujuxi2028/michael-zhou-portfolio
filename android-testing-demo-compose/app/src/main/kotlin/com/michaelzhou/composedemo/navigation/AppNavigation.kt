package com.michaelzhou.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michaelzhou.composedemo.login.LoginScreen
import com.michaelzhou.composedemo.todo.TodoScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { navController.navigate("todo") })
        }
        composable("todo") {
            TodoScreen()
        }
    }
}
