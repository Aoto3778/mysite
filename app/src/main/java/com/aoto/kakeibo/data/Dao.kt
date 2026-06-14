package com.aoto.kakeibo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao {
    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TxnEntity>>

    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY timestamp DESC")
    suspend fun getAll(): List<TxnEntity>

    @Insert
    suspend fun insert(txn: TxnEntity): Long

    @Update
    suspend fun update(txn: TxnEntity)

    @Query("UPDATE transactions SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** 同一source・同一金額が指定時間範囲に既にあるか（重複計上防止）。 */
    @Query(
        "SELECT COUNT(*) FROM transactions WHERE deleted = 0 AND source = :source " +
            "AND amount = :amount AND timestamp BETWEEN :from AND :to"
    )
    suspend fun countSimilar(source: String, amount: Long, from: Long, to: Long): Int
}

@Dao
interface RawDao {
    @Insert
    suspend fun insert(raw: RawCapture)

    @Query("SELECT * FROM raw_captures ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<RawCapture>>

    @Query("DELETE FROM raw_captures")
    suspend fun clear()
}
