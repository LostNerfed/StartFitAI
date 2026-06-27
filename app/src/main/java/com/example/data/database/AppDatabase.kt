package com.example.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ─── ROOM ENTITIES ───────────────────────────────────────────────────────────

@Entity(tableName = "fit_settings")
data class FitSettingsEntity(
    @PrimaryKey val id: String = "local_user",
    val username: String = "Usuario",
    val gender: String = "Hombre",
    val age: Int = 25,
    val weightKg: Double = 70.0,
    val heightCm: Double = 170.0,
    val activityLevel: String = "Sedentario (0 entrenamientos/sem)",
    val fitnessGoal: String = "Hipertrofia / Ganar Músculo",
    val dailyCaloriesTarget: Int = 2500,
    val dailyProteinTarget: Int = 140,
    val dailyCarbsTarget: Int = 300,
    val dailyFatTarget: Int = 70,
    val preferredAiModel: String = "Gemini",
    val weightUnit: String = "kg",
    val heightUnit: String = "cm",
    val activeSessionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val name: String = "",
    val description: String = "",
    val isGenerated: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plan_exercises")
data class PlanExerciseEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val routineId: String = "",
    val exerciseName: String = "",
    val sets: Int = 3,
    val reps: Int = 0,
    val notes: String = "",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val routineId: String? = null,
    val routineName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0,
    val volumeKg: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val sessionId: String = "",
    val exerciseName: String = "",
    val setIndex: Int = 0,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val isDropset: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val type: String = "",
    val dateString: String = "",
    val inputText: String = "",
    val totalCalories: Int = 0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val mealId: String = "",
    val name: String = "",
    val amount: Double = 1.0,
    val unit: String = "g",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val name: String = "",
    val muscleGroup: String = "Otros",
    val equipment: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val dateMillis: Long = System.currentTimeMillis(),
    val title: String = "Chat",
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val sessionId: String = "",
    val role: String = "user",
    val content: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val dateMillis: Long = System.currentTimeMillis(),
    val weight: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "heatmap_data")
data class HeatmapDataEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val muscleGroup: String = "",
    val accumulatedVolume: Double = 0.0,
    val dateString: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ─── DAO ─────────────────────────────────────────────────────────────────────

@Dao
interface AppDao {

    // ── Settings ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM fit_settings WHERE id = 'local_user' LIMIT 1")
    fun watchSettings(): Flow<FitSettingsEntity?>

    @Query("SELECT * FROM fit_settings WHERE id = 'local_user' LIMIT 1")
    suspend fun getSettings(): FitSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(entity: FitSettingsEntity)

    // ── Routines ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun watchRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: String): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(entity: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: String)

