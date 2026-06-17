package com.childlearning.robot.ui.screens.bind

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.ui.components.QrCodeGenerator

@Composable
fun DeviceBindScreen(
    onBindSuccess: () -> Unit,
    viewModel: DeviceBindViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val deviceId = viewModel.deviceId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔗 绑定设备",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when (uiState.value) {
            is BindUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is BindUiState.WaitingForBind -> {
                Text(
                    text = "请使用家长端APP扫描下方二维码绑定设备",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // 生成二维码，内容为设备绑定链接或deviceId
                QrCodeGenerator(
                    content = "childlearning://bind?deviceId=$deviceId",
                    modifier = Modifier.size(240.dp).background(Color.White).padding(8.dp)
                )

                Text(
                    text = "设备ID: $deviceId",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )

                Text(
                    text = "等待绑定中...",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            is BindUiState.Bound -> {
                LaunchedEffect(Unit) {
                    Toast.makeText(
                        androidx.compose.ui.platform.LocalContext.current,
                        "绑定成功！",
                        Toast.LENGTH_SHORT
                    ).show()
                    onBindSuccess()
                }
            }
            is BindUiState.Error -> {
                Text(
                    text = (uiState.value as BindUiState.Error).message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

sealed class BindUiState {
    object Loading : BindUiState()
    data class WaitingForBind(val deviceId: String) : BindUiState()
    object Bound : BindUiState()
    data class Error(val message: String) : BindUiState()
}
