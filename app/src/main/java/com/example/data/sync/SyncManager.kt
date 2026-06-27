package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.SupabaseClientProvider
import com.example.data.database.*
import com.example.data.repository.FitnessRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(private val repository: FitnessRepository, private val context: Context) {

    val client = SupabaseClientProvider.client

    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    private fun getCachedSessionId(): String? {
        return prefs.getString("anonymous_session_id", null)
    }

    private fun cacheSessionId(sessionId: String) {
        prefs.edit().putString("anonymous_session_id", sessionId).apply()
    }

    fun clearCachedSession() {
        prefs.edit().remove("anonymous_session_id").apply()
    }

    private suspend fun syncTable(
        table: String,
        userId: String,
        items: List<Any>,
        maxRetries: Int = 2
    ) {
        for (attempt in 1..maxRetries) {
            try {
                client.postgrest[table].delete { filter { eq("user_id", userId) } }
                if (items.isNotEmpty()) {
                    client.postgrest[table].upsert(items)
                }
                return
            } catch (e: Exception) {
                if (attempt == maxRetries) throw e
            }
        }
    }

    suspend fun syncUp(): Boolean = withContext(Dispatchers.IO) {
        try {
            var session = client.auth.currentSessionOrNull()

            if (session == null) {
                val cachedId = getCachedSessionId()
                if (cachedId != null) {
                    try {
                        client.auth.signInAnonymously()
                        session = client.auth.currentSessionOrNull()
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.e("SyncManager", "Error restoring anonymous session", e)
                    }
                }
            }

            if (session == null) {
                try {
                    client.auth.signInAnonymously()
                    session = client.auth.currentSessionOrNull()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e("SyncManager", "Error en signInAnonymously", e)
                }
            }

            val userId = session?.user?.id ?: return@withContext false
            if (getCachedSessionId() == null) {
                cacheSessionId(userId)
            }

            val settings = repository.getSettingsSync()
            val profile = SupabaseProfile(
                id = userId,
                username = settings.username,
                gender = settings.gender,
                age = settings.age,
                weightKg = settings.bodyWeight,
                heightCm = settings.heightCm,
                activityLevel = settings.activityLevel,
                fitnessGoal = settings.fitnessGoal,
                dailyCaloriesTarget = settings.targetCalories,
                dailyProteinTarget = settings.targetProtein,
                dailyCarbsTarget = settings.targetCarbs,
                dailyFatTarget = settings.targetFat,
                preferredAiModel = settings.iaProvider,
                weightUnit = settings.weightUnit,
                heightUnit = settings.heightUnit,
                activeSessionId = settings.activeSessionId
            )
            client.postgrest["profiles"].upsert(profile)

            val routines = repository.getAllRoutinesSync().map {
                SupabaseRoutine(id = it.id, userId = userId, name = it.name, description = it.description, isGenerated = if (it.isGenerated) 1 else 0)
            }
            syncTable("routines", userId, routines)

            val planExercises = repository.getAllPlanExercisesSync().map {
                SupabasePlanExercise(id = it.id, userId = userId, routineId = it.routineId, exerciseName = it.exerciseName, sets = it.targetSets, reps = 0, orderIndex = it.orderIndex)
            }
            syncTable("plan_exercises", userId, planExercises)

            val sessionsList = repository.getAllSessionsSync().map {
                SupabaseSession(id = it.id, userId = userId, routineId = it.routineId, routineName = it.routineName, dateMillis = it.dateMillis, durationMinutes = it.durationMinutes, volumeKg = it.burnedCalories.toDouble())
            }
            syncTable("sessions", userId, sessionsList)

            val sessionLogs = repository.getAllSessionLogsSync().map {
                SupabaseSessionLog(id = it.id, userId = userId, sessionId = it.sessionId, exerciseName = it.exerciseName, setIndex = it.setIndex, weightKg = it.weightKg, reps = it.reps, isDropset = if (it.isDropset) 1 else 0)
            }
            syncTable("session_logs", userId, sessionLogs)

            val heatmapData = repository.getAllHeatmapDataSync().map {
                SupabaseHeatmapData(id = it.id, userId = userId, dateString = it.lastUpdated.toString(), muscleGroup = it.muscleGroup, accumulatedVolume = it.accumulatedVolume)
            }
            syncTable("heatmap_data", userId, heatmapData)

            val meals = repository.getAllMealsSync().map {
                SupabaseMeal(id = it.id, userId = userId, type = it.category, dateString = it.dateString, inputText = it.inputText, totalCalories = it.totalCalories, totalProtein = it.totalProtein, totalCarbs = it.totalCarbs, totalFat = it.totalFat)
            }
            syncTable("meals", userId, meals)

            val allFoods = repository.getAllFoodsSync().map {
                SupabaseFood(id = it.id, userId = userId, mealId = it.mealId, name = it.name, calories = it.calories, protein = it.protein, carbs = it.carbs, fat = it.fat)
            }
            syncTable("foods", userId, allFoods)

            val exercises = repository.getAllExercisesSync().map {
                SupabaseExercise(id = it.id, userId = userId, name = it.name, muscleGroup = it.category)
            }
            syncTable("exercises", userId, exercises)

            val weightEntries = repository.getAllWeightEntriesSync().map {
                SupabaseWeightEntry(id = it.id, userId = userId, dateMillis = it.dateMillis, weight = it.weight)
            }
            syncTable("weight_entries", userId, weightEntries)

            repository.cleanupOldChats()

            val chatSessions = repository.getAllChatSessionsSync().map {
                SupabaseChatSession(id = it.id, userId = userId, title = it.title, dateMillis = it.dateMillis)
            }
            syncTable("chat_sessions", userId, chatSessions)

            val chatMessages = repository.getAllChatMessagesSync().map {
                SupabaseChatMessage(id = it.id, userId = userId, sessionId = it.sessionId, role = it.role, content = it.content, dateMillis = it.dateMillis)
            }
            syncTable("chat_messages", userId, chatMessages)

            true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("SyncManager", "Error en syncUp", e)
            false
        }
    }

    suspend fun syncDownProfile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = client.auth.currentSessionOrNull()
            val userId = session?.user?.id ?: return@withContext false

            val profile = client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<SupabaseProfile>()

            if (profile != null && !profile.username.isNullOrEmpty()) {
                val currentSettings = repository.getSettingsSync()
                val updatedSettings = currentSettings.copy(
                    username = profile.username ?: currentSettings.username,
                    gender = profile.gender ?: currentSettings.gender,
                    age = profile.age ?: currentSettings.age,
                    bodyWeight = profile.weightKg ?: currentSettings.bodyWeight,
                    heightCm = profile.heightCm ?: currentSettings.heightCm,
                    activityLevel = profile.activityLevel ?: currentSettings.activityLevel,
                    fitnessGoal = profile.fitnessGoal ?: currentSettings.fitnessGoal,
                    targetCalories = profile.dailyCaloriesTarget ?: currentSettings.targetCalories,
                    targetProtein = profile.dailyProteinTarget ?: currentSettings.targetProtein,
                    targetCarbs = profile.dailyCarbsTarget ?: currentSettings.targetCarbs,
                    targetFat = profile.dailyFatTarget ?: currentSettings.targetFat,
                    activeSessionId = profile.activeSessionId
                )
                repository.saveSettings(updatedSettings)
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("SyncManager", "Error en syncDownProfile", e)
            false
        }
    }

    suspend fun syncDownAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = client.auth.currentSessionOrNull()
            val userId = session?.user?.id ?: return@withContext false

            val routineEntities = client.postgrest["routines"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseRoutine>()
            for (r in routineEntities) {
                repository.insertRoutine(
                    Routine(id = r.id, name = r.name, description = r.description, isGenerated = r.isGenerated == 1)
                )
            }

            val planExerciseEntities = client.postgrest["plan_exercises"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabasePlanExercise>()
            if (planExerciseEntities.isNotEmpty()) {
                repository.insertPlanExercises(planExerciseEntities.map {
                    PlanExercise(id = it.id, routineId = it.routineId, exerciseName = it.exerciseName, targetSets = it.sets, orderIndex = it.orderIndex)
                })
            }

            val sessionEntities = client.postgrest["sessions"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseSession>()
            for (s in sessionEntities) {
                repository.insertSession(
                    Session(id = s.id, routineId = s.routineId, routineName = s.routineName ?: "", dateMillis = s.dateMillis, durationMinutes = s.durationMinutes, burnedCalories = s.volumeKg.toInt())
                )
            }

            val logEntities = client.postgrest["session_logs"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseSessionLog>()
            if (logEntities.isNotEmpty()) {
                repository.insertSessionLogs(logEntities.map {
                    SessionLog(id = it.id, sessionId = it.sessionId, exerciseName = it.exerciseName, setIndex = it.setIndex, weightKg = it.weightKg, reps = it.reps, isDropset = it.isDropset == 1)
                })
            }

            val heatmapEntities = client.postgrest["heatmap_data"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseHeatmapData>()
            for (h in heatmapEntities) {
                repository.insertHeatmapData(
                    HeatmapData(id = h.id, userId = userId, muscleGroup = h.muscleGroup, accumulatedVolume = h.accumulatedVolume, lastUpdated = h.dateString.toLongOrNull() ?: System.currentTimeMillis())
                )
            }

            val mealEntities = client.postgrest["meals"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseMeal>()
            for (m in mealEntities) {
                repository.insertMeal(
                    Meal(id = m.id, dateString = m.dateString ?: "", category = m.type ?: "", inputText = m.inputText ?: "", totalCalories = m.totalCalories, totalProtein = m.totalProtein, totalCarbs = m.totalCarbs, totalFat = m.totalFat)
                )
            }

            val foodEntities = client.postgrest["foods"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseFood>()
            if (foodEntities.isNotEmpty()) {
                repository.insertFoods(foodEntities.map {
                    Food(id = it.id, mealId = it.mealId, name = it.name, calories = it.calories, protein = it.protein, carbs = it.carbs, fat = it.fat)
                })
            }

            val exerciseEntities = client.postgrest["exercises"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseExercise>()
            for (e in exerciseEntities) {
                repository.insertExercise(
                    Exercise(id = e.id, name = e.name, category = e.muscleGroup ?: "Otros")
                )
            }

            val weightEntryEntities = client.postgrest["weight_entries"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseWeightEntry>()
            for (w in weightEntryEntities) {
                repository.insertWeightEntry(
                    WeightEntry(id = w.id, dateMillis = w.dateMillis, weight = w.weight)
                )
            }

            val chatSessionEntities = client.postgrest["chat_sessions"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseChatSession>()
            for (cs in chatSessionEntities) {
                repository.insertChatSession(
                    ChatSession(id = cs.id, title = cs.title, dateMillis = cs.dateMillis)
                )
            }

            val chatMessageEntities = client.postgrest["chat_messages"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SupabaseChatMessage>()
            for (cm in chatMessageEntities) {
                repository.insertChatMessage(
                    ChatMessage(id = cm.id, sessionId = cm.sessionId, role = cm.role, content = cm.content, dateMillis = cm.dateMillis)
                )
            }

            syncDownProfile()
            true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("SyncManager", "Error en syncDownAll", e)
            false
        }
    }
}
