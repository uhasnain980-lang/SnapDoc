package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DocumentEntity
import com.example.data.model.FolderEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AdBanner
import com.example.util.ShareUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: MainViewModel,
  onNavigateToScan: () -> Unit,
  onNavigateToImageToPdf: () -> Unit,
  onNavigateToPdfToImages: () -> Unit,
  onNavigateToOcr: () -> Unit,
  onNavigateToLocker: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToMultiPage: () -> Unit
) {
  val context = LocalContext.current
  val recentDocs by viewModel.recentDocuments.collectAsStateWithLifecycle()
  val allDocs by viewModel.documentsList.collectAsStateWithLifecycle()
  val folders by viewModel.allFolders.collectAsStateWithLifecycle()
  val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val lockedDocs by viewModel.lockedDocuments.collectAsStateWithLifecycle()

  var showCreateFolderDialog by remember { mutableStateOf(false) }
  var newFolderName by remember { mutableStateOf("") }

  // Gallery multi-picker for "Scan from Gallery"
  val galleryLauncher = rememberLauncherForActivityResult(
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
      onNavigateToMultiPage()
    }
  }

  // Single image picker for direct OCR
  val ocrImageLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
          if (bitmap != null) {
            viewModel.runOcrOnBitmap(bitmap)
            onNavigateToOcr()
          }
        }
      } catch (_: Exception) {}
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  Brush.linearGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary,
                      MaterialTheme.colorScheme.secondary
                    )
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          // Private Locker Button with badge count
          IconButton(
            onClick = onNavigateToLocker,
            modifier = Modifier.testTag("private_locker_button")
          ) {
            Box {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Private Locker",
                tint = if (lockedDocs.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (lockedDocs.isNotEmpty()) {
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .align(Alignment.TopEnd),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${lockedDocs.size}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          // Settings Button
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      )
    },
    bottomBar = {
      AdBanner()
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      // Search Bar
      item {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = {
              Text(
                stringResource(R.string.search_documents),
                fontSize = 14.sp
              )
            },
            leadingIcon = {
              Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_text_field")
          )
        }
      }

      // Main Primary Action Cards Grid (Scan Document, Scan Gallery, Create PDF, Extract Text, PDF to Images)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Top Big Scan Button (Prominent Camera Call to Action)
          Card(
            onClick = {
              viewModel.clearScanningSession()
              onNavigateToScan()
            },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(96.dp)
              .testTag("scan_document_card")
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = stringResource(R.string.scan_document),
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                  text = "Camera auto-edge detection & perspective crop",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
              }
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CameraAlt,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(30.dp)
                )
              }
            }
          }

          // 2x2 Grid for Secondary Core Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            ActionQuickCard(
              title = stringResource(R.string.scan_from_gallery),
              subtitle = "Pick images to scan",
              icon = Icons.Default.Image,
              gradient = listOf(Color(0xFF0284C7), Color(0xFF0EA5E9)),
              onClick = {
                viewModel.clearScanningSession()
                galleryLauncher.launch("image/*")
              },
              modifier = Modifier
                .weight(1f)
                .testTag("scan_gallery_card")
            )
            ActionQuickCard(
              title = stringResource(R.string.create_pdf),
              subtitle = "Convert images to PDF",
              icon = Icons.Default.PictureAsPdf,
              gradient = listOf(Color(0xFFEA580C), Color(0xFFF97316)),
              onClick = onNavigateToImageToPdf,
              modifier = Modifier
                .weight(1f)
                .testTag("create_pdf_card")
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            ActionQuickCard(
              title = stringResource(R.string.extract_text),
              subtitle = "On-device OCR scanner",
              icon = Icons.Default.TextFields,
              gradient = listOf(Color(0xFF059669), Color(0xFF10B981)),
              onClick = {
                ocrImageLauncher.launch("image/*")
              },
              modifier = Modifier
                .weight(1f)
                .testTag("extract_text_card")
            )
            ActionQuickCard(
              title = stringResource(R.string.pdf_to_images),
              subtitle = "Export PDF pages as JPG/PNG",
              icon = Icons.Default.Description,
              gradient = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6)),
              onClick = onNavigateToPdfToImages,
              modifier = Modifier
                .weight(1f)
                .testTag("pdf_to_images_card")
            )
          }
        }
      }

      // My Folders Section
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(R.string.my_folders),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          TextButton(
            onClick = { showCreateFolderDialog = true },
            modifier = Modifier.testTag("add_folder_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.new_folder), fontSize = 13.sp)
          }
        }

        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // "All" Folder Chip
          item {
            FolderFilterChip(
              name = stringResource(R.string.all_docs),
              isSelected = selectedFolderId == null,
              color = MaterialTheme.colorScheme.primary,
              onClick = { viewModel.setSelectedFolder(null) }
            )
          }

          items(folders) { folder ->
            FolderFilterChip(
              name = folder.name,
              isSelected = selectedFolderId == folder.id,
              color = Color(folder.colorHex),
              onClick = {
                if (selectedFolderId == folder.id) {
                  viewModel.setSelectedFolder(null)
                } else {
                  viewModel.setSelectedFolder(folder.id)
                }
              }
            )
          }
        }
      }

      // Recent Documents / Filtered Documents List
      item {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (searchQuery.isNotBlank()) "Search Results"
            else if (selectedFolderId != null) "Folder Documents"
            else stringResource(R.string.recent_documents),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${allDocs.size} docs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (allDocs.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = stringResource(R.string.no_documents_yet),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = stringResource(R.string.no_documents_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      } else {
        items(allDocs, key = { it.id }) { doc ->
          DocumentListItem(
            doc = doc,
            folders = folders,
            onOpen = { ShareUtil.openPdf(context, File(doc.pdfPath)) },
            onShare = { ShareUtil.sharePdf(context, File(doc.pdfPath), doc.title) },
            onRename = { newTitle -> viewModel.renameDocument(doc.id, newTitle) },
            onDelete = { viewModel.deleteDocument(doc) },
            onMoveToFolder = { folderId -> viewModel.moveDocumentToFolder(doc.id, folderId) },
            onToggleLock = { viewModel.toggleDocumentLock(doc.id, true) }
          )
        }
      }
    }
  }

  // Create Folder Dialog
  if (showCreateFolderDialog) {
    AlertDialog(
      onDismissRequest = { showCreateFolderDialog = false },
      title = { Text(stringResource(R.string.create_folder)) },
      text = {
        OutlinedTextField(
          value = newFolderName,
          onValueChange = { newFolderName = it },
          placeholder = { Text(stringResource(R.string.folder_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newFolderName.isNotBlank()) {
              viewModel.createNewFolder(newFolderName)
              newFolderName = ""
              showCreateFolderDialog = false
            }
          }
        ) {
          Text(stringResource(R.string.save))
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateFolderDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}

@Composable
fun ActionQuickCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  gradient: List<Color>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.height(115.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(22.dp)
        )
      }
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun FolderFilterChip(
  name: String,
  isSelected: Boolean,
  color: Color,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(
        if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
      )
      .border(
        width = 1.dp,
        color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 7.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(if (isSelected) Color.White else color)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = name,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        ),
        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun DocumentListItem(
  doc: DocumentEntity,
  folders: List<FolderEntity>,
  onOpen: () -> Unit,
  onShare: () -> Unit,
  onRename: (String) -> Unit,
  onDelete: () -> Unit,
  onMoveToFolder: (Long) -> Unit,
  onToggleLock: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var showMoveDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var renameInput by remember(doc.title) { mutableStateOf(doc.title) }

  val folderName = folders.find { it.id == doc.folderId }?.name ?: "Documents"
  val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(doc.createdAt))
  val formattedSize = formatFileSize(doc.fileSizeBytes)

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 5.dp)
      .clickable(onClick = onOpen)
      .testTag("doc_item_${doc.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Thumbnail or PDF Icon Box
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        if (doc.thumbnailPath.isNotEmpty() && File(doc.thumbnailPath).exists()) {
          AsyncImage(
            model = File(doc.thumbnailPath),
            contentDescription = "Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = doc.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "${doc.pageCount} ${if (doc.pageCount == 1) "page" else "pages"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
          Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
          Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = formattedSize,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
          Text(
            text = folderName,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Quick share & more actions
      IconButton(onClick = onShare) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = "Share",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }

      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Open PDF") },
            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
            onClick = {
              showMenu = false
              onOpen()
            }
          )
          DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = {
              showMenu = false
              showRenameDialog = true
            }
          )
          DropdownMenuItem(
            text = { Text("Move to Folder") },
            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
            onClick = {
              showMenu = false
              showMoveDialog = true
            }
          )
          DropdownMenuItem(
            text = { Text("Move to Private Locker") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            onClick = {
              showMenu = false
              onToggleLock()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
              showMenu = false
              showDeleteDialog = true
            }
          )
        }
      }
    }
  }

  // Rename Dialog
  if (showRenameDialog) {
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = { Text("Rename Document") },
      text = {
        OutlinedTextField(
          value = renameInput,
          onValueChange = { renameInput = it },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (renameInput.isNotBlank()) {
              onRename(renameInput.trim())
              showRenameDialog = false
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRenameDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Move to Folder Dialog
  if (showMoveDialog) {
    AlertDialog(
      onDismissRequest = { showMoveDialog = false },
      title = { Text("Move to Folder") },
      text = {
        Column {
          folders.forEach { folder ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onMoveToFolder(folder.id)
                  showMoveDialog = false
                }
                .padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = Color(folder.colorHex),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge
              )
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showMoveDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Delete Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Document?") },
      text = { Text("Are you sure you want to delete '${doc.title}'? This action cannot be undone.") },
      confirmButton = {
        Button(
          onClick = {
            onDelete()
            showDeleteDialog = false
          },
          colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
          )
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 KB"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  return if (mb >= 1.0) {
    "%.1f MB".format(mb)
  } else {
    "%.0f KB".format(kb)
  }
}
