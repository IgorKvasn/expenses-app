package com.example.expensetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expensetracker.data.db.dao.CategoryDao
import com.example.expensetracker.data.db.dao.ExpenseDao
import com.example.expensetracker.data.db.dao.IncomeDao
import com.example.expensetracker.data.db.dao.RecurringExpenseDao
import com.example.expensetracker.data.db.dao.RecurringExpenseGenerationDao
import com.example.expensetracker.data.db.dao.RecurringIncomeGenerationDao
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        RecurringExpenseEntity::class,
        RecurringExpenseGenerationEntity::class,
        RecurringIncomeGenerationEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun recurringExpenseGenerationDao(): RecurringExpenseGenerationDao
    abstract fun recurringIncomeGenerationDao(): RecurringIncomeGenerationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }

        fun setInstance(database: AppDatabase) {
            instance = database
        }
    }
}
