package com.example.ui.screens

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.scanner.CropQuad
import com.example.scanner.ImageProcessingEngine
import com.example.ui.MainViewModel
import com.example.ui.components.InteractiveCropView
import kotlinx.coroutines.launch

@Composable
fun CropScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToEnhance: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val pages by viewModel.scannedPages.collectAsStateWithLifecycle()
  val pageIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

  if (pages.isEmpty() || pageIndex !in pages.indices) {
    onNavigateBack()
    return
  }

  val currentPage = pages[pageIndex]
  var currentQuad by remember(currentPage.id) { mutableStateOf(currentPage.quad) }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = Color(0xFF0F172A)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
      ) {
        // Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("crop_back_button")
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }

          Text(
            text = "Adjust Document Edges",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
          )

          // Page counter indicator (e.g. 1 / 3)
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White.copy(alpha = 0.2f))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "${pageIndex + 1} / ${pages.size}",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // Main Interactive Crop Canvas View
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          InteractiveCropView(
            bitmap = currentPage.originalBitmap,
            cropQuad = currentQuad,
            onCropQuadChanged = { updated ->
              currentQuad = updated
            },
            modifier = Modifier.fillMaxSize()
          )

          if (isProcessing) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
          }
        }

        // Toolbar: Auto-detect, Rotate, Full page
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Auto Detect
          IconButton(
            onClick = {
              scope.launch {
                val detected = ImageProcessingEngine.autoDetectDocumentEdges(currentPage.originalBitmap)
                currentQuad = detected
              }
            },
            modifier = Modifier.testTag("auto_detect_button")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Auto", color = Color.White, fontSize = 11.sp)
            }
          }

          // Rotate
          IconButton(
            onClick = {
              viewModel.rotatePage(pageIndex)
            },
            modifier = Modifier.testTag("crop_rotate_button")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.White)
              Text("Rotate", color = Color.White, fontSize = 11.sp)
            }
          }

          // Full page (Reset crop)
          IconButton(
            onClick = {
              currentQuad = CropQuad(
                topLeft = PointF(0f, 0f),
                topRight = PointF(1f, 0f),
                bottomRight = PointF(1f, 1f),
                bottomLeft = PointF(0f, 1f)
              )
            },
            modifier = Modifier.testTag("reset_crop_button")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.CropFree, contentDescription = null, tint = Color.White)
              Text("Full", color = Color.White, fontSize = 11.sp)
            }
          }

          // Retake
          IconButton(
            onClick = {
              viewModel.deletePage(pageIndex)
              onNavigateBack()
            },
            modifier = Modifier.testTag("retake_button")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
              Text("Retake", color = Color.White, fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Action: Confirm Crop & Perspective Warp
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.weight(1f).testTag("cancel_crop_button")
          ) {
            Text("Back")
          }

          Spacer(modifier = Modifier.width(16.dp))

          Button(
            onClick = {
              viewModel.updatePageCrop(pageIndex, currentQuad)
              onNavigateToEnhance()
            },
            enabled = !isProcessing,
            modifier = Modifier.weight(1f).testTag("next_enhance_button")
          ) {
            Text("Next: Filter")
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}
