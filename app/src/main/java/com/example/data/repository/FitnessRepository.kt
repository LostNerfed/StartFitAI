package com.example.data.repository

import android.content.Context
import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * FitnessRepository — capa de acceso a datos 100% local con Room (SQLite).
 * No hay sync con backend. El user_id es siempre "local_user".
 */
class FitnessRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).appDao()
    private val TAG = "FitnessRepository"

    companion object {
        private const val LOCAL_USER_ID = "local_user"

        @Volatile
        private var INSTANCE: FitnessRepository? = null

        fun getInstance(context: Context? = null): FitnessRepository {
            return INSTANCE ?: synchronized(this) {
                val ctx = context ?: throw IllegalStateException("Context required on first init")
                val instance = FitnessRepository(ctx)
                INSTANCE = instance
                instance
            }
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    val settings: Flow<FitSettings?> = dao.watchSettings().map { entity ->
        entity?.toDomain()
    }

    suspend fun getSettingsSync(): FitSettings {
        return dao.getSettings()?.toDomain() ?: FitSettings()
    }

    suspend fun saveSettings(s: FitSettings) {
        dao.saveSettings(s.toEntity())
    }

    // ─── Heatmap ─────────────────────────────────────────────────────────────

    fun getAllHeatmapData(): Flow<List<HeatmapData>> {
        return dao.getAllHeatmapData().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAllHeatmapDataSync(): List<HeatmapData> {
        return dao.getAllHeatmapData().first().map { it.toDomain() }
    }

    suspend fun insertHeatmapData(data: HeatmapData) {
        dao.insertHeatmapData(data.toEntity())
    }

    suspend fun clearHeatmapData() {
        dao.clearHeatmapData()
    }

    // ─── Routines ─────────────────────────────────────────────────────────────

    val routines: Flow<List<Routine>> = dao.watchRoutines().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllRoutinesSync(): List<Routine> {
        return dao.watchRoutines().first().map { it.toDomain() }
    }

    suspend fun getRoutineById(id: String): Routine? {
        return dao.getRoutineById(id)?.toDomain()
    }

    suspend fun insertRoutine(routine: Routine): String {
        val id = routine.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertRoutine(
            RoutineEntity(
                id = id,
                name = routine.name,
                description = routine.description,
                isGenerated = if (routine.isGenerated) 1 else 0
            )
        )
        return id
    }

    suspend fun deleteRoutine(routine: Routine) {
        dao.deleteRoutine(routine.id)
        dao.deleteExercisesForRoutine(routine.id)
    }

    // ─── Plan Exercises ───────────────────────────────────────────────────────

    fun getExercisesForRoutine(routineId: String): Flow<List<PlanExercise>> {
        return dao.watchExercisesForRoutine(routineId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getExercisesForRoutineSync(routineId: String): List<PlanExercise> {
        return dao.watchExercisesForRoutine(routineId).first().map { it.toDomain() }
    }

    suspend fun getAllPlanExercisesSync(): List<PlanExercise> {
        return dao.watchAllPlanExercises().first().map { it.toDomain() }
    }

    suspend fun insertPlanExercise(exercise: PlanExercise) {
        val id = exercise.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertPlanExercise(
            PlanExerciseEntity(
                id = id,
                routineId = exercise.routineId,
                exerciseName = exercise.exerciseName,
                sets = exercise.targetSets,
                orderIndex = exercise.orderIndex
            )
        )
    }

    suspend fun insertPlanExercises(exercises: List<PlanExercise>) {
        val entities = exercises.map { ex ->
            PlanExerciseEntity(
                id = ex.id.ifEmpty { UUID.randomUUID().toString() },
                routineId = ex.routineId,
                exerciseName = ex.exerciseName,
                sets = ex.targetSets,
                orderIndex = ex.orderIndex
            )
        }
        dao.insertPlanExercises(entities)
    }

    suspend fun deletePlanExercise(exercise: PlanExercise) {
        dao.deletePlanExercise(exercise.id)
    }

    suspend fun deleteExercisesForRoutine(routineId: String) {
        dao.deleteExercisesForRoutine(routineId)
    }

    // ─── Sessions ─────────────────────────────────────────────────────────────

    val sessions: Flow<List<Session>> = dao.watchSessions().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllSessionsSync(): List<Session> {
        return dao.watchSessions().first().map { it.toDomain() }
    }

    suspend fun getSessionById(id: String): Session? {
        return dao.getSessionById(id)?.toDomain()
    }

    suspend fun insertSession(session: Session): String {
        val id = session.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertSession(
            SessionEntity(
                id = id,
                routineId = session.routineId,
                routineName = session.routineName,
                dateMillis = session.dateMillis,
                durationMinutes = session.durationMinutes,
                volumeKg = session.burnedCalories
            )
        )
        return id
    }

    suspend fun deleteSession(session: Session) {
        dao.deleteLogsForSession(session.id)
        dao.deleteSession(session.id)
    }

    // ─── Session Logs ─────────────────────────────────────────────────────────

    fun getLogsForSession(sessionId: String): Flow<List<SessionLog>> {
        return dao.watchLogsForSession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAllSessionLogsSync(): List<SessionLog> {
        return dao.watchAllLogs().first().map { it.toDomain() }
    }

    suspend fun getLogsForSessionSync(sessionId: String): List<SessionLog> {
        return dao.watchLogsForSession(sessionId).first().map { it.toDomain() }
    }

    val allLogs: Flow<List<SessionLog>> = dao.watchAllLogs().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun insertSessionLog(log: SessionLog) {
        val id = log.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertSessionLog(
            SessionLogEntity(
                id = id,
                sessionId = log.sessionId,
                exerciseName = log.exerciseName,
                setIndex = log.setIndex,
                weightKg = log.weightKg,
                reps = log.reps,
                isDropset = if (log.isDropset) 1 else 0
            )
        )
    }

    suspend fun insertSessionLogs(logs: List<SessionLog>) {
        val entities = logs.map { log ->
            SessionLogEntity(
                id = log.id.ifEmpty { UUID.randomUUID().toString() },
                sessionId = log.sessionId,
                exerciseName = log.exerciseName,
                setIndex = log.setIndex,
                weightKg = log.weightKg,
                reps = log.reps,
                isDropset = if (log.isDropset) 1 else 0
            )
        }
        dao.insertSessionLogs(entities)
    }

    suspend fun deleteSessionLog(log: SessionLog) {
        dao.deleteSessionLog(log.id)
    }

    suspend fun getPRForExercise(exerciseName: String): SessionLog? {
        return dao.getPRForExercise(exerciseName)?.toDomain()
    }

    // ─── Meals & Foods ────────────────────────────────────────────────────────

    fun getMealsForDate(dateString: String): Flow<List<Meal>> {
        return dao.watchMealsForDate(dateString).map { list ->
            list.map { it.toDomain() }
        }
    }

    val allMeals: Flow<List<Meal>> = dao.watchAllMeals().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllMealsSync(): List<Meal> {
        return dao.getAllMeals().map { it.toDomain() }
    }

    suspend fun getMealById(id: String): Meal? {
        return dao.getMealById(id)?.toDomain()
    }

    suspend fun insertMeal(meal: Meal): String {
        val id = meal.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertMeal(
            MealEntity(
                id = id,
                type = meal.category,
                dateString = meal.dateString,
                inputText = meal.inputText,
                totalCalories = meal.totalCalories,
                totalProtein = meal.totalProtein,
                totalCarbs = meal.totalCarbs,
                totalFat = meal.totalFat
            )
        )
        return id
    }

    suspend fun deleteMealById(id: String) {
        dao.deleteFoodsForMeal(id)
        dao.deleteMeal(id)
    }

    fun getFoodsForMeal(mealId: String): Flow<List<Food>> {
        return dao.watchFoodsForMeal(mealId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAllFoodsSync(): List<Food> {
        return dao.getAllFoods().map { it.toDomain() }
    }

    suspend fun getFoodsForMealSync(mealId: String): List<Food> {
        return dao.getFoodsForMeal(mealId).map { it.toDomain() }
    }

    suspend fun insertFood(food: Food) {
        val id = food.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertFood(
            FoodEntity(
                id = id,
                mealId = food.mealId,
                name = food.name,
                calories = food.calories,
                protein = food.protein,
                carbs = food.carbs,
                fat = food.fat
            )
        )
    }

    suspend fun insertFoods(foods: List<Food>) {
        val entities = foods.map { food ->
            FoodEntity(
                id = food.id.ifEmpty { UUID.randomUUID().toString() },
                mealId = food.mealId,
                name = food.name,
                calories = food.calories,
                protein = food.protein,
                carbs = food.carbs,
                fat = food.fat
            )
        }
        dao.insertFoods(entities)
    }

    suspend fun deleteFoodById(id: String) {
        dao.deleteFood(id)
    }

    // ─── Exercises ────────────────────────────────────────────────────────────

    val allExercises: Flow<List<Exercise>> = dao.watchAllExercises().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllExercisesSync(): List<Exercise> {
        return dao.getAllExercises().map { it.toDomain() }
    }

    suspend fun insertExercise(exercise: Exercise) {
        if (dao.exerciseExists(exercise.name)) return
        dao.insertExercise(
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = exercise.name,
                muscleGroup = exercise.category
            )
        )
    }

    suspend fun insertExercises(exercises: List<Exercise>) {
        val entities = exercises.mapNotNull { ex ->
            if (dao.exerciseExists(ex.name)) return@mapNotNull null
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = ex.name,
                muscleGroup = ex.category
            )
        }
        if (entities.isNotEmpty()) dao.insertExercises(entities)
    }

    suspend fun getExercisesCount(): Int {
        return try { dao.getExercisesCount() } catch (e: Exception) { 0 }
    }

    suspend fun deleteExercise(exercise: Exercise) {
        dao.deleteExercise(exercise.name)
    }

    suspend fun deduplicateExercises() {
        dao.deduplicateExercises()
    }

    // ─── Weight Entries ───────────────────────────────────────────────────────

    val weightEntries: Flow<List<WeightEntry>> = dao.watchAllWeightEntries().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllWeightEntriesSync(): List<WeightEntry> {
        return dao.getAllWeightEntries().map { it.toDomain() }
    }

    suspend fun insertWeightEntry(entry: WeightEntry) {
        dao.insertWeightEntry(
            WeightEntryEntity(
                id = entry.id,
                dateMillis = entry.dateMillis,
                weight = entry.weight
            )
        )
    }

    suspend fun clearWeightEntries() {
        dao.clearWeightEntries()
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    val chatSessions: Flow<List<ChatSession>> = dao.watchChatSessions().map { list ->
        list.map { it.toDomain() }
    }

    fun getChatMessages(sessionId: String): Flow<List<ChatMessage>> {
        return dao.watchChatMessages(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertChatSession(session: ChatSession): String {
        val id = session.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertChatSession(
            ChatSessionEntity(id = id, title = session.title, dateMillis = session.dateMillis)
        )
        return id
    }

    suspend fun insertChatMessage(message: ChatMessage) {
        val id = message.id.ifEmpty { UUID.randomUUID().toString() }
        dao.insertChatMessage(
            ChatMessageEntity(
                id = id,
                sessionId = message.sessionId,
                role = message.role,
                content = message.content,
                dateMillis = message.dateMillis
            )
        )
    }

    suspend fun getAllChatSessionsSync(): List<ChatSession> {
        return dao.getAllChatSessions().map { it.toDomain() }
    }

    suspend fun getAllChatMessagesSync(): List<ChatMessage> {
        return dao.getAllChatMessages().map { it.toDomain() }
    }

    suspend fun deleteChatSession(sessionId: String) {
        dao.deleteChatMessagesForSession(sessionId)
        dao.deleteChatSession(sessionId)
    }

    suspend fun cleanupOldChats() {
        val cutoffDate = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        dao.deleteOldChatMessages(cutoffDate)
        dao.deleteOldChatSessions(cutoffDate)
        dao.cleanupOrphanMessages()
    }

    // ─── Clear All ────────────────────────────────────────────────────────────

    suspend fun clearAllData() {
        dao.clearSettings()
        dao.clearRoutines()
        dao.clearPlanExercises()
        dao.clearSessions()
        dao.clearSessionLogs()
        dao.clearMeals()
        dao.clearFoods()
        dao.clearExercises()
        dao.clearChatSessions()
        dao.clearChatMessages()
        dao.clearWeightEntries()
    }
}

// ─── MAPPERS: Entity → Domain ─────────────────────────────────────────────────

private fun FitSettingsEntity.toDomain() = FitSettings(
    id = id,
    username = username,
    gender = gender,
    age = age,
    bodyWeight = weightKg,
    heightCm = heightCm,
    activityLevel = activityLevel,
    fitnessGoal = fitnessGoal,
    targetCalories = dailyCaloriesTarget,
    targetProtein = dailyProteinTarget,
    targetCarbs = dailyCarbsTarget,
    targetFat = dailyFatTarget,
    iaProvider = preferredAiModel,
    weightUnit = weightUnit,
    heightUnit = heightUnit,
    activeSessionId = activeSessionId
)

private fun FitSettings.toEntity() = FitSettingsEntity(
    id = "local_user",
    username = username,
    gender = gender,
    age = age,
    weightKg = bodyWeight,
    heightCm = heightCm,
    activityLevel = activityLevel,
    fitnessGoal = fitnessGoal,
    dailyCaloriesTarget = targetCalories,
    dailyProteinTarget = targetProtein,
    dailyCarbsTarget = targetCarbs,
    dailyFatTarget = targetFat,
    preferredAiModel = iaProvider,
    weightUnit = weightUnit,
    heightUnit = heightUnit,
    activeSessionId = activeSessionId,
    updatedAt = System.currentTimeMillis()
)

private fun RoutineEntity.toDomain() = Routine(id = id, name = name, description = description, isGenerated = isGenerated == 1)

private fun PlanExerciseEntity.toDomain() = PlanExercise(
    id = id,
    routineId = routineId,
    exerciseName = exerciseName,
    targetSets = sets,
    orderIndex = orderIndex
)

private fun SessionEntity.toDomain() = Session(
    id = id,
    routineId = routineId,
    routineName = routineName,
    dateMillis = dateMillis,
    durationMinutes = durationMinutes,
    burnedCalories = volumeKg
)

private fun SessionLogEntity.toDomain() = SessionLog(
    id = id,
    sessionId = sessionId,
    exerciseName = exerciseName,
    weightKg = weightKg,
    reps = reps,
    isDropset = isDropset == 1,
    setIndex = setIndex
)

private fun MealEntity.toDomain() = Meal(
    id = id,
    dateString = dateString,
    category = type,
    inputText = inputText,
    totalCalories = totalCalories,
    totalProtein = totalProtein,
    totalCarbs = totalCarbs,
    totalFat = totalFat
)

private fun FoodEntity.toDomain() = Food(
    id = id,
    mealId = mealId,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat
)

private fun ExerciseEntity.toDomain() = Exercise(id = id, name = name, category = muscleGroup)

private fun ChatSessionEntity.toDomain() = ChatSession(id = id, title = title, dateMillis = dateMillis)

private fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    dateMillis = dateMillis
)

private fun WeightEntryEntity.toDomain() = WeightEntry(
    id = id,
    dateMillis = dateMillis,
    weight = weight
)

private fun HeatmapDataEntity.toDomain() = com.example.data.database.HeatmapData(
    id = id,
    userId = userId,
    muscleGroup = muscleGroup,
    accumulatedVolume = accumulatedVolume,
    lastUpdated = updatedAt
)

private fun com.example.data.database.HeatmapData.toEntity() = HeatmapDataEntity(
    id = id,
    userId = userId,
    muscleGroup = muscleGroup,
    accumulatedVolume = accumulatedVolume,
    dateString = "",
    createdAt = System.currentTimeMillis(),
    updatedAt = lastUpdated
)
