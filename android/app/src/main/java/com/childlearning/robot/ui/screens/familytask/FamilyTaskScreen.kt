package com.childlearning.robot.ui.screens.familytask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.core.network.FamilyTaskResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTaskScreen(
    onBack: () -> Unit,
    viewModel: FamilyTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("家庭任务", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE8EAF6))
            )
        },
        containerColor = Color(0xFFE8EAF6)
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3F51B5))
            }
        } else if (uiState.tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂时没有家庭任务\n等家长来布置吧 🏠",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF7986CB),
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val pending = uiState.tasks.filter { it.status == "PENDING" }
                val done = uiState.tasks.filter { it.status != "PENDING" }

                if (pending.isNotEmpty()) {
                    item {
                        Text(
                            text = "待完成 (${pending.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A237E)
                        )
                    }
                    items(pending) { task ->
                        FamilyTaskCard(
                            task = task,
                            onComplete = { viewModel.showCompleteDialog(task) }
                        )
                    }
                }

                if (done.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "已完成 (${done.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    items(done) { task ->
                        FamilyTaskCard(task = task, onComplete = null)
                    }
                }
            }
        }
    }

    // 完成任务对话框
    uiState.completingTask?.let { task ->
        CompleteTaskDialog(
            task = task,
            submitText = uiState.completeText,
            onTextChange = viewModel::onCompleteTextChange,
            onConfirm = { viewModel.submitComplete() },
            onDismiss = { viewModel.dismissCompleteDialog() },
            isSubmitting = uiState.isSubmitting
        )
    }
}

@Composable
private fun FamilyTaskCard(task: FamilyTaskResponse, onComplete: (() -> Unit)?) {
    val isDone = task.status != "PENDING"
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFFF1F8E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDone) Color(0xFF4CAF50) else Color(0xFF1A237E)
                    )
                    task.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = desc, fontSize = 13.sp, color = Color(0xFF666666), lineHeight = 20.sp)
                    }
                }
                if (isDone) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            task.deadline?.takeIf { it.isNotBlank() }?.let { deadline ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "截止：${deadline.take(10)}",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isDone && onComplete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("完成任务 ✅", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isDone && !task.submitText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "我说：${task.submitText}",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CompleteTaskDialog(
    task: FamilyTaskResponse,
    submitText: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "完成任务：${task.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1A237E)
            )
        },
        text = {
            Column {
                Text(text = "告诉家长你完成了什么：", fontSize = 13.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = submitText,
                    onValueChange = onTextChange,
                    placeholder = { Text("写下你做了什么…", color = Color(0xFFBBBBBB)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3F51B5),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = submitText.isNotBlank() && !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("提交完成", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF999999))
            }
        }
    )
}