    // ── Plan Exercises ────────────────────────────────────────────────────────
    @Query("SELECT * FROM plan_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun watchExercisesForRoutine(routineId: String): Flow<List<PlanExerciseEntity>>

    @Query("SELECT * FROM plan_exercises")
    fun watchAllPlanExercises(): Flow<List<PlanExerciseEntity>>

    @Query("SELECT * FROM plan_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getExercisesForRoutine(routineId: String): List<PlanExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercise(entity: PlanExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercises(entities: List<PlanExerciseEntity>)

    @Query("DELETE FROM plan_exercises WHERE id = :id")
    suspend fun deletePlanExercise(id: String)

    @Query("DELETE FROM plan_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: String)

    // ── Sessions ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM sessions ORDER BY dateMillis DESC")
    fun watchSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(entity: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    // ── Session Logs ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM session_logs WHERE sessionId = :sessionId ORDER BY setIndex ASC")
    fun watchLogsForSession(sessionId: String): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs WHERE sessionId = :sessionId ORDER BY setIndex ASC")
    suspend fun getLogsForSession(sessionId: String): List<SessionLogEntity>

    @Query("SELECT * FROM session_logs ORDER BY createdAt DESC")
    fun watchAllLogs(): Flow<List<SessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(entity: SessionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLogs(entities: List<SessionLogEntity>)

    @Query("DELETE FROM session_logs WHERE id = :id")
    suspend fun deleteSessionLog(id: String)

    @Query("DELETE FROM session_logs WHERE sessionId = :sessionId")
    suspend fun deleteLogsForSession(sessionId: String)

    @Query("SELECT * FROM session_logs WHERE exerciseName = :exerciseName ORDER BY weightKg DESC LIMIT 1")
    suspend fun getPRForExercise(exerciseName: String): SessionLogEntity?

    // ── Meals ─────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM meals WHERE dateString = :dateString")
    fun watchMealsForDate(dateString: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals")
    fun watchAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    suspend fun getMealById(id: String): MealEntity?

    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(entity: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: String)

    // ── Foods ─────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM foods WHERE mealId = :mealId")
    fun watchFoodsForMeal(mealId: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE mealId = :mealId")
    suspend fun getFoodsForMeal(mealId: String): List<FoodEntity>

    @Query("SELECT * FROM foods")
    suspend fun getAllFoods(): List<FoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(entity: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(entities: List<FoodEntity>)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun deleteFood(id: String)

    @Query("DELETE FROM foods WHERE mealId = :mealId")
    suspend fun deleteFoodsForMeal(mealId: String)

    // ── Exercises ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun watchAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExercisesCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(entity: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(entities: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE name = :name")
    suspend fun deleteExercise(name: String)

    // ── Chat Sessions ─────────────────────────────────────────────────────────
    @Query("SELECT * FROM chat_sessions ORDER BY dateMillis DESC")
    fun watchChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY dateMillis DESC")
    suspend fun getAllChatSessions(): List<ChatSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(entity: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteChatSession(id: String)

    // ── Chat Messages ─────────────────────────────────────────────────────────
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY dateMillis ASC")
    fun watchChatMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY dateMillis ASC")
    suspend fun getAllChatMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteChatMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_sessions WHERE dateMillis < :cutoffMillis")
    suspend fun deleteOldChatSessions(cutoffMillis: Long)

    @Query("DELETE FROM chat_messages WHERE dateMillis < :cutoffMillis")
    suspend fun deleteOldChatMessages(cutoffMillis: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId NOT IN (SELECT id FROM chat_sessions)")
    suspend fun cleanupOrphanMessages()

    // ── Clear All ─────────────────────────────────────────────────────────────
    @Query("DELETE FROM fit_settings")
    suspend fun clearSettings()

    @Query("DELETE FROM routines")
    suspend fun clearRoutines()

    @Query("DELETE FROM plan_exercises")
    suspend fun clearPlanExercises()

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM session_logs")
    suspend fun clearSessionLogs()

    @Query("DELETE FROM meals")
    suspend fun clearMeals()

    @Query("DELETE FROM foods")
    suspend fun clearFoods()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM chat_sessions")
    suspend fun clearChatSessions()

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeatmapData(heatmapData: HeatmapDataEntity)

    @Query("SELECT * FROM heatmap_data WHERE userId = :userId")
    fun getAllHeatmapData(userId: String = "local_user"): Flow<List<HeatmapDataEntity>>

    @Query("SELECT * FROM heatmap_data WHERE userId = :userId AND muscleGroup = :muscleGroup")
    suspend fun getHeatmapDataForMuscle(muscleGroup: String, userId: String = "local_user"): HeatmapDataEntity?

    @Query("DELETE FROM heatmap_data")
    suspend fun clearHeatmapData()

    // ── Weight Entries ─────────────────────────────────────────────────────────
    @Query("SELECT * FROM weight_entries ORDER BY dateMillis DESC")
    fun watchAllWeightEntries(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY dateMillis DESC")
    suspend fun getAllWeightEntries(): List<WeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entity: WeightEntryEntity)

    @Query("DELETE FROM weight_entries")
    suspend fun clearWeightEntries()
}

// ─── DATABASE ─────────────────────────────────────────────────────────────────

@Database(
    entities = [
        FitSettingsEntity::class,
        RoutineEntity::class,
        PlanExerciseEntity::class,
        SessionEntity::class,
        SessionLogEntity::class,
        MealEntity::class,
        FoodEntity::class,
        ExerciseEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        HeatmapDataEntity::class,
        WeightEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE meals ADD COLUMN inputText TEXT NOT NULL DEFAULT ''")
        }

        private val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS weight_entries (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL DEFAULT 'local_user',
                    dateMillis INTEGER NOT NULL,
                    weight REAL NOT NULL DEFAULT 0.0,
                    createdAt INTEGER NOT NULL
                )
            """)
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "synergyfit_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
