package com.example.fastcash.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fastcash.data.model.AgentAccountEntity
import com.example.fastcash.data.model.AppSettingsEntity
import com.example.fastcash.data.model.NotificationEntity
import com.example.fastcash.data.model.ReconRowEntity
import com.example.fastcash.data.model.Transaction
import com.example.fastcash.data.model.UserProfileEntity

@Database(
    entities = [
        Transaction::class,
        NotificationEntity::class,
        UserProfileEntity::class,
        AppSettingsEntity::class,
        AgentAccountEntity::class,
        ReconRowEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        fun open(context: Context, accountScope: String): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java,
                "fast_cash_customer_$accountScope").build()
    }
}
