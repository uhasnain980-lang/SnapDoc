package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.scanner.ImageQuality
import com.example.scanner.PdfGenerationOptions
import com.example.scanner.PdfOrientation
import com.example.scanner.PdfPageSize
import com.example.ui.MainViewModel
import com.example.ui.components.SignatureDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPageScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToCamera: () -> Unit,
  onNavigateToCrop: () -> Unit,
  onNavigateToOcr: () -> Unit,
  onPdfSaved: () -> Unit
) {
  val context = LocalContext.current
  val pages by viewModel.scannedPages.collectAsStateWithLifecycle()
  val folders by viewModel.allFolders.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

  var showPdfDialog by remember { mutableStateOf(false) }
  var showSignatureDialog by remember { mutableStateOf(false) }
  var targetPageForSignature by remember { mutableStateOf(0) }

  // PDF configuration options state
  val defaultName = "SnapDoc_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
  var pdfFilename by remember { mutableStateOf(defaultName) }
  var selectedPageSize by remember { mutableStateOf(PdfPageSize.A4) }
  var selectedOrientation by remember { mutableStateOf(PdfOrientation.PORTRAIT) }
  var selectedQuality by remember { mutableStateOf(ImageQuality.HIGH) }
  var selectedFolderId by remember { mutableStateOf(1L) }

  // Gallery picker to add more pages
  val addGalleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
  ) { uris ->
    if (uris.isNotEmpty()) {
      uris.forEach { uri ->
        try {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
            if (bitmap != null) {
              viewModel.addCapturedBitmap(bitmap)
            }
          }
        } catch (_: Exception) {}
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
          Text(
            text = "Scanned Pages (${pages.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          // OCR Extraction for current page
          IconButton(
            onClick = {
              if (pages.isNotEmpty()) {
                viewModel.runOcrOnCurrentPage()
                onNavigateToOcr()
              }
            },
            enabled = pages.isNotEmpty(),
            modifier = Modifier.testTag("multi_page_ocr_button")
          ) {
            Icon(Icons.Default.TextFields, contentDescription = "OCR Text")
          }
        }
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Add Page Dropdown / Camera button
          OutlinedButton(
            onClick = onNavigateToCamera,
            modifier = Modifier.testTag("add_page_camera_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Page")
          }

          // Create PDF Button
          Button(
            onClick = { showPdfDialog = true },
            enabled = pages.isNotEmpty() && !isProcessing,
            modifier = Modifier.testTag("create_pdf_button")
          ) {
            if (isProcessing) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
              )
            } else {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save as PDF")
            }
          }
        }
      }
    }
  ) { paddingValues ->
    if (pages.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("No pages scanned yet.")
          Spacer(modifier = Modifier.height(12.dp))
          Button(onClick = onNavigateToCamera) {
            Text("Open Camera")
          }
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("page_card_$index")
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              // Image thumbnail
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(160.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .clickable {
                    viewModel.setCurrentPageIndex(index)
                    onNavigateToCrop()
                  }
              ) {
                Image(
                  bitmap = page.enhancedBitmap.asImageBitmap(),
                  contentDescription = "Page ${index + 1}",
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.fillMaxSize()
                )

                // Page number badge
                Box(
                  modifier = Modifier
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(24.dp)
                    .align(Alignment.TopStart),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${index + 1}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                // Edit/Crop indicator
                Box(
                  modifier = Modifier
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .size(24.dp)
                    .align(Alignment.TopEnd),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Page controls (Rotate, Signature, Delete, Reorder)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Move Left / Up
                IconButton(
                  onClick = { viewModel.reorderPage(index, index - 1) },
                  enabled = index > 0,
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.ArrowUpward, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                }

                // Rotate
                IconButton(
                  onClick = { viewModel.rotatePage(index) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(16.dp))
                }

                // Add Signature
                IconButton(
                  onClick = {
                    targetPageForSignature = index
                    showSignatureDialog = true
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    Icons.Default.Draw,
                    contentDescription = "Sign",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                }

                // Delete Page
                IconButton(
                  onClick = { viewModel.deletePage(index) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Signature Pad Dialog
  if (showSignatureDialog) {
    SignatureDialog(
      onDismiss = { showSignatureDialog = false },
      onSignatureCreated = { sigBitmap ->
        viewModel.addSignatureToPage(targetPageForSignature, sigBitmap)
      }
    )
  }

  // PDF Generation Configuration Dialog
  if (showPdfDialog) {
    AlertDialog(
      onDismissRequest = { showPdfDialog = false },
      title = { Text(stringResource(R.string.pdf_options)) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Filename
          OutlinedTextField(
            value = pdfFilename,
            onValueChange = { pdfFilename = it },
            label = { Text(stringResource(R.string.pdf_filename)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Page Size (A4 vs Original)
          Text("Page Size", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = selectedPageSize == PdfPageSize.A4,
              onClick = { selectedPageSize = PdfPageSize.A4 },
              label = { Text("A4 Standard") }
            )
            FilterChip(
              selected = selectedPageSize == PdfPageSize.ORIGINAL,
              onClick = { selectedPageSize = PdfPageSize.ORIGINAL },
              label = { Text("Original") }
            )
          }

          // Orientation (Portrait vs Landscape)
          Text("Orientation", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = selectedOrientation == PdfOrientation.PORTRAIT,
              onClick = { selectedOrientation = PdfOrientation.PORTRAIT },
              label = { Text("Portrait") }
            )
            FilterChip(
              selected = selectedOrientation == PdfOrientation.LANDSCAPE,
              onClick = { selectedOrientation = PdfOrientation.LANDSCAPE },
              label = { Text("Landscape") }
            )
          }

          // Quality (High, Medium, Economy)
          Text("Quality & Compression", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = selectedQuality == ImageQuality.HIGH,
              onClick = { selectedQuality = ImageQuality.HIGH },
              label = { Text("High") }
            )
            FilterChip(
              selected = selectedQuality == ImageQuality.MEDIUM,
              onClick = { selectedQuality = ImageQuality.MEDIUM },
              label = { Text("Medium") }
            )
            FilterChip(
              selected = selectedQuality == ImageQuality.ECONOMY,
              onClick = { selectedQuality = ImageQuality.ECONOMY },
              label = { Text("Economy") }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showPdfDialog = false
            val options = PdfGenerationOptions(
              filename = pdfFilename.ifBlank { defaultName },
              pageSize = selectedPageSize,
              orientation = selectedOrientation,
              quality = selectedQuality
            )
            viewModel.createAndSavePdf(
              context = context,
              filename = options.filename,
              folderId = selectedFolderId,
              options = options,
              onSuccess = {
                onPdfSaved()
              }
            )
          }
        ) {
          Text("Create PDF")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPdfDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}
