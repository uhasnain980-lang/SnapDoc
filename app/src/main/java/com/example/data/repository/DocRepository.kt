package com.example.data.repository

import android.content.Context
import com.example.data.dao.DocDao
import com.example.data.model.DocumentEntity
import com.example.data.model.FolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DocRepository(
  private val context: Context,
  private val docDao: DocDao
) {

  val allDocuments: Flow<List<DocumentEntity>> = docDao.getAllUnlockedDocuments()
  val recentDocuments: Flow<List<DocumentEntity>> = docDao.getRecentDocuments(8)
  val lockedDocuments: Flow<List<DocumentEntity>> = docDao.getLockedDocuments()
  val allFolders: Flow<List<FolderEntity>> = docDao.getAllFolders()

  fun getDocumentsByFolder(folderId: Long): Flow<List<DocumentEntity>> =
    docDao.getDocumentsByFolder(folderId)

  fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
    docDao.searchDocuments(query)

  suspend fun getDocumentById(id: Long): DocumentEntity? =
    docDao.getDocumentById(id)

  suspend fun saveDocument(doc: DocumentEntity): Long = withContext(Dispatchers.IO) {
    docDao.insertDocument(doc)
  }

  suspend fun updateTitle(id: Long, title: String) = withContext(Dispatchers.IO) {
    docDao.updateTitle(id, title)
  }

  suspend fun moveToFolder(id: Long, folderId: Long) = withContext(Dispatchers.IO) {
    docDao.moveToFolder(id, folderId)
  }

  suspend fun setLockedStatus(id: Long, isLocked: Boolean) = withContext(Dispatchers.IO) {
    docDao.setLockedStatus(id, isLocked)
  }

  suspend fun updateExtractedText(id: Long, text: String) = withContext(Dispatchers.IO) {
    docDao.updateExtractedText(id, text)
  }

  suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
    // Delete physical files
    if (doc.pdfPath.isNotEmpty()) {
      val pdfFile = File(doc.pdfPath)
      if (pdfFile.exists()) pdfFile.delete()
    }
    if (doc.thumbnailPath.isNotEmpty()) {
      val thumbFile = File(doc.thumbnailPath)
      if (thumbFile.exists()) thumbFile.delete()
    }
    docDao.deleteDocumentById(doc.id)
  }

  suspend fun createFolder(name: String, iconName: String = "folder", colorHex: Long = 0xFF3B82F6): Long =
    withContext(Dispatchers.IO) {
      docDao.insertFolder(FolderEntity(name = name, iconName = iconName, colorHex = colorHex))
    }

  suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
    docDao.deleteFolder(folderId)
  }
}
