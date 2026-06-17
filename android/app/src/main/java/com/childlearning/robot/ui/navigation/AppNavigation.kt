package com.childlearning.robot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.childlearning.robot.core.network.AuthInterceptor
import com.childlearning.robot.domain.enums.AuthState
import com.childlearning.robot.domain.usecase.AuthUseCase
import com.childlearning.robot.ui.screens.chat.ChatScreen
import com.childlearning.robot.ui.screens.focus.FocusScreen
import com.childlearning.robot.ui.screens.game.GameScreen
import com.childlearning.robot.ui.screens.home.HomeScreen
import com.childlearning.robot.ui.screens.homework.HomeworkScreen
import com.childlearning.robot.ui.screens.login.LoginScreen
import com.childlearning.robot.ui.screens.challenge.ChallengeListScreen
import com.childlearning.robot.ui.screens.challenge.DynamicDragScreen
import com.childlearning.robot.ui.screens.bind.DeviceBindScreen
import com.childlearning.robot.ui.screens.voice.VoiceScreen
import javax.inject.Inject

/**
 * 应用导航
 *
 * AuthState 驱动：
 * - AuthState.Locked / Expired → 跳转登录页
 * - AuthState.Authenticated → 跳转主页
 *
 * 401 自动拦截：
 * - AuthInterceptor 检测到 401 → 清除 token → 触发 unauthorizedEvent
 * - 导航监听事件 → 跳转登录页
 */
@Composable
fun AppNavigation(
    authUseCase: AuthUseCase,
    authInterceptor: AuthInterceptor
) {
    val navController = rememberNavController()
    val authState by authUseCase.authState.collectAsState(initial = AuthState.Locked)

    // 监听 401 未授权事件 → 强制跳转登录页
    LaunchedEffect(Unit) {
        authUseCase.unauthorizedEvent.collect {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // AuthState 驱动导航
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Locked, is AuthState.Expired -> {
                // 未认证时先检查设备绑定状态
                // TODO: 检查是否已绑定，未绑定跳绑定页，已绑定跳登录页
                navController.navigate("bind-device") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                // 已认证时如果在登录页/绑定页则跳转主页
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == "login" || currentRoute == "bind-device") {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "bind-device") {
        composable("bind-device") {
            DeviceBindScreen(
                onBindSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToVoice = { navController.navigate("voice") },
                onNavigateToFocus = { navController.navigate("focus") },
                onNavigateToGame = { navController.navigate("game") },
                onNavigateToHomework = { navController.navigate("homework") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("chat") {
            ChatScreen(onBack = { navController.popBackStack() })
        }

        composable("voice") {
            VoiceScreen(onBack = { navController.popBackStack() })
        }

        composable("focus") {
            FocusScreen(onBack = { navController.popBackStack() })
        }

        composable("game") {
            GameScreen(onBack = { navController.popBackStack() })
        }

        composable("homework") {
            HomeworkScreen(onBack = { navController.popBackStack() })
        }

        composable("challenge-list") {
            ChallengeListScreen(
                onChallengeClick = { challengeId ->
                    navController.navigate("challenge-detail/$challengeId")
                }
            )
        }

        composable("challenge-detail/{challengeId}") { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId")?.toLongOrNull() ?: 0
            DynamicDragScreen(
                challengeId = challengeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
