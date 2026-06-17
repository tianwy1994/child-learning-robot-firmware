package com.childlearning.robot.ui.screens.homework

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    onBack: () -> Unit,
    viewModel: HomeworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    val subjects = listOf("数学", "语文", "英语", "科学", "其他")

    // 相机
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            viewModel.submitPhoto(photoUri!!)
        }
    }

    // 相册
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.submitPhoto(it) }
    }

    // 相机权限
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = createImageFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("作业帮") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 说明
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("拍照或选择作业图片，AI 将帮你识别和批改",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 图片预览
            if (uiState.selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(uiState.selectedImageUri),
                    contentDescription = "作业图片",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 操作按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val permResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        val hasPermission = permResult == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            val file = createImageFile(context)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            photoUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("拍照")
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("选图")
                }
            }

            // 加载中
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator()
                Text("识别中...", style = MaterialTheme.typography.bodyMedium)
            }

            // OCR 结果 + 提交
            if (uiState.ocrResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("识别结果", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.ocrResult!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showSubjectDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text("提交作业", style = MaterialTheme.typography.titleMedium)
                }
            }

            // 提交成功
            if (uiState.submitSuccess) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("作业提交成功！", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.clearResult() }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)) {
                    Text("继续提交")
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    // 科目选择对话框
    if (showSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showSubjectDialog = false },
            title = { Text("选择科目") },
            text = {
                Column {
                    subjects.forEach { subject ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            RadioButton(
                                selected = uiState.subject == subject,
                                onClick = { viewModel.setSubject(subject) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(subject, modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSubjectDialog = false
                    viewModel.submitHomework(uiState.subject)
                }) { Text("确认提交") }
            },
            dismissButton = {
                TextButton(onClick = { showSubjectDialog = false }) { Text("取消") }
            }
        )
    }
}

private fun createImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.cacheDir, "homework_photos")
    if (!storageDir.exists()) storageDir.mkdirs()
    return File(storageDir, "HOMEWORK_${timeStamp}.jpg")
}