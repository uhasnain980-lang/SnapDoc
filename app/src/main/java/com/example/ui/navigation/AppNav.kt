package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CropScreen
import com.example.ui.screens.EnhancementScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageToPdfScreen
import com.example.ui.screens.MultiPageScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.PdfToImagesScreen
import com.example.ui.screens.PrivateLockerScreen
import com.example.ui.screens.SettingsScreen

enum class Screen {
  HOME,
  CAMERA_SCANNER,
  CROP,
  ENHANCEMENT,
  MULTI_PAGE,
  OCR,
  IMAGE_TO_PDF,
  PDF_TO_IMAGES,
  PRIVATE_LOCKER,
  SETTINGS
}

@Composable
fun AppNavigation(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val backStack = remember { mutableStateListOf(Screen.HOME) }
  val currentScreen = backStack.lastOrNull() ?: Screen.HOME

  fun navigateTo(screen: Screen) {
    backStack.add(screen)
  }

  fun navigateBack() {
    if (backStack.size > 1) {
      backStack.removeAt(backStack.lastIndex)
    }
  }

  fun navigateToHomeClearingStack() {
    backStack.clear()
    backStack.add(Screen.HOME)
  }

  BackHandler(enabled = backStack.size > 1) {
    navigateBack()
  }

  Surface(modifier = modifier.fillMaxSize()) {
    AnimatedContent(
      targetState = currentScreen,
      transitionSpec = {
        (slideInHorizontally { width -> width / 4 } + fadeIn()) togetherWith
            (slideOutHorizontally { width -> -width / 4 } + fadeOut())
      },
      label = "screen_navigation"
    ) { targetScreen ->
      when (targetScreen) {
        Screen.HOME -> HomeScreen(
          viewModel = viewModel,
          onNavigateToScan = { navigateTo(Screen.CAMERA_SCANNER) },
          onNavigateToImageToPdf = { navigateTo(Screen.IMAGE_TO_PDF) },
          onNavigateToPdfToImages = { navigateTo(Screen.PDF_TO_IMAGES) },
          onNavigateToOcr = { navigateTo(Screen.OCR) },
          onNavigateToLocker = { navigateTo(Screen.PRIVATE_LOCKER) },
          onNavigateToSettings = { navigateTo(Screen.SETTINGS) },
          onNavigateToMultiPage = { navigateTo(Screen.MULTI_PAGE) }
        )

        Screen.CAMERA_SCANNER -> CameraScannerScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() },
          onNavigateToCrop = { navigateTo(Screen.CROP) },
          onNavigateToMultiPage = { navigateTo(Screen.MULTI_PAGE) }
        )

        Screen.CROP -> CropScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() },
          onNavigateToEnhance = { navigateTo(Screen.ENHANCEMENT) }
        )

        Screen.ENHANCEMENT -> EnhancementScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() },
          onNavigateToMultiPage = { navigateTo(Screen.MULTI_PAGE) }
        )

        Screen.MULTI_PAGE -> MultiPageScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() },
          onNavigateToCamera = { navigateTo(Screen.CAMERA_SCANNER) },
          onNavigateToCrop = { navigateTo(Screen.CROP) },
          onNavigateToOcr = { navigateTo(Screen.OCR) },
          onPdfSaved = { navigateToHomeClearingStack() }
        )

        Screen.OCR -> OcrScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() }
        )

        Screen.IMAGE_TO_PDF -> ImageToPdfScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() },
          onPdfSaved = { navigateToHomeClearingStack() }
        )

        Screen.PDF_TO_IMAGES -> PdfToImagesScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() }
        )

        Screen.PRIVATE_LOCKER -> PrivateLockerScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() }
        )

        Screen.SETTINGS -> SettingsScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBack() }
        )
      }
    }
  }
}
