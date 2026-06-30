package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AIService
import com.example.data.database.*
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.data.sync.SyncManager
import java.text.SimpleDateFormat
import java.util.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _toastMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val toastMessage: kotlinx.coroutines.flow.SharedFlow<String> = _toastMessage

    private val repository = FitnessRepository.getInstance(app.applicationContext)
    private val syncManager = SyncManager(repository, app.applicationContext)
    private val dbWriteMutex = Mutex()

    private fun autoSync() {
        viewModelScope.launch {
            try {
                syncManager.syncUp()
            } catch (_: Exception) { }
        }
    }

    val heatmapData: StateFlow<List<HeatmapData>> = repository.getAllHeatmapData().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weightEntries: StateFlow<List<WeightEntry>> = repository.weightEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val settingsState: StateFlow<FitSettings> = repository.settings
        .map { it ?: FitSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FitSettings())

    // Chat FIFO History
    private val _activeChatSessionId = MutableStateFlow<String?>(null)
    val activeChatSessionId: StateFlow<String?> = _activeChatSessionId.asStateFlow()

    val chatSessions: StateFlow<List<ChatSession>> = repository.chatSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessage>> = _activeChatSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList()) else repository.getChatMessages(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()


    // Nutrition State
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Observed Meals
    val selectedDateMeals: StateFlow<List<Meal>> = _selectedDate
        .flatMapLatest { date -> repository.getMealsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly summary of calories
    val weeklyCaloriesState: StateFlow<Map<String, Int>> = repository.allMeals
        .map { meals ->
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val resultMap = mutableMapOf<String, Int>()

            // Initialize last 7 days with 0 calories
            for (i in 0 until 7) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = format.format(cal.time)
                resultMap[dateStr] = 0
            }

            meals.forEach { meal ->
                if (resultMap.containsKey(meal.dateString)) {
                    resultMap[meal.dateString] = (resultMap[meal.dateString] ?: 0) + meal.totalCalories
                }
            }
            resultMap
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Observed Foods map by mealId
    private val _mealFoods = MutableStateFlow<Map<String, List<Food>>>(emptyMap())
    val mealFoods: StateFlow<Map<String, List<Food>>> = _mealFoods.asStateFlow()

    // Active Workout Screen State
    private val _activeSession = MutableStateFlow<Session?>(null)
    val activeSession: StateFlow<Session?> = _activeSession.asStateFlow()

    private val _activeLogs = MutableStateFlow<List<SessionLog>>(emptyList())
    val activeLogs: StateFlow<List<SessionLog>> = _activeLogs.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()


    private var cronometerJob: Job? = null

    // Routines & Plans
    val routines: StateFlow<List<Routine>> = repository.routines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sessions & Historical Logs for Analytics & Calendar
    val sessions: StateFlow<List<Session>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<SessionLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeExercisePRs: StateFlow<Map<String, SessionLog>> = kotlinx.coroutines.flow.combine(_activeLogs, allLogs) { active, all ->
        val map = mutableMapOf<String, SessionLog>()
        val exerciseNames = active.map { it.exerciseName }.distinct()
        exerciseNames.forEach { name ->
            val prLog = all.filter { it.exerciseName.equals(name, ignoreCase = true) }
                .maxWithOrNull(compareBy<com.example.data.database.SessionLog> { it.weightKg }.thenBy { it.reps })
            if (prLog != null && prLog.weightKg > 0) {
                map[name] = prLog
            }
        }
        map
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyMap())


    // Global Exercises registry state
    val allExercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Daily Nutrition Insight
    private val _dailyInsight = MutableStateFlow<String>("")
    val dailyInsight: StateFlow<String> = _dailyInsight.asStateFlow()

    private val _dailyInsightLoading = MutableStateFlow(false)
    val dailyInsightLoading: StateFlow<Boolean> = _dailyInsightLoading.asStateFlow()

    // Discover Carousel Cards
    private val _discoverCards = MutableStateFlow<List<com.example.ui.components.DiscoverCardModel>>(emptyList())
    val discoverCards: StateFlow<List<com.example.ui.components.DiscoverCardModel>> = _discoverCards.asStateFlow()

    private val _suggestedRoutines = MutableStateFlow<List<String>>(emptyList())
    val suggestedRoutines: StateFlow<List<String>> = _suggestedRoutines.asStateFlow()

    // Calculated Maintenance Calories (TDEE)
    val maintenanceCalories: StateFlow<Int> = settingsState.map { settings ->
        val weightKg = if (settings.weightUnit == "lb") settings.bodyWeight / 2.20462 else settings.bodyWeight
        val heightCm = if (settings.heightUnit == "in") settings.heightCm * 2.54 else settings.heightCm
        val age = settings.age
        val gender = settings.gender

        var bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age)
        if (gender.lowercase().startsWith("hombre")) {
            bmr += 5
        } else {
            bmr -= 161
        }

        val pal = when {
            settings.activityLevel.contains("Ligero", ignoreCase = true) -> 1.375
            settings.activityLevel.contains("Moderado", ignoreCase = true) -> 1.55
            settings.activityLevel.contains("Muy Activo", ignoreCase = true) -> 1.9
            settings.activityLevel.contains("Activo", ignoreCase = true) -> 1.725
            else -> 1.2
        }

        (bmr * pal).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    // Auth screen profile/local loading
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Local backup management
    private val _localBackupsList = MutableStateFlow<List<String>>(emptyList())
    val localBackupsList: StateFlow<List<String>> = _localBackupsList.asStateFlow()

    // Unit Preference (KG/LBS)

    init {
        // Build initial settings record if none exists
        viewModelScope.launch {
            val s = repository.getSettingsSync() ?: FitSettings()
            // If they already entered username, skip auth screen or proceed
            _isUserLoggedIn.value = s.username != "Usuario" && s.username.trim().isNotEmpty()
            _isInitializing.value = false

            // Delete any existing default preloaded exercises from the database
            val existingEx = repository.getAllExercisesSync()
            val targetsToDelete = setOf("ejercicio", "ejercicio de ejemplo", "press de banca", "dominadas", "press de hombro")
            existingEx.forEach { ex ->
                if (targetsToDelete.contains(ex.name.lowercase().trim())) {
                    repository.deleteExercise(ex)
                }
            }
            repository.deduplicateExercises()

            // Restore/Auto-save memory recovery: check if a workout remained open/unfinished
            val activeId = s.activeSessionId
            if (activeId != null) {
                val session = repository.getSessionById(activeId)
                if (session != null) {
                    _activeSession.value = session
                    _activeLogs.value = repository.getLogsForSessionSync(activeId)
                    // Estimate elapsed seconds dynamically with a sane 2-hour cap
                    val elapsed = ((System.currentTimeMillis() - session.dateMillis) / 1000).toInt().coerceIn(0, 7200)
                    _elapsedSeconds.value = elapsed
                    startCronometer()
                } else {
                    repository.saveSettings(s.copy(activeSessionId = null))
                }
            }
        }

        // Keep meals and foods updated
        viewModelScope.launch {
            selectedDateMeals.collect { meals ->
                val foodMap = mutableMapOf<String, List<Food>>()
                meals.forEach { meal ->
                    val foods = repository.getFoodsForMealSync(meal.id)
                    foodMap[meal.id] = foods
                }
                _mealFoods.value = foodMap
            }
        }

        // Automatic local backup on the 1st of each month
        checkAndPerformMonthlyBackup()
        loadLocalBackups()

        // Cleanup old chats
        viewModelScope.launch { repository.cleanupOldChats() }

        // Generate Daily Discover Cards
        generateDailyDiscoverCards()
    }

    // Auth and settings helper
    fun toggleWeightUnit() {
        viewModelScope.launch {
            val current = settingsState.value
            val newUnit = if (current.weightUnit == "kg") "lb" else "kg"
            repository.saveSettings(current.copy(weightUnit = newUnit))
        }
    }

    fun loginLocalUser(name: String, weightUnit: String, heightUnit: String, gender: String, age: Int, heightCm: Double, activityLevel: String, weightKg: Double, targetCalories: Int, fitnessGoal: String) {
        viewModelScope.launch {
            val current = settingsState.value
            val updated = current.copy(
                username = name,
                gender = gender,
                age = age,
                heightCm = heightCm,
                activityLevel = activityLevel,
                bodyWeight = weightKg,
                targetCalories = targetCalories,
                fitnessGoal = fitnessGoal,
                weightUnit = weightUnit,
                heightUnit = heightUnit
            )
            repository.saveSettings(updated)
            
            _isUserLoggedIn.value = true
        }
    }

    // Weight Conversion Utils
    fun getDisplayWeight(weightKg: Double): Double {
        val isLb = settingsState.value.weightUnit == "lb"
        return if (isLb) weightKg * 2.20462 else weightKg
    }

    fun getStorageWeight(displayWeight: Double): Double {
        val isLb = settingsState.value.weightUnit == "lb"
        return if (isLb) displayWeight / 2.20462 else displayWeight
    }

    fun getUnitString(): String {
        val isLb = settingsState.value.weightUnit == "lb"
        return if (isLb) "LBS" else "KG"
    }


    fun calculate1RM(weightKg: Double, reps: Int): Double {
        if (reps <= 1) return weightKg
        if (reps >= 37) return weightKg * (1 + (reps / 30.0)) // Fallback to Epley
        return weightKg * (36.0 / (37.0 - reps))
    }

    fun formatDisplayWeight(weightKg: Double): String {
        if (weightKg <= 0.0) return ""
        val d = getDisplayWeight(weightKg)
        val s = String.format(java.util.Locale.US, "%.1f", d)
        return if (s.endsWith(".0")) s.substringBefore(".") else s
    }

    fun resetPassword(email: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        // Funcionalidad de contraseña no aplica en modo local
        viewModelScope.launch {
            withContext(Dispatchers.Main) { onError("No aplicable en modo local") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                com.example.data.remote.AuthRepository().signOut()
            } catch (e: Exception) {
                // Ignore any sign-out errors to ensure local logout still completes
            }
            _activeChatSessionId.value = null
            _activeSession.value = null
            _activeLogs.value = emptyList()
            _elapsedSeconds.value = 0
            _isUserLoggedIn.value = false
            syncManager.clearCachedSession()
        }
    }

    fun updateSettings(settings: FitSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            autoSync()
        }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            val entry = WeightEntry(weight = weightKg)
            repository.insertWeightEntry(entry)
            repository.saveSettings(settingsState.value.copy(bodyWeight = weightKg))
            autoSync()
        }
    }

    fun clearWeightHistory() {
        viewModelScope.launch {
            repository.clearWeightEntries()
            autoSync()
        }
    }

    // Date navigation
    fun selectDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun selectDateOffset(days: Int) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = format.parse(_selectedDate.value) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, days)
            _selectedDate.value = format.format(cal.time)
        } catch (e: Exception) {
            _selectedDate.value = getTodayDateString()
        }
    }

    // Room operations
    fun getExercisesForRoutine(routineId: String): Flow<List<PlanExercise>> {
        return repository.getExercisesForRoutine(routineId)
    }

    fun addRoutine(name: String, description: String, exercisesList: List<String>) {
        viewModelScope.launch {
            val routineId = repository.insertRoutine(Routine(name = name, description = description))
            val planExercises = exercisesList.mapIndexed { idx, exName ->
                val capitalized = exName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                // Store in global registry
                repository.insertExercise(Exercise(name = capitalized, category = "Otros"))
                PlanExercise(routineId = routineId, exerciseName = capitalized, targetSets = 3, orderIndex = idx)
            }
            repository.insertPlanExercises(planExercises)
            autoSync()
        }
    }

    fun addExerciseToRoutine(routineId: String, name: String, targetSets: Int, category: String = "Otros") {
        viewModelScope.launch {
            val capitalized = name.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            // Store/ensure exist in global registry – ignore duplicates
            try {
                repository.insertExercise(Exercise(name = capitalized, category = category))
            } catch (e: Exception) {
                // Duplicate exercise ignored
            }

            val existing = repository.getExercisesForRoutineSync(routineId)
            val newIdx = existing.size
            repository.insertPlanExercise(
                PlanExercise(routineId = routineId, exerciseName = capitalized, targetSets = targetSets, orderIndex = newIdx)
            )
            autoSync()
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
            autoSync()
        }
    }

    fun removeExerciseFromRoutine(exercise: PlanExercise) {
        viewModelScope.launch {
            repository.deletePlanExercise(exercise)
            autoSync()
        }
    }

    fun addCustomExercise(name: String, category: String) {
        val capitalized = name.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        viewModelScope.launch {
            repository.insertExercise(Exercise(name = capitalized, category = category))
            autoSync()
        }
    }

    fun deleteCustomExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
            autoSync()
        }
    }

    // Nutrition Meal Details Parsing via Gemini
    private val _mealAnalysisLoading = MutableStateFlow(false)
    val mealAnalysisLoading: StateFlow<Boolean> = _mealAnalysisLoading.asStateFlow()

    fun logMealFromNaturalLanguage(category: String, inputText: String, dateStr: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _mealAnalysisLoading.value = true
            try {
                val config = settingsState.value
                val result = AIService.analyzeMeal(inputText)
                if (result != null) {
                    val mealId = repository.insertMeal(
                        Meal(
                            dateString = dateStr,
                            category = category,
                            inputText = inputText,
                            totalCalories = result.calories,
                            totalProtein = result.protein,
                            totalCarbs = result.carbs,
                            totalFat = result.fat
                        )
                    )

                    val foods = result.foods.map {
                        Food(
                            mealId = mealId,
                            name = it.name,
                            calories = it.calories,
                            protein = it.protein,
                            carbs = it.carbs,
                            fat = it.fat
                        )
                    }
                    repository.insertFoods(foods)

                    // Refresh food list for this date
                    val currentMap = _mealFoods.value.toMutableMap()
                    currentMap[mealId] = foods
                    _mealFoods.value = currentMap

                    generateNutritionInsight()
                    onDone(true)
                } else {
                    // Fail gracefully by inserting a draft meal so the user can see *something* if Gemini fails
                    val mockMealId = repository.insertMeal(
                        Meal(
                            dateString = dateStr,
                            category = category,
                            inputText = inputText,
                            totalCalories = 280,
                            totalProtein = 15.0,
                            totalCarbs = 25.0,
                            totalFat = 8.0
                        )
                    )
                    // We don't insert any generic food item so the UI remains clean
                    generateNutritionInsight()
                    onDone(true)
                }
                autoSync()
            } catch (e: Exception) {
                onDone(false)
            } finally {
                _mealAnalysisLoading.value = false
            }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            repository.deleteMealById(mealId)
            generateNutritionInsight()
            autoSync()
        }
    }

    // Daily Insight Generation
    fun generateNutritionInsight() {
        viewModelScope.launch {
            _dailyInsightLoading.value = true
            try {
                val config = settingsState.value
                val meals = selectedDateMeals.value
                val totalCalories = meals.sumOf { it.totalCalories }
                val totalProtein = meals.sumOf { it.totalProtein }

                val prompt = """
                    Actúa como un Nutricionista deportivo de StartFit AI.
                    El usuario tiene el siguiente objetivo fit: "${config.fitnessGoal}".
                    Hoy ha consumido: $totalCalories / ${config.targetCalories} kcal y $totalProtein / ${config.targetProtein}g de proteínas.
                    Su peso corporal es ${config.bodyWeight} kg.
                    Proporciónale un consejo nutricional útil y claro basándote en los macros de su comida de hoy y su objetivo. 
                    Sé profesional, motivador y usa un tono natural y empático. Si ayuda, puedes usar puntos cortos. Responde en español.
                """.trimIndent()

                val insight = AIService.generateResponse(prompt = prompt)
                _dailyInsight.value = insight
            } catch (e: Exception) {
                _dailyInsight.value = "Mantente bien hidratado para mejorar la recuperación muscular."
            } finally {
                _dailyInsightLoading.value = false
            }
        }
    }

    // Daily Discover Cards Generation (AI Cached)
    fun generateDailyDiscoverCards() {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val todayStr = getTodayDateString()
            val cards = mutableListOf<com.example.ui.components.DiscoverCardModel>()
            
            val allSessions = repository.getAllSessionsSync()
            val lastSession = allSessions.maxByOrNull { it.dateMillis }

            // 1. PR Check (Only if broke a mark in the last session)
            if (lastSession != null) {
                val lastSessionLogs = repository.getLogsForSessionSync(lastSession.id)
                val allLogsList = repository.getAllLogsSync()
                
                var brokenPRLog: com.example.data.database.SessionLog? = null
                
                for (log in lastSessionLogs) {
                    if (log.weightKg > 0) {
                        val allExLogs = allLogsList.filter { it.exerciseName.equals(log.exerciseName, ignoreCase = true) }
                        val previousMax = allExLogs.filter { it.sessionId != lastSession.id }.maxOfOrNull { it.weightKg } ?: 0.0
                        if (log.weightKg > previousMax && previousMax > 0.0) {
                            brokenPRLog = log
                            break
                        }
                    }
                }
                
                if (brokenPRLog != null) {
                    cards.add(
                        com.example.ui.components.DiscoverCardModel(
                            id = "pr_${brokenPRLog.id}",
                            title = "¡Récord Personal!",
                            description = "Rompiste tu marca: ${brokenPRLog.weightKg}kg en ${brokenPRLog.exerciseName}",
                            tag = "Logro",
                            type = com.example.ui.components.DiscoverCardType.PR
                        )
                    )
                }
            }

            // 2. 2-Day AI Fitness Tip (Replacing "Primeros Pasos")
            val nowMillis = System.currentTimeMillis()
            val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L
            
            if (nowMillis - s.lastTipFetchTimestamp > twoDaysInMillis || s.cachedPrimerosPasosTip.isEmpty()) {
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "consejo_fitness",
                        title = "Cargando Consejo...",
                        description = "Generando tu consejo fitness de IA...",
                        tag = "Sugerencia",
                        type = com.example.ui.components.DiscoverCardType.FITNESS_TIP
                    )
                )
                
                // Launch separate fetch for the 2-day tip
                viewModelScope.launch {
                    try {
                        val tipPrompt = "Actúa como Coach de StartFit AI. Dame un consejo general de fitness útil, motivacional y claro para un usuario cuyo objetivo es ${s.fitnessGoal}. Responde en máximo 10 palabras. Sé muy breve y directo."
                        val tipResponse = com.example.data.api.AIService.generateResponse(tipPrompt).trim()
                        
                        val updatedS = repository.getSettingsSync().copy(
                            cachedPrimerosPasosTip = tipResponse,
                            lastTipFetchTimestamp = System.currentTimeMillis()
                        )
                        repository.saveSettings(updatedS)
                        
                        val currentCards = _discoverCards.value.toMutableList()
                        val tipIdx = currentCards.indexOfFirst { it.id == "consejo_fitness" }
                        if (tipIdx != -1) {
                            currentCards[tipIdx] = currentCards[tipIdx].copy(title = "Consejo Fitness", description = tipResponse)
                            _discoverCards.value = currentCards
                        }
                    } catch (e: Exception) {
                        // Fallback to cache if fails
                        if (s.cachedPrimerosPasosTip.isNotEmpty()) {
                            val currentCards = _discoverCards.value.toMutableList()
                            val tipIdx = currentCards.indexOfFirst { it.id == "consejo_fitness" }
                            if (tipIdx != -1) {
                                currentCards[tipIdx] = currentCards[tipIdx].copy(title = "Consejo Fitness", description = s.cachedPrimerosPasosTip)
                                _discoverCards.value = currentCards
                            }
                        }
                    }
                }
            } else {
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "consejo_fitness",
                        title = "Consejo Fitness",
                        description = s.cachedPrimerosPasosTip,
                        tag = "Sugerencia",
                        type = com.example.ui.components.DiscoverCardType.FITNESS_TIP
                    )
                )
            }

            // 3. Daily AI Content (Challenge & Tip)
            if (s.lastDiscoverDate == todayStr && s.cachedDiscoverChallenge.isNotEmpty() && s.cachedDiscoverTip.isNotEmpty()) {
                // Use Cache
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "challenge_daily",
                        title = "Reto de Hoy",
                        description = s.cachedDiscoverChallenge,
                        tag = "Desafío",
                        type = com.example.ui.components.DiscoverCardType.CHALLENGE
                    )
                )
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "tip_daily",
                        title = "Consejo Nutricional",
                        description = s.cachedDiscoverTip,
                        tag = "Nutrición",
                        type = com.example.ui.components.DiscoverCardType.TIP
                    )
                )
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "routine_daily",
                        title = "Rutinas de la Semana",
                        description = "Toca para ver tus 3 rutinas sugeridas por la IA.",
                        tag = "Entrenamiento",
                        type = com.example.ui.components.DiscoverCardType.ROUTINE
                    )
                )
                _discoverCards.value = cards
            } else {
                // Ask AI (Silent Background Request)
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "challenge_daily",
                        title = "Cargando Reto...",
                        description = "Generando tu desafío personalizado del día.",
                        tag = "Desafío",
                        type = com.example.ui.components.DiscoverCardType.CHALLENGE
                    )
                )
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "tip_daily",
                        title = "Cargando Consejo...",
                        description = "Analizando tu perfil para el mejor tip.",
                        tag = "Nutrición",
                        type = com.example.ui.components.DiscoverCardType.TIP
                    )
                )
                cards.add(
                    com.example.ui.components.DiscoverCardModel(
                        id = "routine_daily",
                        title = "Rutinas de la Semana",
                        description = "Toca para ver tus 3 rutinas sugeridas por la IA.",
                        tag = "Entrenamiento",
                        type = com.example.ui.components.DiscoverCardType.ROUTINE
                    )
                )
                _discoverCards.value = cards

                try {
                    val prompt = """
                        Actúa como Coach de StartFit AI. El usuario pesa ${s.bodyWeight}kg, objetivo: ${s.fitnessGoal}. 
                        Genera recomendaciones generales:
                        1. Un reto físico general y corto para hoy.
                        2. Un consejo nutricional general y corto.
                        Regla estricta: Cada uno no debe pasar de 10 palabras.
                        Formato estricto (no uses markdown, solo las 2 líneas):
                        RETO=tu reto aquí
                        TIP=tu tip aquí
                    """.trimIndent()

                    val response = com.example.data.api.AIService.generateResponse(prompt)
                    val challengeMatch = Regex("RETO=(.*)").find(response)?.groupValues?.get(1)?.trim() ?: "Mantente activo por 30 minutos."
                    val tipMatch = Regex("TIP=(.*)").find(response)?.groupValues?.get(1)?.trim() ?: "Bebe agua constantemente."

                    val updatedSettings = repository.getSettingsSync().copy(
                        lastDiscoverDate = todayStr,
                        cachedDiscoverChallenge = challengeMatch,
                        cachedDiscoverTip = tipMatch
                    )
                    repository.saveSettings(updatedSettings)

                    val finalCards = _discoverCards.value.toMutableList()
                    val cIdx = finalCards.indexOfFirst { it.id == "challenge_daily" }
                    if (cIdx != -1) finalCards[cIdx] = finalCards[cIdx].copy(title = "Reto de Hoy", description = challengeMatch)
                    val tIdx = finalCards.indexOfFirst { it.id == "tip_daily" }
                    if (tIdx != -1) finalCards[tIdx] = finalCards[tIdx].copy(title = "Consejo Nutricional", description = tipMatch)

                    _discoverCards.value = finalCards
                } catch (e: Exception) {
                    // Fail silently, generic cards remain
                }
            }
        }
    }

    fun checkAndFetchSuggestedRoutines() {
        viewModelScope.launch {
            repository.settings.firstOrNull()?.let { s ->
                val now = System.currentTimeMillis()
                val twoDaysInMillis = 172_800_000L

                if (now - s.lastRoutineFetchTimestamp > twoDaysInMillis || s.cachedSuggestedRoutines.isEmpty()) {
                    // Time to fetch new routines
                    try {
                        val prompt = """
                            Actúa como un Coach experto de StartFit AI. El usuario pesa ${s.bodyWeight}kg y su objetivo es: ${s.fitnessGoal}.
                            REQUISITO ESTRICTO: Las 3 rutinas sugeridas deben ser las MEJORES a nivel de HIPERTROFIA, con base científica (ej. volumen adecuado, cercanía al fallo, ejercicios clave).
                            Genera 3 rutinas sugeridas cortas. Por ejemplo: 1 de Pecho, 1 de Espalda, 1 de Brazo o variaciones según el objetivo.
                            Usa formato estricto (3 líneas):
                            RUTINA1=tu rutina 1
                            RUTINA2=tu rutina 2
                            RUTINA3=tu rutina 3
                        """.trimIndent()

                        val response = com.example.data.api.AIService.generateResponse(prompt)
                        val r1 = Regex("RUTINA1=(.*)").find(response)?.groupValues?.get(1)?.trim() ?: "Pecho: 4x10 Press de Banca"
                        val r2 = Regex("RUTINA2=(.*)").find(response)?.groupValues?.get(1)?.trim() ?: "Espalda: 4x10 Dominadas"
                        val r3 = Regex("RUTINA3=(.*)").find(response)?.groupValues?.get(1)?.trim() ?: "Brazos: 3x12 Curl de Bíceps"

                        val combinedStr = "$r1|$r2|$r3"

                        repository.saveSettings(
                            s.copy(
                                lastRoutineFetchTimestamp = now,
                                cachedSuggestedRoutines = combinedStr
                            )
                        )

                        _suggestedRoutines.value = listOf(r1, r2, r3)

                    } catch (e: Exception) {
                        if (s.cachedSuggestedRoutines.isNotEmpty()) {
                            _suggestedRoutines.value = s.cachedSuggestedRoutines.split("|")
                        } else {
                            _suggestedRoutines.value = emptyList()
                        }
                    }
                } else {
                    // Use cache
                    if (s.cachedSuggestedRoutines.isNotEmpty()) {
                        _suggestedRoutines.value = s.cachedSuggestedRoutines.split("|")
                    }
                }
            }
        }
    }


    // AI Chat History Management
    fun createNewChat() {
        _activeChatSessionId.value = null
    }

    fun loadChatSession(sessionId: String) {
        _activeChatSessionId.value = sessionId
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_activeChatSessionId.value == sessionId) {
                _activeChatSessionId.value = null
            }
        }
    }

    fun askCoach(question: String, thinkingText: String = "Pensando...") {
        if (question.trim().isEmpty()) return
        val config = settingsState.value

        viewModelScope.launch {
            _chatLoading.value = true

            // Ensure we have an active session
            if (_activeChatSessionId.value == null) {
                val session = ChatSession(title = question.take(30) + if (question.length > 30) "..." else "")
                _activeChatSessionId.value = repository.insertChatSession(session)
            }
            val sessionId = _activeChatSessionId.value!!

            try {
                // Save user message
                val userMsg = ChatMessage(sessionId = sessionId, role = "user", content = question)
                repository.insertChatMessage(userMsg)

                // Build history context
                val historyContext = chatMessages.value.takeLast(10).joinToString("\n") {
                    val roleName = if (it.role == "user") "Usuario" else "Coach"
                    "$roleName: ${it.content}"
                }

                val prompt = """
                    Eres el Coach Deportivo de StartFit AI. ERES EXTREMADAMENTE CONCISO Y DIRECTO. No uses texto de relleno ni preámbulos gigantes.
                    Objetivo principal del usuario: ${config.fitnessGoal}
                    Peso corporal: ${config.bodyWeight} kg
                    
                    Conversación histórica reciente:
                    $historyContext
                    
                    Usuario pregunta: "$question"
                    Responde directamente a la pregunta en no más de 1-2 párrafos cortos. No te extiendas en explicaciones a menos que sea estrictamente necesario.
                """.trimIndent()

                val response = AIService.generateResponse(prompt = prompt)

                val coachMsg = ChatMessage(sessionId = sessionId, role = "model", content = response)
                repository.insertChatMessage(coachMsg)

            } catch (e: Exception) {
                val errorMsg = ChatMessage(sessionId = sessionId, role = "model", content = "El coach no pudo conectarse. Revisa tu red o los ajustes.")
                repository.insertChatMessage(errorMsg)
            } finally {
                _chatLoading.value = false
            }
        }
    }

    // Active Workout Operations
    fun startActiveWorkout(routine: Routine) {
        viewModelScope.launch {
            // Cancel any running job
            cronometerJob?.cancel()
            _elapsedSeconds.value = 0

            val dateMillis = System.currentTimeMillis()
            val newSession = Session(
                routineId = routine.id,
                routineName = routine.name,
                dateMillis = dateMillis,
                durationMinutes = 0
            )

            // We do a temporary session ID (doesn't save to Room yet until we click Save, or we can save an active session in Room)
            // Let's create it in Room directly as a draft!
            val sessionId = repository.insertSession(newSession)
            val currentSession = newSession.copy(id = sessionId)
            _activeSession.value = currentSession

            // Save activeSessionId to settings for auto-saving / persistence
            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(activeSessionId = sessionId))

            // Pre-populate with exercises in that routine
            val exercises = repository.getExercisesForRoutineSync(routine.id)
            val logs = ArrayList<SessionLog>()
            exercises.forEach { ex ->
                // Add 3 default set logs for easy typing
                for (s in 1..ex.targetSets) {
                    logs.add(
                        SessionLog(
                            sessionId = sessionId,
                            exerciseName = ex.exerciseName,
                            weightKg = 0.0,
                            reps = 0,
                            isDropset = false,
                            setIndex = s
                        )
                    )
                }
            }
            // If the routine is empty, add one mock log so there's an editable field
            if (logs.isEmpty()) {
                logs.add(SessionLog(sessionId = sessionId, exerciseName = "Ejercicio", weightKg = 0.0, reps = 0, isDropset = false, setIndex = 1))
            }

            repository.insertSessionLogs(logs)
            refreshActiveLogs()
            autoSync()

            // Start clock
            startCronometer()
        }
    }

    fun startCustomActiveWorkout() {
        viewModelScope.launch {
            cronometerJob?.cancel()
            _elapsedSeconds.value = 0

            val newSession = Session(
                routineId = null,
                routineName = "Rutinas Rápidas",
                dateMillis = System.currentTimeMillis(),
                durationMinutes = 0
            )

            val sessionId = repository.insertSession(newSession)
            val currentSession = newSession.copy(id = sessionId)
            _activeSession.value = currentSession

            // Save activeSessionId to settings for auto-saving / persistence
            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(activeSessionId = sessionId))

            // Pre-populate with a single general log
            val initialLog = SessionLog(sessionId = sessionId, exerciseName = "Ejercicio", weightKg = 0.0, reps = 0, isDropset = false, setIndex = 1)
            repository.insertSessionLog(initialLog)
            refreshActiveLogs()
            autoSync()

            startCronometer()
        }
    }


    // Rest Timer
    private val _restTimerSeconds = kotlinx.coroutines.flow.MutableStateFlow(0)
    val restTimerSeconds: kotlinx.coroutines.flow.StateFlow<Int> = _restTimerSeconds.asStateFlow()
    private var restTimerJob: kotlinx.coroutines.Job? = null
    private var lastRestDuration = 120
    private val _completedSets = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    val completedSets: kotlinx.coroutines.flow.StateFlow<Set<String>> = _completedSets.asStateFlow()

    fun toggleSetComplete(setId: String, context: android.content.Context? = null) {
        val current = _completedSets.value.toMutableSet()
        if (current.contains(setId)) {
            current.remove(setId)
            _completedSets.value = current
        } else {
            current.add(setId)
            _completedSets.value = current
            startRestTimer(lastRestDuration, context)
        }
    }

    fun startRestTimer(seconds: Int, context: android.content.Context? = null) {
        restTimerJob?.cancel()
        _restTimerSeconds.value = seconds
        lastRestDuration = seconds
        restTimerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                _restTimerSeconds.value -= 1
            }
            // Finished
            context?.let { ctx ->
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val vibrator = ctx.getSystemService(android.os.Vibrator::class.java)
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        ctx.getSystemService(android.os.Vibrator::class.java)?.vibrate(1000)
                    }
                } catch(e: Exception) {}
            }
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = _restTimerSeconds.value
        _restTimerSeconds.value = (current + deltaSeconds).coerceAtLeast(0)
        if (_restTimerSeconds.value == 0) restTimerJob?.cancel()
    }

    fun cancelRestTimer() {
        restTimerJob?.cancel()
        _restTimerSeconds.value = 0
    }

    private fun startCronometer() {
        cronometerJob = viewModelScope.launch {
            val startMillis = System.currentTimeMillis() - (_elapsedSeconds.value * 1000L)
            while (true) {
                _elapsedSeconds.value = ((System.currentTimeMillis() - startMillis) / 1000).toInt()
                delay(1000)
            }
        }
    }



    fun addActiveSet(exerciseName: String) {
        val current = _activeSession.value ?: return
        val currentLogs = _activeLogs.value.toMutableList()
        val logs = currentLogs.filter { it.exerciseName == exerciseName }
        val maxIndex = logs.maxOfOrNull { it.setIndex } ?: 0
        val nextIndex = maxIndex + 1
        
        val newLog = SessionLog(
            sessionId = current.id,
            exerciseName = exerciseName,
            weightKg = if (logs.isNotEmpty()) logs.last().weightKg else 0.0,
            reps = if (logs.isNotEmpty()) logs.last().reps else 0,
            isDropset = false,
            setIndex = nextIndex
        )
        
        // Optimistic update
        currentLogs.add(newLog)
        _activeLogs.value = currentLogs
        
        viewModelScope.launch {
            try {
                repository.insertSessionLog(newLog)
                refreshActiveLogs()
            } catch (e: Exception) {
                // DB write failed, UI already updated optimistically
            }
        }
    }

    fun addActiveDropset(exerciseName: String, parentIndex: Int) {
        val current = _activeSession.value ?: return
        val currentLogs = _activeLogs.value.toMutableList()
        val parentLog = currentLogs.firstOrNull { it.exerciseName == exerciseName && it.setIndex == parentIndex }
        
        val newLog = SessionLog(
            sessionId = current.id,
            exerciseName = exerciseName,
            weightKg = if (parentLog != null) parentLog.weightKg * 0.75 else 0.0,
            reps = if (parentLog != null) parentLog.reps else 0,
            isDropset = true,
            setIndex = parentIndex
        )
        
        // Optimistic update
        currentLogs.add(newLog)
        _activeLogs.value = currentLogs
        
        viewModelScope.launch {
            try {
                repository.insertSessionLog(newLog)
                refreshActiveLogs()
            } catch (e: Exception) {
                // DB write failed, UI already updated optimistically
            }
        }
    }

    fun updateActiveSetWeight(id: String, weightKg: Double) {
        // Fast UI update - immediate and synchronous on the Main thread to keep the keyboard ultra responsive
        val currentLogs = _activeLogs.value.toMutableList()
        val index = currentLogs.indexOfFirst { it.id == id }
        if (index != -1) {
            val updatedLog = currentLogs[index].copy(weightKg = weightKg)
            currentLogs[index] = updatedLog
            _activeLogs.value = currentLogs

            // Safeguarded DB write using Mutex and Dispatchers.IO to prevent locking contention and race conditions
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dbWriteMutex.withLock {
                        repository.insertSessionLog(updatedLog)
                    }
                } catch (e: Exception) {
                    // DB write failed, UI already updated optimistically
                }
            }
        }
    }

    fun updateActiveSetReps(id: String, reps: Int) {
        // Fast UI update - immediate and synchronous on the Main thread to keep the keyboard ultra responsive
        val currentLogs = _activeLogs.value.toMutableList()
        val index = currentLogs.indexOfFirst { it.id == id }
        if (index != -1) {
            val updatedLog = currentLogs[index].copy(reps = reps)
            currentLogs[index] = updatedLog
            _activeLogs.value = currentLogs

            // Safeguarded DB write using Mutex and Dispatchers.IO to prevent locking contention and race conditions
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dbWriteMutex.withLock {
                        repository.insertSessionLog(updatedLog)
                    }
                } catch (e: Exception) {
                    // DB write failed, UI already updated optimistically
                }
            }
        }
    }

    fun renameActiveExercise(oldName: String, newName: String) {
        if (newName.trim().isEmpty() || oldName == newName) return
        val currentLogs = _activeLogs.value.toMutableList()
        var updatedAny = false
        val capitalized = newName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        for (i in currentLogs.indices) {
            if (currentLogs[i].exerciseName == oldName) {
                currentLogs[i] = currentLogs[i].copy(exerciseName = capitalized)
                updatedAny = true
            }
        }
        if (updatedAny) {
            _activeLogs.value = currentLogs
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dbWriteMutex.withLock {
                        val targets = currentLogs.filter { it.exerciseName == capitalized }
                        repository.insertSessionLogs(targets)
                    }
                } catch (e: Exception) {
                    // DB write failed
                }
            }
        }
    }

    fun deleteActiveSetLog(id: String) {
        val currentLogs = _activeLogs.value.toMutableList()
        val logToDelete = currentLogs.firstOrNull { it.id == id }
        if (logToDelete != null) {
            currentLogs.remove(logToDelete)
            _activeLogs.value = currentLogs
            
            viewModelScope.launch {
                try {
                    repository.deleteSessionLog(logToDelete)
                    refreshActiveLogs()
                } catch (e: Exception) {
                    // DB write failed
                }
            }
        }
    }

    fun addExerciseToActiveWorkout(exerciseName: String, category: String = "Otros") {
        val current = _activeSession.value ?: return
        viewModelScope.launch {
            try {
                val capitalized = exerciseName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val newLog = SessionLog(
                    sessionId = current.id,
                    exerciseName = capitalized,
                    weightKg = 0.0,
                    reps = 0,
                    isDropset = false,
                    setIndex = 1
                )
                repository.insertSessionLog(newLog)
                refreshActiveLogs()

                // Save to global exercises registry – ignore duplicates
                try {
                    repository.insertExercise(Exercise(name = capitalized, category = category))
            } catch (e: Exception) {
                // Duplicate exercise ignored
            }
        } catch (e: Exception) {
            // DB write failed
        }
        }
    }

    private suspend fun refreshActiveLogs() {
        val current = _activeSession.value ?: return
        _activeLogs.value = repository.getLogsForSessionSync(current.id)
    }

    fun finishActiveWorkout(backdatedOffsetDays: Int, onDone: () -> Unit) {
        val current = _activeSession.value ?: return
        cronometerJob?.cancel()

        viewModelScope.launch {
            try {
                // Apply dates
                var finalDate = System.currentTimeMillis()
                if (backdatedOffsetDays > 0) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -backdatedOffsetDays)
                    finalDate = cal.timeInMillis
                }

                // Filter out totally empty sets to avoid logging garbage
                val validLogs = _activeLogs.value.filter { it.weightKg > 0 && it.reps > 0 }
                
                // Delete old session draft if empty completely
                if (validLogs.isEmpty()) {
                    repository.deleteSession(current)
                } else {
                    // Keep only valid logs
                    val durationMin = (_elapsedSeconds.value / 60).coerceAtLeast(1)
                    
                    // Clear existing logs in DB and re-insert valid ones
                    repository.deleteSession(current) // this cleans everything
                    val newSessionId = repository.insertSession(
                        current.copy(
                            id = "",
                            dateMillis = finalDate,
                            durationMinutes = durationMin
                        )
                    )

                    val completedLogs = validLogs.map { 
                        it.copy(id = "", sessionId = newSessionId)
                    }
                    repository.insertSessionLogs(completedLogs)

                    // Call AI to estimate calories ONLY to avoid heavy quota usage
                    viewModelScope.launch {
                        try {
                            val totalVolume = completedLogs.sumOf { it.weightKg * it.reps }
                            val uniqueExercisesCount = completedLogs.distinctBy { it.exerciseName }.size
                            val weight = repository.getSettingsSync().bodyWeight
                            
                            val prompt = """
                                Actúa como analista fitness. 
                                Un usuario de $weight kg entrenó $durationMin minutos, $uniqueExercisesCount ejercicios, volumen total $totalVolume kg. Estima las calorías quemadas.
                                
                                Devuelve tu respuesta ESTRICTAMENTE en este formato:
                                CALORIAS=$"{número}"
                            """.trimIndent()
                            
                            val response = com.example.data.api.AIService.generateResponse(prompt).trim()
                            
                            // Parse Calories
                            val calMatch = Regex("CALORIAS=(\\d+)").find(response)
                            val calories = calMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                            if (calories > 0) {
                                val session = repository.getSessionById(newSessionId)
                                if (session != null) {
                                    repository.insertSession(session.copy(burnedCalories = calories))
                                }
                            }
                        } catch(e: Throwable) {
                            // AI calories estimation failed, continue without it
                        }
                    }
                    
                    recalculateHeatmap()
                }

                // Complete in ViewModel
                _activeSession.value = null
                _activeLogs.value = emptyList()
                _elapsedSeconds.value = 0

                // Clear activeSessionId from settings for auto-saving / persistence
                val s = repository.getSettingsSync()
                repository.saveSettings(s.copy(activeSessionId = null))
            } catch (e: Exception) {
                // Error finishing workout
            } finally {
                autoSync()
                onDone()
            }
        }
    }

    fun discardActiveWorkout() {
        val current = _activeSession.value ?: return
        cronometerJob?.cancel()
        viewModelScope.launch {
            try {
                repository.deleteSession(current)
                _activeSession.value = null
                _activeLogs.value = emptyList()
                _elapsedSeconds.value = 0

                // Clear activeSessionId from settings for auto-saving / persistence
                val s = repository.getSettingsSync()
                repository.saveSettings(s.copy(activeSessionId = null))
                autoSync()
            } catch (e: Exception) {
                // Error discarding workout
            }
        }
    }

    fun loadLocalBackups() {
        _localBackupsList.value = getLocalBackups()
    }

    // Helper utilities
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private object BackupCrypto {
        private const val KEY_ALIAS = "startfit_backup_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private fun getOrCreateKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            }
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return keyGenerator.generateKey()
        }

        fun encrypt(plainText: String): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return iv + encrypted
        }

        fun decrypt(encryptedData: ByteArray): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = encryptedData.copyOfRange(0, 12)
            val cipherText = encryptedData.copyOfRange(12, encryptedData.size)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            return String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }
    }

    fun checkAndPerformMonthlyBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = Calendar.getInstance()
            if (today.get(Calendar.DAY_OF_MONTH) == 1) {
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(today.time)
                val filename = "backup_month_$monthFormat.enc"
                val file = java.io.File(app.filesDir, filename)
                if (!file.exists()) {
                    try {
                        val json = exportJson()
                        val encrypted = BackupCrypto.encrypt(json)
                        file.writeBytes(encrypted)
                    } catch (e: Exception) {
                        // Monthly backup failed silently
                    }
                }
            }
        }
    }

    fun getLocalBackups(): List<String> {
        val dir = app.filesDir
        val files = dir.listFiles { _, name -> name.startsWith("backup_month_") && name.endsWith(".enc") }
        return files?.map { it.name }?.sortedDescending() ?: emptyList()
    }

    fun generateManualBackup(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(today.time)
            val filename = "backup_month_$dateFormat.enc"
            val file = java.io.File(app.filesDir, filename)
            try {
                val json = exportJson()
                val encrypted = BackupCrypto.encrypt(json)
                file.writeBytes(encrypted)
                withContext(Dispatchers.Main) {
                    loadLocalBackups()

        // Cleanup old chats
        viewModelScope.launch { repository.cleanupOldChats() }
                    onResult("Copia guardada localmente: $filename")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Error al crear copia: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- SAF BACKUP LOGIC ---
    fun generateFullDatabaseBackupToStream(outputStream: java.io.OutputStream, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // removed
                val dbFile = app.getDatabasePath("startfit_database")
                val dbShmFile = java.io.File(dbFile.parent, "startfit_database-shm")
                val dbWalFile = java.io.File(dbFile.parent, "startfit_database-wal")
                
                java.util.zip.ZipOutputStream(outputStream).use { zos ->
                    val filesToZip = listOf(dbFile, dbShmFile, dbWalFile)
                    for (file in filesToZip) {
                        if (file.exists()) {
                            java.io.FileInputStream(file).use { fis ->
                                val entry = java.util.zip.ZipEntry(file.name)
                                zos.putNextEntry(entry)
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult("Copia ZIP guardada con éxito en la ubicación seleccionada.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Error al guardar ZIP: ${e.localizedMessage}")
                }
            }
        }
    }

    fun restoreFullDatabaseBackupFromStream(inputStream: java.io.InputStream, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // removed
                val dbFile = app.getDatabasePath("startfit_database")
                val parentDir = dbFile.parentFile ?: app.getDatabasePath("")
                
                java.util.zip.ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        // Avoid path traversal and extract only valid files
                        val fileName = entry.name
                        if (fileName == "startfit_database" || fileName == "startfit_database-shm" || fileName == "startfit_database-wal") {
                            val outFile = java.io.File(parentDir, fileName)
                            java.io.FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult("EXITO_REINICIAR")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Error al restaurar ZIP: ${e.localizedMessage}")
                }
            }
        }
    }

    fun restoreLocalBackup(filename: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = java.io.File(getApplication<Application>().filesDir, filename)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    onResult("Error: El archivo no existe")
                }
                return@launch
            }
            try {
                val encrypted = file.readBytes()
                val json = try {
                    BackupCrypto.decrypt(encrypted)
                } catch (e: Exception) {
                    // Fallback for legacy plaintext backups
                    encrypted.toString(Charsets.UTF_8)
                }
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val map = moshi.adapter(Map::class.java).fromJson(json) as? Map<*, *>
                if (map != null) {
                    val userName = map["userName"] as? String ?: "Usuario"
                    val fitnessGoal = map["fitnessGoal"] as? String ?: "Hipertrofia"
                    val bodyWeightObj = map["bodyWeight"]
                    val bodyWeight = when (bodyWeightObj) {
                        is Double -> bodyWeightObj
                        is Float -> bodyWeightObj.toDouble()
                        is Number -> bodyWeightObj.toDouble()
                        else -> 70.0
                    }
                    val current = settingsState.value
                    val updated = current.copy(
                        username = userName,
                        fitnessGoal = fitnessGoal,
                        bodyWeight = bodyWeight
                    )
                    repository.saveSettings(updated)
                    withContext(Dispatchers.Main) {
                        onResult("Copia de seguridad local restaurada exitosamente.")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult("Error: Formato de copia de seguridad inválido.")
                    }
                }
            } catch (e: java.lang.Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Error al restaurar: ${e.localizedMessage}")
                }
            }
        }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        try {
            val settings = settingsState.value
            val meals = repository.allMeals.firstOrNull() ?: emptyList()
            val sessionsList = repository.sessions.firstOrNull() ?: emptyList()

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val map = mapOf(
                "userName" to settings.username,
                "fitnessGoal" to settings.fitnessGoal,
                "bodyWeight" to settings.bodyWeight,
                "meals_count" to meals.size,
                "sessions_count" to sessionsList.size,
                "exportedAt" to SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )

            return@withContext moshi.adapter(Map::class.java).toJson(map)
        } catch (e: Exception) {
            return@withContext "{\"error\": \"${e.localizedMessage}\"}"
        }
    }

    suspend fun exportRoutinesJson(): String = withContext(Dispatchers.IO) {
        try {
            val routinesList = routines.value
            val exportedRoutines = routinesList.map { routine ->
                val exercises = repository.getExercisesForRoutineSync(routine.id)
                val exportedExercises = exercises.map { ex ->
                    ExportedPlanExercise(
                        exerciseName = ex.exerciseName,
                        targetSets = ex.targetSets,
                        orderIndex = ex.orderIndex
                    )
                }
                ExportedRoutine(
                    name = routine.name,
                    description = routine.description,
                    exercises = exportedExercises
                )
            }
            val exportData = ExportData(routines = exportedRoutines)
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            return@withContext moshi.adapter(ExportData::class.java).toJson(exportData)
        } catch (e: Exception) {
            return@withContext "{\"error\": \"${e.localizedMessage}\"}"
        }
    }

    suspend fun importRoutinesJson(jsonStr: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val exportData = moshi.adapter(ExportData::class.java).fromJson(jsonStr)
            
            if (exportData == null || exportData.routines.isEmpty()) {
                return@withContext false to "Archivo JSON no contiene rutinas válidas."
            }
            
            var importedCount = 0
            
            exportData.routines.forEach { expRoutine ->
                val newRoutineId = repository.insertRoutine(Routine(name = expRoutine.name, description = expRoutine.description))
                
                val newExercises = expRoutine.exercises.map { expEx ->
                    PlanExercise(
                        routineId = newRoutineId,
                        exerciseName = expEx.exerciseName,
                        targetSets = expEx.targetSets,
                        orderIndex = expEx.orderIndex
                    )
                }
                repository.insertPlanExercises(newExercises)
                
                newExercises.forEach {
                    try {
                        repository.insertExercise(Exercise(name = it.exerciseName))
                    } catch (e: Exception) {
                        // Ignored
                    }
                }
                
                importedCount++
            }
            
            return@withContext true to "$importedCount rutinas importadas exitosamente."
        } catch (e: Exception) {
            return@withContext false to "Error leyendo el archivo: ${e.localizedMessage}"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _activeChatSessionId.value = null
            _activeSession.value = null
            _activeLogs.value = emptyList()
            _elapsedSeconds.value = 0
            _isUserLoggedIn.value = false
        }
    }

    fun recalculateHeatmap() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                val sessions = repository.sessions.firstOrNull() ?: emptyList()
                val recentSessionIds = sessions.filter { it.dateMillis >= sevenDaysAgo }.map { it.id }.toSet()
                
                val allLogs = repository.allLogs.firstOrNull() ?: emptyList()
                val recentLogs = allLogs.filter { recentSessionIds.contains(it.sessionId) }
                
                val allExMap = repository.getAllExercisesSync().associateBy { it.name.lowercase().trim() }
                
                val muscleVolumes = mutableMapOf<String, Double>()
                
                recentLogs.forEach { log ->
                    val exName = log.exerciseName.lowercase().trim()
                    val effectiveWeight = if (log.weightKg > 0.0) log.weightKg else 20.0
                    val volume = effectiveWeight * log.reps
                    
                    val exObj = allExMap[exName]
                    val categories = exObj?.category?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() } ?: listOf("otros")
                    
                    categories.forEach { cat ->
                        var normalizedCat = cat
                            .replace("á", "a")
                            .replace("é", "e")
                            .replace("í", "i")
                            .replace("ó", "o")
                            .replace("ú", "u")

                        normalizedCat = when (normalizedCat) {
                            "abdomen", "oblicuo" -> "core"
                            "trapecio", "romboides" -> "trapecio y romboides"
                            "serrato" -> "pectoral"
                            "hombro frontal", "hombro trasero" -> "hombros"
                            else -> normalizedCat
                        }
                        muscleVolumes[normalizedCat] = (muscleVolumes[normalizedCat] ?: 0.0) + volume
                    }
                }
                
                repository.clearHeatmapData()
                muscleVolumes.forEach { (muscle, vol) ->
                    repository.insertHeatmapData(HeatmapData(muscleGroup = muscle, accumulatedVolume = vol))
                }
            } catch (e: Exception) {
                // error recalculating heatmap
            }
        }
    }

    suspend fun checkAndDownloadProfileSuspend(): Boolean {
        val success = syncManager.syncDownProfile()
        if (success) {
            _isUserLoggedIn.value = true
        }
        syncManager.syncDownAll()
        return success
    }
}
