package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object LoginRoute

@Serializable
object ForgotPasswordRoute

@Serializable
object RegisterRoute

@Serializable
object ChatListRoute

@Serializable
object SearchUsersRoute

@Serializable
data class ChatRoute(val chatId: String, val chatName: String)

@Serializable
object ProfileRoute

@Serializable
object SettingsRoute

@Serializable
object OnboardingRoute

