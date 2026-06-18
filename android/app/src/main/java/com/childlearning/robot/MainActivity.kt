package com.childlearning.robot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.childlearning.robot.core.network.AuthInterceptor
import com.childlearning.robot.domain.usecase.AuthUseCase
import com.childlearning.robot.ui.navigation.AppNavigation
import com.childlearning.robot.ui.theme.RobotTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authUseCase: AuthUseCase
    @Inject lateinit var authInterceptor: AuthInterceptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 注册 401 拦截回调 → 触发未授权事件
        authInterceptor.onUnauthorized = {
            CoroutineScope(Dispatchers.Main).launch {
                authUseCase.handleUnauthorized()
            }
        }

        setContent {
            RobotTheme {
                AppNavigation(
                    authUseCase = authUseCase,
                    authInterceptor = authInterceptor
                )
            }
        }
    }
}
