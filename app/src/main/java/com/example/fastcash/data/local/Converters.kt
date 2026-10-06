package com.example.fastcash.data.local

import androidx.room.TypeConverter
import com.example.fastcash.data.model.StatementProvider
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.data.model.TransactionType
import com.example.fastcash.data.model.VerificationStatus

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrDefault(TransactionType.DEPOSIT) }

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus? =
        value?.let { runCatching { TransactionStatus.valueOf(it) }.getOrDefault(TransactionStatus.PENDING) }

    @TypeConverter
    fun fromStatementProvider(value: StatementProvider?): String? = value?.name

    @TypeConverter
    fun toStatementProvider(value: String?): StatementProvider? =
        value?.let { runCatching { StatementProvider.valueOf(it) }.getOrDefault(StatementProvider.BOC) }

    @TypeConverter
    fun fromVerificationStatus(value: VerificationStatus?): String? = value?.name

    @TypeConverter
    fun toVerificationStatus(value: String?): VerificationStatus? =
        value?.let { runCatching { VerificationStatus.valueOf(it) }.getOrDefault(VerificationStatus.UNMATCHED) }
}
