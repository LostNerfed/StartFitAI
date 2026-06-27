package com.example.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseProfile(
    val id: String,
    val username: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("activity_level") val activityLevel: String? = null,
    @SerialName("fitness_goal") val fitnessGoal: String? = null,
    @SerialName("daily_calories_target") val dailyCaloriesTarget: Int? = null,
    @SerialName("daily_protein_target") val dailyProteinTarget: Int? = null,
    @SerialName("daily_carbs_target") val dailyCarbsTarget: Int? = null,
    @SerialName("daily_fat_target") val dailyFatTarget: Int? = null,
    @SerialName("preferred_ai_model") val preferredAiModel: String? = null,
    @SerialName("weight_unit") val weightUnit: String? = null,
    @SerialName("height_unit") val heightUnit: String? = null,
    @SerialName("active_session_id") val activeSessionId: String? = null
)

@Serializable
data class SupabaseRoutine(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String,
    @SerialName("is_generated") val isGenerated: Int
)

@Serializable
data class SupabasePlanExercise(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("routine_id") val routineId: String,
    @SerialName("exercise_name") val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val notes: String? = null,
    @SerialName("order_index") val orderIndex: Int
)

@Serializable
data class SupabaseSession(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("routine_id") val routineId: String? = null,
    @SerialName("routine_name") val routineName: String? = null,
    @SerialName("date_millis") val dateMillis: Long,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("volume_kg") val volumeKg: Double,
    val notes: String? = null
)

@Serializable
data class SupabaseSessionLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("set_index") val setIndex: Int,
    @SerialName("weight_kg") val weightKg: Double,
    val reps: Int,
    @SerialName("is_dropset") val isDropset: Int
)

@Serializable
data class SupabaseHeatmapData(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("date_string") val dateString: String,
    @SerialName("muscle_group") val muscleGroup: String,
    @SerialName("accumulated_volume") val accumulatedVolume: Double
)

@Serializable
data class SupabaseMeal(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String? = null,
    @SerialName("date_string") val dateString: String? = null,
    @SerialName("input_text") val inputText: String? = null,
    @SerialName("total_calories") val totalCalories: Int = 0,
    @SerialName("total_protein") val totalProtein: Double = 0.0,
    @SerialName("total_carbs") val totalCarbs: Double = 0.0,
    @SerialName("total_fat") val totalFat: Double = 0.0
)

@Serializable
data class SupabaseFood(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("meal_id") val mealId: String,
    val name: String,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

@Serializable
data class SupabaseExercise(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String? = null
)

@Serializable
data class SupabaseWeightEntry(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("date_millis") val dateMillis: Long,
    val weight: Double
)

@Serializable
data class SupabaseChatSession(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("date_millis") val dateMillis: Long
)

@Serializable
data class SupabaseChatMessage(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_id") val sessionId: String,
    val role: String,
    val content: String,
    @SerialName("date_millis") val dateMillis: Long
)
