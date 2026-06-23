package com.childlearning.robot.ui.screens.gratitude

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GratitudeScreen(
    onBack: () -> Unit,
    viewModel: GratitudeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
        if (!granted) Toast.makeText(context, "需要录音权限才能用语音写日记", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            Toast.makeText(context, "感恩日记已保存 🌱", Toast.LENGTH_SHORT).show()
            viewModel.clearSubmitSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("感恩日记", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFF8E1)
                )
            )
        },
        containerColor = Color(0xFFFFF8E1)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {


            // 写日记区域
            WriteSection(
                content = uiState.inputContent,
                target = uiState.inputTarget,
                isRecording = uiState.isRecording,
                isSubmitting = uiState.isSubmitting,
                onContentChange = viewModel::onContentChange,
                onTargetChange = viewModel::onTargetChange,
                onMicPress = {
                    if (hasRecordPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onMicRelease = { viewModel.stopRecordingAndRecognize() },
                onSubmit = { viewModel.submitGratitude() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 历史记录
            if (uiState.entries.isNotEmpty()) {
                Text(
                    text = "往日感恩",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.entries) { entry ->
                        GratitudeEntryCard(entry = entry)
                    }
                }
            } else if (!uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有感恩日记\n说出今天感谢的事吧 🌻",
                        textAlign = TextAlign.Center,
                        color = Color(0xFF8D6E63),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WriteSection(
    content: String,
    target: String,
    isRecording: Boolean,
    isSubmitting: Boolean,
    onContentChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val micScale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "micScale"
    )
    val micColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFE53935) else Color(0xFF6C63FF),
        label = "micColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "今天，我感谢…",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = { Text("说出或写下你感谢的事情", color = Color(0xFFBCAAA4)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = target,
                onValueChange = onTargetChange,
                placeholder = { Text("感谢的对象（可选，如：妈妈、老师）", color = Color(0xFFBCAAA4)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 语音输入按钮
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .scale(micScale)
                            .clip(CircleShape)
                            .background(micColor)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onMicPress()
                                        tryAwaitRelease()
                                        onMicRelease()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "语音输入",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRecording) "松开识别" else "按住说话",
                        fontSize = 11.sp,
                        color = micColor
                    )
                }

                // 提交按钮
                Button(
                    onClick = onSubmit,
                    enabled = content.isNotBlank() && !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    modifier = Modifier.height(52.dp).widthIn(min = 120.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存日记", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GratitudeEntryCard(entry: com.childlearning.robot.core.network.GratitudeEntry) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.gratitudeTarget?.let { "感谢 $it" } ?: "感恩记录",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF9800)
                )
                Text(
                    text = entry.createdAt?.take(10) ?: "",
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.content,
                fontSize = 14.sp,
                color = Color(0xFF5D4037),
                lineHeight = 22.sp
            )
            if (entry.shared) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "💝 已分享给家长", fontSize = 11.sp, color = Color(0xFFE91E63))
            }
        }
    }
}
