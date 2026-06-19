package com.childlearning.robot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * 1. 未登录/未绑定 → 直接进入认证选择页（登录或绑定设备）
 * 2. 已认证 → 进入首页
 * 3. 使用功能时如遇 401 → 跳到选择页重新认证
 */
@Composable
fun AppNavigation(
    authUseCase: AuthUseCase,
    authInterceptor: AuthInterceptor
) {
    val navController = rememberNavController()
    val authState by authUseCase.authState.collectAsState(initial = AuthState.Locked)

    // 根据认证状态决定启动页面
    val startDestination = if (authState == AuthState.Authenticated) "home" else "auth-choice"

    // 监听 401 未授权事件 → 跳到选择页
    LaunchedEffect(Unit) {
        authUseCase.unauthorizedEvent.collect {
            navController.navigate("auth-choice") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
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
        // 题库题目临时存储（跨屏幕传递）
        var pendingBankQuestion: com.childlearning.robot.core.network.ChallengeDetailResponse? by
            androidx.compose.runtime.mutableStateOf(null)

        composable("challenge-list") {
            val listViewModel: com.childlearning.robot.ui.screens.challenge.ChallengeViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            ChallengeListScreen(
                onChallengeClick = { challengeId, bankQuestion ->
                    pendingBankQuestion = bankQuestion
                    navController.navigate("challenge-detail/$challengeId")
                },
                onBack = { navController.popBackStack() },
                viewModel = listViewModel
            )
        }

        composable("challenge-detail/{challengeId}") { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId")?.toLongOrNull() ?: 0
            val listBackStackEntry = navController.getBackStackEntry("challenge-list")
            val listViewModel: com.childlearning.robot.ui.screens.challenge.ChallengeViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(listBackStackEntry)
            val bankQ = pendingBankQuestion

            // 统一使用选择模式（拖拽题自动转为选项展示）
            ChallengeDetailScreen(
                challengeId = challengeId,
                bankQuestion = bankQ,
                onBack = { navController.popBackStack() },
                onNextBankQuestion = { currentId, domainKey ->
                    listViewModel.nextBankQuestion(currentId, domainKey)
                },
                viewModel = listViewModel
            )
        }
    }
}
