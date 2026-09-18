package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.scanner.ImageQuality
import com.example.scanner.PdfGenerationOptions
import com.example.scanner.PdfOrientation
import com.example.scanner.PdfPageSize
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  onPdfSaved: () -> Unit
) {
  val context = LocalContext.current
  val folders by viewModel.allFolders.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

  val selectedUris = remember { mutableStateListOf<Uri>() }

  val defaultName = "SnapDoc_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
  var filename by remember { mutableStateOf(defaultName) }
  var pageSize by remember { mutableStateOf(PdfPageSize.A4) }
  var orientation by remember { mutableStateOf(PdfOrientation.PORTRAIT) }
  var quality by remember { mutableStateOf(ImageQuality.HIGH) }
  var selectedFolderId by remember { mutableStateOf(1L) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
  ) { uris ->
    selectedUris.addAll(uris)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
          Text(
            text = stringResource(R.string.image_to_pdf),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Button(
            onClick = {
              val options = PdfGenerationOptions(
                filename = filename.ifBlank { defaultName },
                pageSize = pageSize,
                orientation = orientation,
                quality = quality
              )
              viewModel.convertGalleryImagesToPdf(
                context = context,
                uris = selectedUris,
                options = options,
                folderId = selectedFolderId,
                onSuccess = {
                  onPdfSaved()
                }
              )
            },
            enabled = selectedUris.isNotEmpty() && !isProcessing,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("convert_images_to_pdf_button")
          ) {
            if (isProcessing) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
            } else {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Convert to PDF (${selectedUris.size} images)")
            }
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Image Selector & Horizontal Gallery Carousel
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Selected Images (${selectedUris.size})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Button(
              onClick = { galleryLauncher.launch("image/*") },
              modifier = Modifier.testTag("select_images_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Images")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (selectedUris.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { galleryLauncher.launch("image/*") },
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Default.Add,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Tap to choose images from gallery",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          } else {
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(vertical = 4.dp)
            ) {
              itemsIndexed(selectedUris) { index, uri ->
                Box(
                  modifier = Modifier
                    .size(100.dp, 130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                  AsyncImage(
                    model = uri,
                    contentDescription = "Selected Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )

                  // Remove button
                  IconButton(
                    onClick = { selectedUris.removeAt(index) },
                    modifier = Modifier
                      .size(26.dp)
                      .clip(CircleShape)
                      .background(Color.Black.copy(alpha = 0.6f))
                      .align(Alignment.TopEnd)
                  ) {
                    Icon(
                      Icons.Default.Close,
                      contentDescription = "Remove",
                      tint = Color.White,
                      modifier = Modifier.size(14.dp)
                    )
                  }

                  // Page index badge
                  Box(
                    modifier = Modifier
                      .size(22.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary)
                      .align(Alignment.BottomStart)
                      .padding(2.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "${index + 1}",
                      color = Color.White,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }

      // PDF Settings
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "PDF Output Options",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )

          // Filename
          OutlinedTextField(
            value = filename,
            onValueChange = { filename = it },
            label = { Text("PDF Filename") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Page size
          Text("Page Size", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = pageSize == PdfPageSize.A4,
              onClick = { pageSize = PdfPageSize.A4 },
              label = { Text("A4 Standard") }
            )
            FilterChip(
              selected = pageSize == PdfPageSize.ORIGINAL,
              onClick = { pageSize = PdfPageSize.ORIGINAL },
              label = { Text("Original Aspect") }
            )
          }

          // Orientation
          Text("Orientation", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = orientation == PdfOrientation.PORTRAIT,
              onClick = { orientation = PdfOrientation.PORTRAIT },
              label = { Text("Portrait") }
            )
            FilterChip(
              selected = orientation == PdfOrientation.LANDSCAPE,
              onClick = { orientation = PdfOrientation.LANDSCAPE },
              label = { Text("Landscape") }
            )
          }

          // Quality
          Text("Quality & Compression", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = quality == ImageQuality.HIGH,
              onClick = { quality = ImageQuality.HIGH },
              label = { Text("High") }
            )
            FilterChip(
              selected = quality == ImageQuality.MEDIUM,
              onClick = { quality = ImageQuality.MEDIUM },
              label = { Text("Medium") }
            )
            FilterChip(
              selected = quality == ImageQuality.ECONOMY,
              onClick = { quality = ImageQuality.ECONOMY },
              label = { Text("Economy") }
            )
          }

          // Target folder
          Text("Target Folder", style = MaterialTheme.typography.labelMedium)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders.size) { idx ->
              val f = folders[idx]
              FilterChip(
                selected = selectedFolderId == f.id,
                onClick = { selectedFolderId = f.id },
                label = { Text(f.name) }
              )
            }
          }
        }
      }
    }
  }
}
