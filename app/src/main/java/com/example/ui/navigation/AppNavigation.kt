package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.utils.PreferenceManager
import com.example.ui.screens.*

@Composable
fun AppNavigation(onThemeChange: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }

    NavHost(navController = navController, startDestination = SplashRoute) {
        composable<SplashRoute> {
            SplashScreen(
                onNavigateToLogin = {
                    if (preferenceManager.isFirstLaunch) {
                        navController.navigate(OnboardingRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    } else {
                        navController.navigate(LoginRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(ChatListRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinished = {
                    preferenceManager.isFirstLaunch = false
                    navController.navigate(LoginRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(ChatListRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(ForgotPasswordRoute)
                }
            )
        }
        
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<RegisterRoute> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(ChatListRoute) {
                        popUpTo(RegisterRoute) { inclusive = true }
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<ChatListRoute> {
            ChatListScreen(
                onNavigateToChat = { id, name ->
                    navController.navigate(ChatRoute(id, name))
                },
                onNavigateToProfile = {
                    navController.navigate(SettingsRoute)
                },
                onNavigateToSearch = {
                    navController.navigate(SearchUsersRoute)
                }
            )
        }
        
        composable<SearchUsersRoute> {
            SearchUsersScreen(
                onNavigateToChat = { id, name ->
                    navController.navigate(ChatRoute(id, name))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<ChatRoute> { backStackEntry ->
            val chatRoute = backStackEntry.toRoute<ChatRoute>()
            ChatScreen(
                chatId = chatRoute.chatId,
                chatName = chatRoute.chatName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = { navController.navigate(ProfileRoute) },
                onThemeChange = onThemeChange,
                onLogoutSuccess = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable<ProfileRoute> {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

