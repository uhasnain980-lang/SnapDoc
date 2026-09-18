package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.FolderEntity
import com.example.data.repository.DocRepository
import com.example.scanner.CropQuad
import com.example.scanner.FilterType
import com.example.scanner.ImageProcessingEngine
import com.example.scanner.OcrEngine
import com.example.scanner.OcrResult
import com.example.scanner.PdfEngine
import com.example.scanner.PdfGenerationOptions
import com.example.scanner.PinLockerManager
import com.example.scanner.ScannedPage
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getInstance(application)
  val repository = DocRepository(application, database.docDao())
  val pinLocker = PinLockerManager(application)

  // App Settings
  private val _currentLanguage = MutableStateFlow("en")
  val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

  private val _appThemeMode = MutableStateFlow(AppThemeMode.SYSTEM)
  val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

  // Scanning Session State
  private val _scannedPages = MutableStateFlow<List<ScannedPage>>(emptyList())
  val scannedPages: StateFlow<List<ScannedPage>> = _scannedPages.asStateFlow()

  private val _currentPageIndex = MutableStateFlow(0)
  val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

  private val _selectedDocType = MutableStateFlow("A4")
  val selectedDocType: StateFlow<String> = _selectedDocType.asStateFlow()

  private val _isProcessing = MutableStateFlow(false)
  val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  // OCR state
  private val _extractedText = MutableStateFlow("")
  val extractedText: StateFlow<String> = _extractedText.asStateFlow()

  private val _ocrInProgress = MutableStateFlow(false)
  val ocrInProgress: StateFlow<Boolean> = _ocrInProgress.asStateFlow()

  // Search & Filtering
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedFolderId = MutableStateFlow<Long?>(null)
  val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

  // Database flows
  val allFolders: StateFlow<List<FolderEntity>> = repository.allFolders
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentDocuments: StateFlow<List<DocumentEntity>> = repository.recentDocuments
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val lockedDocuments: StateFlow<List<DocumentEntity>> = repository.lockedDocuments
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val documentsList: StateFlow<List<DocumentEntity>> = combine(
    _searchQuery,
    _selectedFolderId
  ) { query, folderId ->
    Pair(query, folderId)
  }.flatMapLatest { (query, folderId) ->
    if (query.isNotBlank()) {
      repository.searchDocuments(query.trim())
    } else if (folderId != null) {
      repository.getDocumentsByFolder(folderId)
    } else {
      repository.allDocuments
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun setLanguage(lang: String) {
    _currentLanguage.value = lang
  }

  fun setThemeMode(mode: AppThemeMode) {
    _appThemeMode.value = mode
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSelectedFolder(folderId: Long?) {
    _selectedFolderId.value = folderId
  }

  fun setSelectedDocType(type: String) {
    _selectedDocType.value = type
  }

  fun clearMessages() {
    _statusMessage.value = null
    _errorMessage.value = null
  }

  fun clearScanningSession() {
    _scannedPages.value = emptyList()
    _currentPageIndex.value = 0
    _extractedText.value = ""
  }

  // Adding captured bitmap
  fun addCapturedBitmap(bitmap: Bitmap) {
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        val detectedQuad = ImageProcessingEngine.autoDetectDocumentEdges(bitmap)
        val newPage = ScannedPage(
          originalBitmap = bitmap,
          enhancedBitmap = bitmap,
          quad = detectedQuad,
          filterType = FilterType.AUTO
        )
        val enhanced = ImageProcessingEngine.applyFilter(bitmap, FilterType.AUTO)
        newPage.enhancedBitmap = enhanced

        val updatedList = _scannedPages.value.toMutableList().apply { add(newPage) }
        _scannedPages.value = updatedList
        _currentPageIndex.value = updatedList.size - 1
      } catch (e: Exception) {
        _errorMessage.value = "Failed to process image: ${e.localizedMessage}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  fun setCurrentPageIndex(index: Int) {
    if (index in 0 until _scannedPages.value.size) {
      _currentPageIndex.value = index
    }
  }

  fun updatePageCrop(pageIndex: Int, quad: CropQuad) {
    val pages = _scannedPages.value.toMutableList()
    if (pageIndex !in pages.indices) return
    val page = pages[pageIndex]
    page.quad = quad
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        val warped = ImageProcessingEngine.warpPerspective(page.originalBitmap, quad)
        val filtered = ImageProcessingEngine.applyFilter(
          source = warped,
          filterType = page.filterType,
          brightness = page.brightness,
          contrast = page.contrast
        )
        page.enhancedBitmap = filtered
        _scannedPages.value = pages
      } catch (e: Exception) {
        _errorMessage.value = "Crop failed: ${e.localizedMessage}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  fun applyPageFilter(pageIndex: Int, filter: FilterType, brightness: Float, contrast: Float) {
    val pages = _scannedPages.value.toMutableList()
    if (pageIndex !in pages.indices) return
    val page = pages[pageIndex]
    page.filterType = filter
    page.brightness = brightness
    page.contrast = contrast

    viewModelScope.launch {
      _isProcessing.value = true
      try {
        val filtered = ImageProcessingEngine.applyFilter(
          source = page.originalBitmap,
          filterType = filter,
          brightness = brightness,
          contrast = contrast
        )
        page.enhancedBitmap = filtered
        _scannedPages.value = pages
      } catch (e: Exception) {
        _errorMessage.value = "Filter failed: ${e.localizedMessage}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  fun applyFilterToAllPages(filter: FilterType, brightness: Float, contrast: Float) {
    val pages = _scannedPages.value.toMutableList()
    viewModelScope.launch {
      _isProcessing.value = true
      try {
        for (page in pages) {
          page.filterType = filter
          page.brightness = brightness
          page.contrast = contrast
          page.enhancedBitmap = ImageProcessingEngine.applyFilter(
            source = page.originalBitmap,
            filterType = filter,
            brightness = brightness,
            contrast = contrast
          )
        }
        _scannedPages.value = pages
      } catch (e: Exception) {
        _errorMessage.value = "Filter application failed: ${e.localizedMessage}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  fun rotatePage(pageIndex: Int) {
    val pages = _scannedPages.value.toMutableList()
    if (pageIndex !in pages.indices) return
    val page = pages[pageIndex]
    page.rotationDegrees = (page.rotationDegrees + 90f) % 360f

    val rotatedOriginal = ImageProcessingEngine.rotateBitmap(page.originalBitmap, 90f)
    val rotatedEnhanced = ImageProcessingEngine.rotateBitmap(page.enhancedBitmap, 90f)

    pages[pageIndex] = page.copy(
      originalBitmap = rotatedOriginal,
      enhancedBitmap = rotatedEnhanced
    )
    _scannedPages.value = pages
  }

  fun reorderPage(fromIndex: Int, toIndex: Int) {
    val pages = _scannedPages.value.toMutableList()
    if (fromIndex in pages.indices && toIndex in pages.indices) {
      val item = pages.removeAt(fromIndex)
      pages.add(toIndex, item)
      _scannedPages.value = pages
      _currentPageIndex.value = toIndex
    }
  }

  fun deletePage(pageIndex: Int) {
    val pages = _scannedPages.value.toMutableList()
    if (pageIndex in pages.indices) {
      pages.removeAt(pageIndex)
      _scannedPages.value = pages
      if (_currentPageIndex.value >= pages.size) {
        _currentPageIndex.value = (pages.size - 1).coerceAtLeast(0)
      }
    }
  }

  fun addSignatureToPage(
    pageIndex: Int,
    signatureBitmap: Bitmap,
    normalizedX: Float = 0.5f,
    normalizedY: Float = 0.8f,
    scale: Float = 0.35f
  ) {
    val pages = _scannedPages.value.toMutableList()
    if (pageIndex !in pages.indices) return
    val page = pages[pageIndex]
    page.signatureBitmap = signatureBitmap
    page.signatureX = normalizedX
    page.signatureY = normalizedY
    page.signatureScale = scale

    val signed = ImageProcessingEngine.compositeSignature(
      documentBitmap = page.enhancedBitmap,
      signatureBitmap = signatureBitmap,
      normalizedX = normalizedX,
      normalizedY = normalizedY,
      scale = scale
    )
    page.enhancedBitmap = signed
    _scannedPages.value = pages
  }

  fun runOcrOnCurrentPage() {
    val pages = _scannedPages.value
    val idx = _currentPageIndex.value
    if (idx !in pages.indices) {
      _errorMessage.value = "No page available for OCR"
      return
    }
    runOcrOnBitmap(pages[idx].enhancedBitmap)
  }

  fun runOcrOnBitmap(bitmap: Bitmap) {
    viewModelScope.launch {
      _ocrInProgress.value = true
      _statusMessage.value = "Extracting text using on-device OCR…"
      when (val result = OcrEngine.extractText(bitmap)) {
        is OcrResult.Success -> {
          _extractedText.value = result.text
          _statusMessage.value = if (result.text.isEmpty()) "No readable text found." else "Text extracted successfully!"
        }
        is OcrResult.Error -> {
          _errorMessage.value = result.message
        }
      }
      _ocrInProgress.value = false
    }
  }

  fun saveExtractedTextToTxt(context: Context, filename: String, onDone: (File) -> Unit) {
    viewModelScope.launch {
      try {
        val file = OcrEngine.saveAsTxt(context, filename, _extractedText.value)
        _statusMessage.value = "Saved TXT to ${file.name}"
        onDone(file)
      } catch (e: Exception) {
        _errorMessage.value = "Failed to save TXT: ${e.localizedMessage}"
      }
    }
  }

  /**
   * Generates PDF from current session pages and saves to Room database.
   */
  fun createAndSavePdf(
    context: Context,
    filename: String,
    folderId: Long = 1,
    options: PdfGenerationOptions,
    onSuccess: (DocumentEntity) -> Unit
  ) {
    val pages = _scannedPages.value
    if (pages.isEmpty()) {
      _errorMessage.value = "Please scan at least one page."
      return
    }

    viewModelScope.launch {
      _isProcessing.value = true
      _statusMessage.value = "Creating PDF…"
      try {
        val bitmaps = pages.map { it.enhancedBitmap }
        val pdfFile = PdfEngine.createPdfFromBitmaps(context, bitmaps, options)
        val thumbFile = PdfEngine.saveThumbnail(context, bitmaps.first(), filename)

        val docEntity = DocumentEntity(
          title = filename,
          folderId = folderId,
          pageCount = bitmaps.size,
          fileSizeBytes = pdfFile.length(),
          pdfPath = pdfFile.absolutePath,
          thumbnailPath = thumbFile.absolutePath,
          extractedText = _extractedText.value.takeIf { it.isNotBlank() },
          docType = _selectedDocType.value
        )

        val savedId = repository.saveDocument(docEntity)
        val finalDoc = docEntity.copy(id = savedId)

        _statusMessage.value = "PDF created successfully!"
        clearScanningSession()
        onSuccess(finalDoc)
      } catch (e: Exception) {
        _errorMessage.value = "Failed to create PDF: ${e.localizedMessage}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  /**
   * Import multiple images from gallery and convert to PDF.
   */
  fun convertGalleryImagesToPdf(
    context: Context,
    uris: List<Uri>,
    options: PdfGenerationOptions,
    folderId: Long = 1,
    onSuccess: (DocumentEntity) -> Unit
  ) {
    if (uris.isEmpty()) {
      _errorMessage.value = "No images selected."
      return
    }

    viewModelScope.launch {
      _isProcessing.value = true
      _statusMessage.value = "Processing ${uris.size} images…"
      try {
        val bitmaps = withContext(Dispatchers.IO) {
          uris.mapNotNull { uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
              BitmapFactory.decodeStream(stream)
            }
          }
        }

        if (bitmaps.isEmpty()) {
          _errorMessage.value = "Unable to load selected images."
          return@launch
        }

        val pdfFile = PdfEngine.createPdfFromBitmaps(context, bitmaps, options)
        val thumbFile = PdfEngine.saveThumbnail(context, bitmaps.first(), options.filename)

        val docEntity = DocumentEntity(
          title = options.filename,
          folderId = folderId,
          pageCount = bitmaps.size,
          fileSizeBytes = pdfFile.length(),
          pdfPath = pdfFile.absolutePath,
          thumbnailPath = thumbFile.absolutePath,
          docType = "Gallery Import"
        )

        val savedId = repository.saveDocument(docEntity)
        _statusMessage.value = "PDF successfully generated!"
        onSuccess(docEntity.copy(id = savedId))
      } catch (e: Exception) {
        _errorMessage.value = "Image to PDF failed: ${e.localizedMessage}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  /**
   * Export PDF pages as JPG or PNG images.
   */
  fun exportPdfToImages(
    context: Context,
    doc: DocumentEntity,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    onSuccess: (List<File>) -> Unit
  ) {
    viewModelScope.launch {
      _isProcessing.value = true
      _statusMessage.value = "Exporting PDF pages to images…"
      try {
        val pdfFile = File(doc.pdfPath)
        if (!pdfFile.exists()) {
          _errorMessage.value = "PDF file not found."
          return@launch
        }
        val exported = PdfEngine.exportPdfToImages(context, pdfFile, format)
        _statusMessage.value = "Exported ${exported.size} pages to images!"
        onSuccess(exported)
      } catch (e: Exception) {
        _errorMessage.value = "Failed to export images: ${e.localizedMessage}"
      } finally {
        _isProcessing.value = false
      }
    }
  }

  // Document Operations
  fun renameDocument(id: Long, newTitle: String) {
    viewModelScope.launch {
      repository.updateTitle(id, newTitle)
      _statusMessage.value = "Document renamed."
    }
  }

  fun moveDocumentToFolder(id: Long, folderId: Long) {
    viewModelScope.launch {
      repository.moveToFolder(id, folderId)
      _statusMessage.value = "Document moved to folder."
    }
  }

  fun toggleDocumentLock(id: Long, isLocked: Boolean) {
    viewModelScope.launch {
      repository.setLockedStatus(id, isLocked)
      _statusMessage.value = if (isLocked) "Moved to Private Locker." else "Unlocked from Locker."
    }
  }

  fun deleteDocument(doc: DocumentEntity) {
    viewModelScope.launch {
      repository.deleteDocument(doc)
      _statusMessage.value = "Document deleted."
    }
  }

  fun createNewFolder(name: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.createFolder(name.trim())
      _statusMessage.value = "Folder created."
    }
  }
}
