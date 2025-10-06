package com.example.fridgetracker.view.barcode

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private const val TAG = "BarcodeCompose"

@Composable
fun BarcodeScannerScreen(onDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                context = context,
                onBarcodeDetected = onDetected // normal callback
            )
        } else {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Grant camera permission") }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: Context,
    onBarcodeDetected: (String) -> Unit
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner: BarcodeScanner = BarcodeScanning.getClient()
    // Use rememberUpdatedState so the analyzer uses latest lambda
    val onDetectedState by rememberUpdatedState(onBarcodeDetected)

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            try {
                analyzerExecutor.shutdown()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to shutdown analyzer executor", t)
            }
            try {
                scanner.close()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to close barcode scanner", t)
            }
        }
    }

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy: ImageProxy ->
                processImageProxy(scanner, imageProxy) { rawValue ->
                    if (!rawValue.isNullOrEmpty()) {
                        // Pozovi callback na glavnoj niti - veoma važno!
                        ContextCompat.getMainExecutor(ctx).execute {
                            onBarcodeDetected(rawValue)
                        }
                    }
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e(TAG, "Binding camera failed", e)
            }
        }, ContextCompat.getMainExecutor(ctx))

        previewView
    }, modifier = modifier)
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(scanner: BarcodeScanner, imageProxy: ImageProxy, onResult: (String?) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                var found: String? = null
                for (barcode in barcodes) {
                    found = barcode.rawValue
                    if (!found.isNullOrEmpty()) break
                }
                onResult(found)
            }
            .addOnFailureListener { /* optionally log */ }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
