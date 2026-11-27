package com.example.fastcash

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fastcash.data.local.AppDatabase
import com.example.fastcash.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TransactionCacheTest {
    @Test fun sameRemoteIdCannotReplaceAnotherReference() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.transactionDao()
            val first = Transaction(id = 1, reference = "TXN-FIRST", type = TransactionType.DEPOSIT,
                amount = 1000, playerId = "TEST0001")
            val cached = dao.cacheByReference(first)
            dao.cacheByReference(first.copy(reference = "TXN-SECOND"))
            assertNull(dao.getByReference("TXN-MISSING"))
            val updated = dao.cacheByReference(first.copy(status = TransactionStatus.PROCESSING))
            assertEquals(cached.id, updated.id)
            assertEquals(TransactionStatus.PROCESSING, dao.getByReference(first.reference)?.status)
            assertEquals(TransactionStatus.PENDING, dao.getByReference("TXN-SECOND")?.status)
        } finally {
            db.close()
        }
    }
}
