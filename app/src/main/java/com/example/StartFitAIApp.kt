package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.FitnessRepository

class StartFitAIApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AppDatabase.getInstance(this)
        FitnessRepository.getInstance(this)
    }
}
