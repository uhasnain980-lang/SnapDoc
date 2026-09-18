package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AdMob Configuration for easy integration.
 * When ready to display live production Google AdMob ads,
 * simply update ADMOB_APP_ID and BANNER_AD_UNIT_ID below.
 */
object AdConfig {
  const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713" // Google Test App ID
  const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Google Test Banner ID
  var isAdsEnabled: Boolean = true
}

@Composable
fun AdBanner(
  modifier: Modifier = Modifier
) {
  if (!AdConfig.isAdsEnabled) return

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
      .height(52.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = "Ad",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.primary
        )
      }
      Text(
        text = "AdMob Banner Ready • Unit: ${AdConfig.BANNER_AD_UNIT_ID.takeLast(10)}",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
