package com.velocity.app.ui.pairing

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.data.repository.ServerConfig
import com.velocity.app.data.repository.ServerConfigManager
import com.velocity.app.ui.components.LucideIcons
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.theme.VelocityTypography
import com.velocity.app.ui.util.VelocityHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

enum class PairingTab {
    SCAN_QR,
    MANUAL
}

@Composable
fun PairingScreen(
    initialConfig: ServerConfig? = null,
    onPairingSuccess: (ServerConfig) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(PairingTab.SCAN_QR) }
    var serverUrl by remember { mutableStateOf(initialConfig?.baseUrl ?: "https://chat.sathwik.work") }
    var cfClientId by remember { mutableStateOf(initialConfig?.cfClientId ?: "") }
    var cfClientSecret by remember { mutableStateOf(initialConfig?.cfClientSecret ?: "") }

    var isTestingConnection by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            activeTab = PairingTab.MANUAL
        }
    }

    fun verifyAndSave(config: ServerConfig) {
        if (isTestingConnection) return
        isTestingConnection = true
        statusMessage = "Connecting to server..."
        isError = false

        coroutineScope.launch {
            val repo = ChatRepository(config)
            val result = withContext(Dispatchers.IO) { repo.testConnection() }
            isTestingConnection = false

            if (result.isSuccess) {
                VelocityHaptics.success(context)
                statusMessage = "Connected successfully!"
                ServerConfigManager.saveConfig(context, config)
                onPairingSuccess(config)
            } else {
                VelocityHaptics.error(context)
                isError = true
                statusMessage = result.exceptionOrNull()?.message ?: "Unable to connect to server"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelocityColors.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Brand Header
            Text(
                text = "Velocity",
                style = VelocityTypography.headlineMedium,
                color = VelocityColors.Accent,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Pair your device to start thinking together",
                style = VelocityTypography.bodyMedium,
                color = VelocityColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Apple-Style Segmented Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VelocityColors.SurfaceCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab: Scan QR
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeTab == PairingTab.SCAN_QR) VelocityColors.SurfaceElevated else Color.Transparent)
                        .clickable {
                            VelocityHaptics.lightClick(context)
                            activeTab = PairingTab.SCAN_QR
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Qr),
                            contentDescription = null,
                            tint = if (activeTab == PairingTab.SCAN_QR) VelocityColors.TextPrimary else VelocityColors.TextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Scan QR",
                            style = VelocityTypography.titleSmall,
                            color = if (activeTab == PairingTab.SCAN_QR) VelocityColors.TextPrimary else VelocityColors.TextDim
                        )
                    }
                }

                // Tab: Manual
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeTab == PairingTab.MANUAL) VelocityColors.SurfaceElevated else Color.Transparent)
                        .clickable {
                            VelocityHaptics.lightClick(context)
                            activeTab = PairingTab.MANUAL
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Settings),
                            contentDescription = null,
                            tint = if (activeTab == PairingTab.MANUAL) VelocityColors.TextPrimary else VelocityColors.TextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Manual",
                            style = VelocityTypography.titleSmall,
                            color = if (activeTab == PairingTab.MANUAL) VelocityColors.TextPrimary else VelocityColors.TextDim
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Body Content based on Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (activeTab == PairingTab.SCAN_QR) {
                    if (hasCameraPermission) {
                        QrScannerView(
                            onQrScanned = { payload ->
                                val parsed = ServerConfigManager.parseQrPayload(payload)
                                if (parsed != null && !isTestingConnection) {
                                    verifyAndSave(parsed)
                                }
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(VelocityColors.SurfaceCard)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Camera Access Required",
                                style = VelocityTypography.titleMedium,
                                color = VelocityColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "To scan the pairing code on your desktop screen at /mobile, grant camera access.",
                                style = VelocityTypography.bodyMedium,
                                color = VelocityColors.TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VelocityColors.TextPrimary)
                                    .clickable {
                                        VelocityHaptics.lightClick(context)
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Allow Camera",
                                    style = VelocityTypography.titleSmall,
                                    color = VelocityColors.Canvas,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Manual Form
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(VelocityColors.SurfaceCard)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Server Details",
                            style = VelocityTypography.titleMedium,
                            color = VelocityColors.TextPrimary
                        )

                        // Server URL Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Server URL",
                                style = VelocityTypography.labelSmall,
                                color = VelocityColors.TextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VelocityColors.SurfaceCapsule)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = serverUrl,
                                    onValueChange = { serverUrl = it },
                                    textStyle = TextStyle(
                                        color = VelocityColors.TextPrimary,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(VelocityColors.AccentSky),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Cloudflare Client ID (Optional)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Cloudflare Client ID (Optional)",
                                style = VelocityTypography.labelSmall,
                                color = VelocityColors.TextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VelocityColors.SurfaceCapsule)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = cfClientId,
                                    onValueChange = { cfClientId = it },
                                    textStyle = TextStyle(
                                        color = VelocityColors.TextPrimary,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(VelocityColors.AccentSky),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Cloudflare Client Secret (Optional)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Cloudflare Client Secret (Optional)",
                                style = VelocityTypography.labelSmall,
                                color = VelocityColors.TextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VelocityColors.SurfaceCapsule)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = cfClientSecret,
                                    onValueChange = { cfClientSecret = it },
                                    textStyle = TextStyle(
                                        color = VelocityColors.TextPrimary,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(VelocityColors.AccentSky),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val config = ServerConfig(
                                            baseUrl = serverUrl,
                                            cfClientId = cfClientId,
                                            cfClientSecret = cfClientSecret,
                                            isPaired = true
                                        )
                                        verifyAndSave(config)
                                    }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Quick Defaults Pill Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VelocityColors.SurfaceCode)
                                    .clickable {
                                        VelocityHaptics.subtleTick(context)
                                        serverUrl = "https://chat.sathwik.work"
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "chat.sathwik.work",
                                    style = VelocityTypography.labelSmall,
                                    color = VelocityColors.TextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VelocityColors.SurfaceCode)
                                    .clickable {
                                        VelocityHaptics.subtleTick(context)
                                        serverUrl = "http://10.0.2.2:8000"
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Emulator (10.0.2.2)",
                                    style = VelocityTypography.labelSmall,
                                    color = VelocityColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status or Error Feedback Pill
            AnimatedVisibility(
                visible = statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isError) androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else VelocityColors.AccentEmerald.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusMessage ?: "",
                        style = VelocityTypography.bodySmall,
                        color = if (isError) androidx.compose.material3.MaterialTheme.colorScheme.error else VelocityColors.AccentEmerald,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button (Connect)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VelocityColors.TextPrimary)
                    .clickable(enabled = !isTestingConnection && serverUrl.isNotBlank()) {
                        VelocityHaptics.lightClick(context)
                        val config = ServerConfig(
                            baseUrl = serverUrl,
                            cfClientId = cfClientId,
                            cfClientSecret = cfClientSecret,
                            isPaired = true
                        )
                        verifyAndSave(config)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = VelocityColors.Canvas,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Connect",
                            style = VelocityTypography.titleSmall,
                            color = VelocityColors.Canvas,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            painter = painterResource(LucideIcons.ChevronRight),
                            contentDescription = null,
                            tint = VelocityColors.Canvas,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrScannerView(
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    var lastScannedValue by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(VelocityColors.SurfaceCode),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank() && rawValue != lastScannedValue) {
                                            lastScannedValue = rawValue
                                            onQrScanned(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Apple Reticle Overlay
        Box(
            modifier = Modifier
                .size(220.dp)
                .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(18.dp))
        )
    }
}
