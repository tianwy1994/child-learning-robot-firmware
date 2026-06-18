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
import com.childlearning.robot.ui.screens.auth.AuthChoiceScreen
import com.childlearning.robot.ui.screens.chat.ChatScreen
import com.childlearning.robot.ui.screens.focus.FocusScreen
import com.childlearning.robot.ui.screens.game.GameScreen
import com.childlearning.robot.ui.screens.home.HomeScreen
import com.childlearning.robot.ui.screens.homework.HomeworkScreen
import com.childlearning.robot.ui.screens.login.LoginScreen
import com.childlearning.robot.ui.screens.challenge.ChallengeListScreen
import com.childlearning.robot.ui.screens.challenge.ChallengeDetailScreen
import com.childlearning.robot.ui.screens.bind.DeviceBindScreen
import com.childlearning.robot.ui.screens.voice.VoiceScreen

/**
 * 应用导航
 *
 * 启动流程：
 * 1. 默认进入首页（无需登录即可浏览）
 * 2. 使用功能时如未认证，401 拦截 → 跳到选择页（登录/绑定二选一）
 * 3. 认证成功后返回首页
 */
@Composable
fun AppNavigation(
    authUseCase: AuthUseCase,
    authInterceptor: AuthInterceptor
) {
    val navController = rememberNavController()
    val authState by authUseCase.authState.collectAsState(initial = AuthState.Locked)

    // 监听 401 未授权事件 → 跳到选择页
    LaunchedEffect(Unit) {
        authUseCase.unauthorizedEvent.collect {
            navController.navigate("auth-choice") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        // ===== 首页 =====
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToVoice = { navController.navigate("voice") },
                onNavigateToFocus = { navController.navigate("focus") },
                onNavigateToGame = { navController.navigate("game") },
                onNavigateToHomework = { navController.navigate("homework") },
                onNavigateToChallenge = { navController.navigate("challenge-list") },
                onNavigateToBind = { navController.navigate("auth-choice") },
                onLogout = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ===== 认证选择（登录/绑定二选一）=====
        composable("auth-choice") {
            AuthChoiceScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToBind = { navController.navigate("bind-device") },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 账号密码登录 =====
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 设备绑定 =====
        composable("bind-device") {
            DeviceBindScreen(
                onBindSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 功能页面 =====
        composable("chat") { ChatScreen(onBack = { navController.popBackStack() }) }
        composable("voice") { VoiceScreen(onBack = { navController.popBackStack() }) }
        composable("focus") { FocusScreen(onBack = { navController.popBackStack() }) }
        composable("game") { GameScreen(onBack = { navController.popBackStack() }) }
        composable("homework") { HomeworkScreen(onBack = { navController.popBackStack() }) }

        // ===== 挑战 =====
        composable("challenge-list") {
            ChallengeListScreen(
                onChallengeClick = { challengeId ->
                    navController.navigate("challenge-detail/$challengeId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("challenge-detail/{challengeId}") { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId")?.toLongOrNull() ?: 0
            ChallengeDetailScreen(
                challengeId = challengeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
