package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.MainViewModel
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToCrop: () -> Unit,
  onNavigateToMultiPage: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  val scannedPages by viewModel.scannedPages.collectAsStateWithLifecycle()
  val selectedDocType by viewModel.selectedDocType.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

  var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
  var autoCaptureEnabled by remember { mutableStateOf(false) }
  val imageCapture = remember { ImageCapture.Builder().setFlashMode(flashMode).build() }

  val docTypes = listOf(
    "A4" to "A4 Document",
    "Receipt" to "Receipt",
    "Notes" to "Notes",
    "Form" to "Form",
    "ID Card" to "ID Card",
    "Certificate" to "Certificate",
    "Book" to "Book"
  )

  // Gallery picker inside camera
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val bitmap = BitmapFactory.decodeStream(stream)
          if (bitmap != null) {
            viewModel.addCapturedBitmap(bitmap)
            onNavigateToCrop()
          }
        }
      } catch (_: Exception) {}
    }
  }

  if (!hasCameraPermission) {
    // Permission Request Screen
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = stringResource(R.string.camera_permission_required),
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = stringResource(R.string.camera_permission_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
          modifier = Modifier.testTag("grant_camera_permission_button")
        ) {
          Text(stringResource(R.string.grant_permission))
        }
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = onNavigateBack) {
          Text("Go Back")
        }
      }
    }
    return
  }

  // Camera UI
  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    // Camera PreviewView
    AndroidView(
      factory = { ctx ->
        val previewView = PreviewView(ctx)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
          val cameraProvider = cameraProviderFuture.get()
          val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
          }
          val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
          try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
              lifecycleOwner,
              cameraSelector,
              preview,
              imageCapture
            )
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }, ContextCompat.getMainExecutor(ctx))
        previewView
      },
      modifier = Modifier.fillMaxSize()
    )

    // Viewfinder Scanner Overlay
    ScannerViewfinderOverlay(
      docType = selectedDocType,
      modifier = Modifier.fillMaxSize()
    )

    // Top Controls Bar (Back, Flash, Auto-Capture, Page Count)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onNavigateBack,
        modifier = Modifier
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.5f))
          .testTag("camera_back_button")
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Auto Capture Toggle
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (autoCaptureEnabled) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f))
            .clickable { autoCaptureEnabled = !autoCaptureEnabled }
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = if (autoCaptureEnabled) "Auto ON" else "Auto OFF",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        // Flash Button
        IconButton(
          onClick = {
            flashMode = when (flashMode) {
              ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
              ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
              else -> ImageCapture.FLASH_MODE_AUTO
            }
            imageCapture.flashMode = flashMode
          },
          modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .testTag("flash_toggle_button")
        ) {
          val flashIcon = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
            ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
            else -> Icons.Default.FlashAuto
          }
          Icon(flashIcon, contentDescription = "Flash", tint = Color.White)
        }
      }
    }

    // Bottom Controls & Document Type Carousel
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Document Type Selector Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        items(docTypes) { (typeKey, label) ->
          val isSelected = selectedDocType == typeKey
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f))
              .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
              )
              .clickable { viewModel.setSelectedDocType(typeKey) }
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text(
              text = label,
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      // Action Row: Gallery | Shutter Button | Review / Done Pages
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Gallery Import
        IconButton(
          onClick = { galleryLauncher.launch("image/*") },
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .testTag("camera_gallery_button")
        ) {
          Icon(
            Icons.Default.Image,
            contentDescription = "Gallery",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
          )
        }

        // Shutter Capture Button
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.3f))
            .clickable(enabled = !isProcessing) {
              val executor = Executors.newSingleThreadExecutor()
              imageCapture.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                  override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    if (bitmap != null) {
                      viewModel.addCapturedBitmap(bitmap)
                      ContextCompat.getMainExecutor(context).execute {
                        onNavigateToCrop()
                      }
                    }
                  }

                  override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                  }
                }
              )
            }
            .padding(5.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(62.dp)
              .clip(CircleShape)
              .background(Color.White)
              .testTag("shutter_button")
          )
        }

        // Review Pages Badge Button
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
              if (scannedPages.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)
            )
            .clickable(enabled = scannedPages.isNotEmpty()) {
              onNavigateToMultiPage()
            }
            .testTag("camera_review_pages_button"),
          contentAlignment = Alignment.Center
        ) {
          if (scannedPages.isNotEmpty()) {
            Text(
              text = "${scannedPages.size}",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          } else {
            Icon(
              Icons.Default.Check,
              contentDescription = "Done",
              tint = Color.White.copy(alpha = 0.5f),
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ScannerViewfinderOverlay(
  docType: String,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
  val laserProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_y"
  )

  Canvas(modifier = modifier) {
    val canvasW = size.width
    val canvasH = size.height

    // Frame dimensions based on doc type
    val frameRatio = when (docType) {
      "ID Card" -> 1.58f // Credit card standard
      "Receipt" -> 0.55f // Long narrow receipt
      "Book" -> 1.25f
      else -> 0.707f // A4 standard
    }

    val targetW = canvasW * 0.82f
    val targetH = (targetW / frameRatio).coerceAtMost(canvasH * 0.65f)
    val frameLeft = (canvasW - targetW) / 2f
    val frameTop = (canvasH - targetH) / 2f - 20.dp.toPx()
    val frameRight = frameLeft + targetW
    val frameBottom = frameTop + targetH

    val cornerLen = 28.dp.toPx()
    val strokeWidth = 3.5.dp.toPx()
    val bracketColor = Color(0xFF38BDF8) // Bright Cyan

    // Top-Left corner
    drawLine(bracketColor, Offset(frameLeft, frameTop), Offset(frameLeft + cornerLen, frameTop), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(frameLeft, frameTop), Offset(frameLeft, frameTop + cornerLen), strokeWidth, StrokeCap.Round)

    // Top-Right corner
    drawLine(bracketColor, Offset(frameRight, frameTop), Offset(frameRight - cornerLen, frameTop), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(frameRight, frameTop), Offset(frameRight, frameTop + cornerLen), strokeWidth, StrokeCap.Round)

    // Bottom-Left corner
    drawLine(bracketColor, Offset(frameLeft, frameBottom), Offset(frameLeft + cornerLen, frameBottom), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(frameLeft, frameBottom), Offset(frameLeft, frameBottom - cornerLen), strokeWidth, StrokeCap.Round)

    // Bottom-Right corner
    drawLine(bracketColor, Offset(frameRight, frameBottom), Offset(frameRight - cornerLen, frameBottom), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(frameRight, frameBottom), Offset(frameRight, frameBottom - cornerLen), strokeWidth, StrokeCap.Round)

    // Subtle Animated Scanning Laser Line
    val laserY = frameTop + (targetH * laserProgress)
    drawLine(
      color = Color(0x9938BDF8),
      start = Offset(frameLeft + 4.dp.toPx(), laserY),
      end = Offset(frameRight - 4.dp.toPx(), laserY),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round
    )
  }
}

/**
 * Converts ImageProxy from CameraX to high-quality oriented Bitmap.
 */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
  val buffer = image.planes[0].buffer
  val bytes = ByteArray(buffer.remaining())
  buffer.get(bytes)
  val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

  val rotation = image.imageInfo.rotationDegrees
  if (rotation != 0) {
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
  return bitmap
}
