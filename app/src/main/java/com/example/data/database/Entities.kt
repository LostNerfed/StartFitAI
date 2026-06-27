package com.example.data.database

import java.io.Serializable

import java.util.UUID

data class FitSettings(
    val id: String = "profile",
    val username: String = "Usuario",
    val iaProvider: String = "Gemini", // "Gemini", "Groq", "DeepSeek"
    val fitnessGoal: String = "Hipertrofia", // "Hipertrofia", "Pérdida de grasa", etc.
    val bodyWeight: Double = 70.0,
    val targetCalories: Int = 2500,
    val targetProtein: Int = 140,
    val targetCarbs: Int = 300,
    val targetFat: Int = 70,
    val activeSessionId: String? = null,
    val gender: String = "Hombre",
    val age: Int = 25,
    val heightCm: Double = 170.0,
    val activityLevel: String = "Sedentario",
    val weightUnit: String = "kg", // "kg" or "lb"
    val heightUnit: String = "cm",  // "cm" or "in"
    val weightHistory: List<WeightEntry> = emptyList()
) : Serializable

data class WeightEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long = System.currentTimeMillis(),
    val weight: Double = 0.0
) : Serializable

data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val isGenerated: Boolean = false
) : Serializable

data class PlanExercise(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String = "",
    val exerciseName: String = "",
    val targetSets: Int = 3,
    val orderIndex: Int = 0
) : Serializable

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String? = null,
    val routineName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0,
    val burnedCalories: Int = 0
) : Serializable

data class SessionLog(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val exerciseName: String = "",
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val isDropset: Boolean = false,
    val setIndex: Int = 0
) : Serializable

data class Meal(
    val id: String = UUID.randomUUID().toString(),
    val dateString: String = "", // "YYYY-MM-DD"
    val category: String = "", // "Desayuno", "Almuerzo", "Cena", "Snack"
    val inputText: String = "", // natural language description
    val totalCalories: Int = 0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0
) : Serializable

data class Food(
    val id: String = UUID.randomUUID().toString(),
    val mealId: String = "", // links to Meal
    val name: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
) : Serializable

data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Otros"
) : Serializable

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Chat",
    val dateMillis: Long = System.currentTimeMillis()
) : Serializable

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val role: String = "user", // "user" or "model"
    val content: String = "",
    val dateMillis: Long = System.currentTimeMillis()
) : Serializable

data class HeatmapData(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "local_user",
    val muscleGroup: String = "",
    val accumulatedVolume: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable
