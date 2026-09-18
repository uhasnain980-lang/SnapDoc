package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DocDao
import com.example.data.model.DocumentEntity
import com.example.data.model.FolderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [DocumentEntity::class, FolderEntity::class],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun docDao(): DocDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "snapdoc_database.db"
        )
          .addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
              CoroutineScope(Dispatchers.IO).launch {
                populateDefaultFolders(getInstance(context).docDao())
              }
            }
          })
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }

    suspend fun populateDefaultFolders(dao: DocDao) {
      if (dao.getFolderCount() == 0) {
        val defaultFolders = listOf(
          FolderEntity(name = "Documents", iconName = "description", colorHex = 0xFF3B82F6),
          FolderEntity(name = "Receipts", iconName = "receipt_long", colorHex = 0xFF10B981),
          FolderEntity(name = "School", iconName = "school", colorHex = 0xFFF59E0B),
          FolderEntity(name = "Work", iconName = "work", colorHex = 0xFF8B5CF6),
          FolderEntity(name = "Personal", iconName = "person", colorHex = 0xFFEC4899),
          FolderEntity(name = "IDs", iconName = "badge", colorHex = 0xFF14B8A6)
        )
        for (folder in defaultFolders) {
          dao.insertFolder(folder)
        }
      }
    }
  }
}
