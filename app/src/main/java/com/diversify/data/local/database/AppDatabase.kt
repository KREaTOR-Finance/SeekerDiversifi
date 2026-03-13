package com.diversify.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.diversify.data.local.dao.SessionDao
import com.diversify.data.local.dao.TransactionDao
import com.diversify.data.local.entity.SessionEntity
import com.diversify.data.local.entity.TransactionEntity

@Database(
    entities = [SessionEntity::class, TransactionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun sessionDao(): SessionDao
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diversify_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN walletIndex INTEGER")
                // new session field
                db.execSQL("ALTER TABLE sessions ADD COLUMN fundingAsset TEXT DEFAULT 'SOL' NOT NULL")
            }
        }
    }
}
