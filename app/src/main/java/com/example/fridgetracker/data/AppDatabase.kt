package com.example.fridgetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fridgetracker.model.Product

@Database(entities = [Product::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        val MIGRATION_1_3: Migration = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN category TEXT")
            }
        }
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN comment TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN price TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN notifyExpiry INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE products ADD COLUMN expiryDaysBefore INTEGER NOT NULL DEFAULT 4")
                database.execSQL("ALTER TABLE products ADD COLUMN notifyAfterOpening INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE products ADD COLUMN afterOpeningDays INTEGER NOT NULL DEFAULT 2")
            }
        }
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fridge_db"
                ).addMigrations(MIGRATION_1_3, MIGRATION_3_4)
                    .build()
                INSTANCE = inst
                inst
            }
    }
}