package com.aoto.kakeibo.data

import kotlinx.coroutines.flow.Flow

/** DAO をまとめた薄いラッパ。サービスと ViewModel の双方から使う。 */
class Repository(private val txnDao: TxnDao, private val rawDao: RawDao) {

    fun observeTxns(): Flow<List<TxnEntity>> = txnDao.observeAll()
    fun observeRaw(): Flow<List<RawCapture>> = rawDao.observeRecent()

    suspend fun getAll(): List<TxnEntity> = txnDao.getAll()
    suspend fun insert(txn: TxnEntity): Long = txnDao.insert(txn)
    suspend fun update(txn: TxnEntity) = txnDao.update(txn)
    suspend fun delete(id: Long) = txnDao.softDelete(id)
    suspend fun countSimilar(source: String, amount: Long, from: Long, to: Long): Int =
        txnDao.countSimilar(source, amount, from, to)

    suspend fun insertRaw(raw: RawCapture) = rawDao.insert(raw)
    suspend fun clearRaw() = rawDao.clear()
}
