package com.aoto.kakeibo

import android.app.Application
import com.aoto.kakeibo.data.AppDatabase
import com.aoto.kakeibo.data.Repository

class KakeiboApp : Application() {
    val repository: Repository by lazy {
        val db = AppDatabase.get(this)
        Repository(db.txnDao(), db.rawDao())
    }
}
