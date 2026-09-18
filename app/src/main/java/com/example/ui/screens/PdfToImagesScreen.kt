package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.DocumentEntity
import com.example.scanner.PdfEngine
import com.example.ui.MainViewModel
import com.example.util.ShareUtil
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImagesScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val documents by viewModel.documentsList.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

  var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
  var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
  var selectedFormat by remember { mutableStateOf(Bitmap.CompressFormat.JPEG) }

  val exportedFiles = remember { mutableStateListOf<File>() }

  // External PDF picker
  val pdfPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      selectedFileUri = uri
      selectedDoc = null
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
            text = stringResource(R.string.pdf_to_images),
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
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (exportedFiles.isNotEmpty()) {
            Button(
              onClick = {
                ShareUtil.shareImages(context, exportedFiles)
              },
              modifier = Modifier.weight(1f).testTag("share_exported_images_button")
            ) {
              Icon(Icons.Default.Share, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Share ${exportedFiles.size} Images")
            }
          } else {
            Button(
              onClick = {
                val doc = selectedDoc
                if (doc != null) {
                  viewModel.exportPdfToImages(context, doc, selectedFormat) { files ->
                    exportedFiles.clear()
                    exportedFiles.addAll(files)
                  }
                } else if (selectedFileUri != null) {
                  // Copy picked PDF to temp file and export
                  try {
                    val tempFile = File(context.cacheDir, "temp_picked.pdf")
                    context.contentResolver.openInputStream(selectedFileUri!!)?.use { input ->
                      FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                      }
                    }
                    val dummyDoc = DocumentEntity(
                      title = "External PDF",
                      pdfPath = tempFile.absolutePath
                    )
                    viewModel.exportPdfToImages(context, dummyDoc, selectedFormat) { files ->
                      exportedFiles.clear()
                      exportedFiles.addAll(files)
                    }
                  } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              enabled = (selectedDoc != null || selectedFileUri != null) && !isProcessing,
              modifier = Modifier.fillMaxWidth().testTag("export_pdf_pages_button")
            ) {
              if (isProcessing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Text("Export Pages as ${if (selectedFormat == Bitmap.CompressFormat.PNG) "PNG" else "JPG"}")
              }
            }
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Exported images preview
      if (exportedFiles.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "Exported Images (${exportedFiles.size} pages)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Spacer(modifier = Modifier.height(12.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(exportedFiles) { file ->
                  Box(
                    modifier = Modifier
                      .size(110.dp, 150.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surface)
                      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                  ) {
                    AsyncImage(
                      model = file,
                      contentDescription = "Page Image",
                      contentScale = ContentScale.Fit,
                      modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Format selector (JPG vs PNG)
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Output Format",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = selectedFormat == Bitmap.CompressFormat.JPEG,
                onClick = { selectedFormat = Bitmap.CompressFormat.JPEG },
                label = { Text("JPG") }
              )
              FilterChip(
                selected = selectedFormat == Bitmap.CompressFormat.PNG,
                onClick = { selectedFormat = Bitmap.CompressFormat.PNG },
                label = { Text("PNG") }
              )
            }
          }
        }
      }

      // Select from Device Files button
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { pdfPicker.launch(arrayOf("application/pdf")) }
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Pick PDF from Files",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                if (selectedFileUri != null) {
                  Text(
                    text = "Selected external PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
            if (selectedFileUri != null) {
              Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }

      // Documents from App Library
      item {
        Text(
          text = "Or Select from App Documents",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      if (documents.isEmpty()) {
        item {
          Text(
            text = "No saved documents in library yet. Scan or create one first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        items(documents) { doc ->
          val isSelected = selectedDoc?.id == doc.id
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
              else MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                selectedDoc = doc
                selectedFileUri = null
                exportedFiles.clear()
              }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = doc.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "${doc.pageCount} pages",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (isSelected) {
                Icon(
                  Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }
    }
  }
}
