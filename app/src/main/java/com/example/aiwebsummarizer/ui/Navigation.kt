package com.example.aiwebsummarizer.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aiwebsummarizer.ui.auth.LoginScreen
import com.example.aiwebsummarizer.ui.auth.RegisterScreen
import com.example.aiwebsummarizer.ui.summary.SummaryScreen
import com.example.aiwebsummarizer.ui.summary.SummaryViewModel

// Define navigation routes
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Summary : Screen("summary")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(Screen.Summary.route) {
            SummaryScreen(navController = navController)
        }
    }
}