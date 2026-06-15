package com.childlearning.robot.core.camera

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 二维码扫描器
 * 使用 ML Kit Barcode Scanning + CameraX
 */
@Singleton
class QrCodeAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scanner = BarcodeScanning.getClient()

    /**
     * 启动摄像头扫描二维码
     * @param lifecycleOwner 生命周期持有者
     * @param previewView CameraX PreviewView (用于显示摄像头画面)
     * @param onScanned 扫描到结果的回调
     */
    suspend fun startScanning(
        lifecycleOwner: LifecycleOwner,
        onScanned: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            processImage(imageProxy) { result ->
                if (result != null) {
                    onScanned(result)
                    imageAnalysis.clearAnalyzer()
                }
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
        } catch (e: Exception) {
            // 摄像头不可用
        }
    }

    private fun processImage(
        imageProxy: ImageProxy,
        onResult: (String?) -> Unit
    ) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            onResult(null)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val qrContent = barcodes.firstOrNull { barcode ->
                    barcode.format == Barcode.FORMAT_QR_CODE
                }?.rawValue
                onResult(qrContent)
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
