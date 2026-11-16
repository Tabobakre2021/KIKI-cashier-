package com.example

import android.app.Application
import com.example.data.KikiDatabase
import com.example.data.repository.KikiRepository

class KikiApplication : Application() {
    val database: KikiDatabase by lazy { KikiDatabase.getInstance(this) }
    val repository: KikiRepository by lazy { KikiRepository(this, database) }
}
