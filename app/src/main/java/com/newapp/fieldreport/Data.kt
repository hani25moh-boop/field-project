package com.newapp.fieldreport

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class Project(@PrimaryKey val code: String, val name: String, val nameAr: String)

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val code: String,
    val name: String,
    val nameAr: String,
    val unit: String,
    val category: String
)

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey val id: String,
    val role: String,
    val recordType: String,
    val projectCode: String,
    val itemCode: String,
    val itemName: String,
    val unit: String,
    val quantity: Double?,
    val hours: Double?,
    val status: String,
    val priority: String,
    val personCode: String,
    val personName: String,
    val eventAt: String,
    val notes: String,
    // added in database version 2
    val personTitle: String = "",
    val reportDate: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoRef: String = ""
)

@Dao
interface AppDao {
    @Query("SELECT * FROM projects ORDER BY nameAr") fun projects(): Flow<List<Project>>
    @Query("SELECT * FROM items ORDER BY nameAr") fun items(): Flow<List<Item>>
    @Query("SELECT * FROM entries ORDER BY eventAt DESC") fun entries(): Flow<List<Entry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun projects(v: List<Project>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun items(v: List<Item>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun item(v: Item)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun entry(v: Entry)

    @Delete suspend fun deleteEntry(e: Entry)

    @Query("SELECT COUNT(*) FROM projects") suspend fun projectCount(): Int
    @Query("SELECT COUNT(*) FROM items") suspend fun itemCount(): Int

    @Query("DELETE FROM projects WHERE code = :c AND code NOT IN (SELECT projectCode FROM entries)")
    suspend fun deleteProjectIfUnused(c: String)

    @Query("DELETE FROM entries") suspend fun clearEntries()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN personTitle TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE entries ADD COLUMN reportDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE entries ADD COLUMN latitude REAL")
        db.execSQL("ALTER TABLE entries ADD COLUMN longitude REAL")
        db.execSQL("ALTER TABLE entries ADD COLUMN photoRef TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Project::class, Item::class, Entry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(c: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, AppDatabase::class.java, "fieldreport_v3.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
