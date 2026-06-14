package com.aoto.kakeibo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TxnEntity::class, RawCapture::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txnDao(): TxnDao
    abstract fun rawDao(): RawDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // v1→v2: 収入フラグを追加（既存記録は支出=0として保持）。
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isIncome INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kakeibo.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
