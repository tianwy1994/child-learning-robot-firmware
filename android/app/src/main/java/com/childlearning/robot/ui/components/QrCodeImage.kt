package com.childlearning.robot.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.childlearning.robot.core.camera.QrCodeGenerator

/**
 * 二维码图片组件
 * 使用 QrCodeGenerator 生成二维码并显示
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Int = 512
) {
    val qrGenerator = remember { QrCodeGenerator() }
    val bitmap = remember(content, size) { qrGenerator.generate(content, size) }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR Code",
        modifier = modifier
    )
}
