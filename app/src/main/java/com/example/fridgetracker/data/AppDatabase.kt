package com.example.fridgetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.model.ShoppingListEntity
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.model.UserProfile

@Database(entities = [Product::class, ShoppingListEntity::class,ShoppingListItemEntity::class, UserProfile::class ],  version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun userProfileDao(): UserProfileDao
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
        val MIGRATION_4_TO_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS `shopping_lists` (
               `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
               `name` TEXT NOT NULL,
               `createdAtEpochMs` INTEGER NOT NULL
            )
        """.trimIndent())
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS `shopping_list_items` (
               `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
               `listId` INTEGER NOT NULL,
               `name` TEXT NOT NULL,
               `quantity` INTEGER NOT NULL,
               `checked` INTEGER NOT NULL,
               `orderIndex` INTEGER NOT NULL,
               FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_list_items_listId` ON `shopping_list_items` (`listId`)")
            }
        }
        val MIGRATION_5_TO_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `firstName` TEXT NOT NULL,
                        `lastName` TEXT NOT NULL,
                        `dateOfBirth` TEXT NOT NULL,
                        `weight` REAL,
                        `gender` TEXT NOT NULL,
                        `isVegan` INTEGER NOT NULL,
                        `isVegetarian` INTEGER NOT NULL,
                        `isPorkFree` INTEGER NOT NULL,
                        `isMeatFree` INTEGER NOT NULL,
                        `isNoBeef` INTEGER NOT NULL,
                        `isGlutenFree` INTEGER NOT NULL,
                        `isNoLactose` INTEGER NOT NULL,
                        `isNoAlcohol` INTEGER NOT NULL,
                        `isNoShellfish` INTEGER NOT NULL,
                        `isNoNuts` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fridge_db"
                ).addMigrations(MIGRATION_1_3, MIGRATION_3_4, MIGRATION_4_TO_5,MIGRATION_5_TO_6)
                    .build()
                INSTANCE = inst
                inst
            }
    }
}