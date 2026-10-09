package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.rudra.smartworktracker.data.entity.BillSplit
import kotlinx.coroutines.flow.Flow

@Dao
interface BillSplitDao {
    @Insert
    suspend fun insert(billSplit: BillSplit): Long

    @Update
    suspend fun update(billSplit: BillSplit)

    @Query("SELECT * FROM bill_splits ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BillSplit>>

    @Query("SELECT * FROM bill_splits WHERE id = :id")
    suspend fun getById(id: Long): BillSplit?

    @Query("DELETE FROM bill_splits WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Upsert
    suspend fun upsert(billSplit: BillSplit)
}
