package com.childlearning.robot.ui.screens.bind

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.ui.components.QrCodeImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceBindScreen(
    onBindSuccess: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: DeviceBindViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deviceId = viewModel.deviceId
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("绑定设备") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF6C63FF), Color(0xFF9C27B0), Color(0xFFF8F9FE))
                    )
                )
        ) {
            when (uiState) {
                is BindUiState.Loading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is BindUiState.WaitingForBind -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📱 扫码绑定设备",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "请使用家长端APP扫描下方二维码",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // 二维码卡片
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                        ) {
                            QrCodeImage(
                                content = "childlearning://bind?deviceId=$deviceId",
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = "设备ID: $deviceId",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "⏳ 等待绑定中...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
                is BindUiState.Bound -> {
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "🎉 绑定成功！", Toast.LENGTH_SHORT).show()
                        onBindSuccess()
                    }
                }
                is BindUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ ${(uiState as BindUiState.Error).message}",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
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
