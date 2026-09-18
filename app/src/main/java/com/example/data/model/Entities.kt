package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val folderId: Long = 1, // 1 = Documents folder by default
  val pageCount: Int = 1,
  val fileSizeBytes: Long = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val pdfPath: String,
  val thumbnailPath: String = "",
  val extractedText: String? = null,
  val isLocked: Boolean = false,
  val docType: String = "A4"
)

@Entity(tableName = "folders")
data class FolderEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val iconName: String = "folder",
  val colorHex: Long = 0xFF3B82F6
)
