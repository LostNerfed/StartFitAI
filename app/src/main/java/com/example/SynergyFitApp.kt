package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.FitnessRepository

class SynergyFitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar la base de datos Room local
        AppDatabase.getInstance(this)
        
        // Inicializar el repositorio con contexto de aplicación
        FitnessRepository.getInstance(this)
    }
}
