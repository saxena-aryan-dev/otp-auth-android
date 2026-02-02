package com.app.otpauth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.otpauth.ui.login.LoginScreen
import com.app.otpauth.ui.otp.OtpScreen
import com.app.otpauth.ui.session.SessionScreen
import com.app.otpauth.viewmodel.AuthUiState
import com.app.otpauth.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun AppNavigation(viewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate only when the state *type* changes, not on every timer tick
    LaunchedEffect(Unit) {
        viewModel.uiState
            .map { it::class }
            .distinctUntilChanged()
            .collect { stateClass ->
                when (stateClass) {
                    AuthUiState.EmailEntry::class -> {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    AuthUiState.OtpEntry::class -> {
                        navController.navigate("otp") {
                            popUpTo("login") { inclusive = false }
                        }
                    }
                    AuthUiState.Session::class -> {
                        navController.navigate("session") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            val state = uiState
            if (state is AuthUiState.EmailEntry) {
                LoginScreen(
                    state = state,
                    onEmailChanged = viewModel::onEmailChanged,
                    onSendOtp = viewModel::sendOtp
                )
            }
        }

        composable("otp") {
            val state = uiState
            if (state is AuthUiState.OtpEntry) {
                OtpScreen(
                    state = state,
                    onVerifyOtp = viewModel::verifyOtp,
                    onResendOtp = viewModel::resendOtp
                )
            }
        }

        composable("session") {
            val state = uiState
            if (state is AuthUiState.Session) {
                SessionScreen(
                    state = state,
                    onLogout = viewModel::logout
                )
            }
        }
    }
}
