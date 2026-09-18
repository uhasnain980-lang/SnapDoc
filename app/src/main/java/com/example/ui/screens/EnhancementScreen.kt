package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.scanner.FilterType
import com.example.ui.MainViewModel

@Composable
fun EnhancementScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToMultiPage: () -> Unit
) {
  val pages by viewModel.scannedPages.collectAsStateWithLifecycle()
  val pageIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

  if (pages.isEmpty() || pageIndex !in pages.indices) {
    onNavigateBack()
    return
  }

  val currentPage = pages[pageIndex]

  var selectedFilter by remember(currentPage.id) { mutableStateOf(currentPage.filterType) }
  var brightness by remember(currentPage.id) { mutableFloatStateOf(currentPage.brightness) }
  var contrast by remember(currentPage.id) { mutableFloatStateOf(currentPage.contrast) }
  var showAdjustments by remember { mutableStateOf(false) }

  val filterOptions = listOf(
    FilterType.ORIGINAL to "Original",
    FilterType.AUTO to "Auto Magic",
    FilterType.BLACK_AND_WHITE to "B&W Document",
    FilterType.GRAYSCALE to "Grayscale",
    FilterType.COLOR_BOOST to "Vibrant Color",
    FilterType.SHARPEN to "Sharpen"
  )

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Top Navigation Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("enhance_back_button")
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Text(
          text = stringResource(R.string.enhance_image),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        // Apply to all pages action
        IconButton(
          onClick = {
            viewModel.applyFilterToAllPages(selectedFilter, brightness, contrast)
          },
          modifier = Modifier.testTag("apply_all_pages_button")
        ) {
          Icon(
            Icons.Default.DoneAll,
            contentDescription = "Apply to All",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Enhanced Image Preview Canvas
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Image(
            bitmap = currentPage.enhancedBitmap.asImageBitmap(),
            contentDescription = "Enhanced Document Page",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(6.dp)
          )

          if (isProcessing) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }

      // Filter selector carousel
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
      ) {
        items(filterOptions) { (filter, label) ->
          val isSelected = selectedFilter == filter
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
              )
              .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
              )
              .clickable {
                selectedFilter = filter
                viewModel.applyPageFilter(pageIndex, filter, brightness, contrast)
              }
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Text(
              text = label,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      // Brightness and Contrast Sliders Toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (showAdjustments) "Hide Brightness & Contrast" else "Fine-tune Brightness & Contrast",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clickable { showAdjustments = !showAdjustments }
        )
      }

      if (showAdjustments) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
          // Brightness Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Brightness", style = MaterialTheme.typography.bodySmall)
            Text("${brightness.toInt()}", style = MaterialTheme.typography.bodySmall)
          }
          Slider(
            value = brightness,
            onValueChange = {
              brightness = it
              viewModel.applyPageFilter(pageIndex, selectedFilter, brightness, contrast)
            },
            valueRange = -50f..50f,
            modifier = Modifier.fillMaxWidth()
          )

          // Contrast Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Contrast", style = MaterialTheme.typography.bodySmall)
            Text("${contrast.toInt()}", style = MaterialTheme.typography.bodySmall)
          }
          Slider(
            value = contrast,
            onValueChange = {
              contrast = it
              viewModel.applyPageFilter(pageIndex, selectedFilter, brightness, contrast)
            },
            valueRange = -50f..50f,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // Bottom Navigation Buttons
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onNavigateBack,
          modifier = Modifier.weight(1f).testTag("enhance_re_crop_button")
        ) {
          Text("Re-crop")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(
          onClick = onNavigateToMultiPage,
          modifier = Modifier.weight(1f).testTag("enhance_done_button")
        ) {
          Text("Pages & PDF")
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}
