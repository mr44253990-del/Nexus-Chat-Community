package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = SplashRoute) {
        composable<SplashRoute> {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(ChatListRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
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

