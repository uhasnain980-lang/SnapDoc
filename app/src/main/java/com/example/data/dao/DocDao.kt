package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocumentEntity
import com.example.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocDao {

  @Query("SELECT * FROM documents WHERE isLocked = 0 ORDER BY createdAt DESC")
  fun getAllUnlockedDocuments(): Flow<List<DocumentEntity>>

  @Query("SELECT * FROM documents WHERE isLocked = 0 ORDER BY createdAt DESC LIMIT :limit")
  fun getRecentDocuments(limit: Int = 8): Flow<List<DocumentEntity>>

  @Query("SELECT * FROM documents WHERE folderId = :folderId AND isLocked = 0 ORDER BY createdAt DESC")
  fun getDocumentsByFolder(folderId: Long): Flow<List<DocumentEntity>>

  @Query("SELECT * FROM documents WHERE isLocked = 1 ORDER BY createdAt DESC")
  fun getLockedDocuments(): Flow<List<DocumentEntity>>

  @Query("""
    SELECT * FROM documents 
    WHERE isLocked = 0 
    AND (title LIKE '%' || :query || '%' OR (extractedText IS NOT NULL AND extractedText LIKE '%' || :query || '%'))
    ORDER BY createdAt DESC
  """)
  fun searchDocuments(query: String): Flow<List<DocumentEntity>>

  @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
  suspend fun getDocumentById(id: Long): DocumentEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocument(doc: DocumentEntity): Long

  @Update
  suspend fun updateDocument(doc: DocumentEntity)

  @Query("UPDATE documents SET title = :newTitle WHERE id = :id")
  suspend fun updateTitle(id: Long, newTitle: String)

  @Query("UPDATE documents SET folderId = :folderId WHERE id = :id")
  suspend fun moveToFolder(id: Long, folderId: Long)

  @Query("UPDATE documents SET isLocked = :isLocked WHERE id = :id")
  suspend fun setLockedStatus(id: Long, isLocked: Boolean)

  @Query("UPDATE documents SET extractedText = :text WHERE id = :id")
  suspend fun updateExtractedText(id: Long, text: String)

  @Query("DELETE FROM documents WHERE id = :id")
  suspend fun deleteDocumentById(id: Long)

  // Folders
  @Query("SELECT * FROM folders ORDER BY id ASC")
  fun getAllFolders(): Flow<List<FolderEntity>>

  @Query("SELECT COUNT(*) FROM folders")
  suspend fun getFolderCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolder(folder: FolderEntity): Long

  @Query("DELETE FROM folders WHERE id = :id")
  suspend fun deleteFolder(id: Long)
}
