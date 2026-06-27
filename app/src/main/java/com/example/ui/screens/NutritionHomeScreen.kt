package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Meal
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.components.ApiKeyDialog

@Composable
fun NutritionHomeScreen(
    viewModel: FitnessViewModel,
    onNavigateToMealDetails: (category: String, date: String) -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val meals by viewModel.selectedDateMeals.collectAsState()
    val weeklyCaloriesMap by viewModel.weeklyCaloriesState.collectAsState()
    val mealFoodsMap by viewModel.mealFoods.collectAsState()
    val mealAnalysisLoading by viewModel.mealAnalysisLoading.collectAsState()
    
    val maintenanceCalories by viewModel.maintenanceCalories.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            if (msg.contains("éxito")) {
                // Using reflection or state variable would be cleaner, but for now we just handle it via the state hoist
            }
        }
    }

    // Collapsible state for sections
    var showMealForm by remember { mutableStateOf(false) }
    var showMealHistory by remember { mutableStateOf(false) }
    var showWeeklyChart by remember { mutableStateOf(false) }
    var showApiDialog by remember { mutableStateOf(false) }

    // Calculate sum of consumed macros for the day
    val totalCalories = meals.sumOf { it.totalCalories }
    val totalProtein = meals.sumOf { it.totalProtein }
    val totalCarbs = meals.sumOf { it.totalCarbs }
    val totalFat = meals.sumOf { it.totalFat }

    // Macros fractions
    val proteinFraction = if (settings.targetProtein > 0) (totalProtein / settings.targetProtein).toFloat().coerceIn(0f, 1f) else 0f
    val carbsFraction = if (settings.targetCarbs > 0) (totalCarbs / settings.targetCarbs).toFloat().coerceIn(0f, 1f) else 0f
    val fatFraction = if (settings.targetFat > 0) (totalFat / settings.targetFat).toFloat().coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))


        // Header: title + today button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nut_diary),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(
                onClick = { viewModel.selectDate(getFormattedToday()) },
                modifier = Modifier
                    .liquidGlassModifier(CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Today",
                    tint = AccentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDatePicker(
            selectedDate = selectedDate,
            onDateSelect = { dateStr -> viewModel.selectDate(dateStr) }
        )

        // Macro summary card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .supercardGlassModifier(RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            val dynamicTargetCalories = remember(settings.targetCalories, settings.fitnessGoal, maintenanceCalories) {
                if (settings.targetCalories > 0) return@remember settings.targetCalories
                val goalMultiplier = when {
                    settings.fitnessGoal.contains("Hipertrofia", ignoreCase = true) -> 1.15
                    settings.fitnessGoal.contains("Pérdida de Grasa", ignoreCase = true) -> 0.85
                    settings.fitnessGoal.contains("Fuerza", ignoreCase = true) -> 1.10
                    else -> 1.0
                }
                (maintenanceCalories * goalMultiplier).toInt()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = stringResource(R.string.nut_consumed_today), fontSize = 12.sp, color = TextSecundario)
                    Text(
                        text = "$totalCalories kcal",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.nut_daily_goal), fontSize = 12.sp, color = TextSecundario)
                    Text(
                        text = "${dynamicTargetCalories} kcal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.nut_maintenance, maintenanceCalories),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress line
            LinearProgressIndicator(
                progress = { if (dynamicTargetCalories > 0) (totalCalories.toFloat() / dynamicTargetCalories).coerceIn(0f, 1f) else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color.White,
                trackColor = ProgressTrackColor,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Detail of Proteínas, Carbohidratos y Grasas linear progress bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroProgressWidget(
                    title = stringResource(R.string.home_proteins),
                    amount = "${totalProtein.toInt()}g",
                    target = "${settings.targetProtein}g",
                    ratio = proteinFraction,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressWidget(
                    title = stringResource(R.string.home_carbs),
                    amount = "${totalCarbs.toInt()}g",
                    target = "${settings.targetCarbs}g",
                    ratio = carbsFraction,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressWidget(
                    title = stringResource(R.string.home_fats),
                    amount = "${totalFat.toInt()}g",
                    target = "${settings.targetFat}g",
                    ratio = fatFraction,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Consumo Semanal (always visible) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .supercardGlassModifier(RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Semanal",
                    tint = AccentAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.nut_weekly_consumption),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            WeeklyNutritionBarChart(weeklyCaloriesMap)
        }
        // ── Registrar alimento (collapsible button) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Trigger button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .supercardGlassModifier(RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showMealForm = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Agregar",
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.nut_log_food),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            // Centered Dialog for Meal Form
            if (showMealForm) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showMealForm = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        UnifiedMealCard(
                            meals = meals,
                            mealFoodsMap = mealFoodsMap,
                            selectedDate = selectedDate,
                            mealAnalysisLoading = mealAnalysisLoading,
                            onLogMeal = { category, description ->
                                viewModel.logMealFromNaturalLanguage(category, description, selectedDate) { success ->
                                        if (success) showMealForm = false
                                    }
                            },
                            onDeleteMeal = { mealId -> viewModel.deleteMeal(mealId) },
                            showHistory = false
                        )
                    }
                }
            }
        }

        // ── Historial de comidas (collapsible) ──
        val totalMeals = meals.size
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .supercardGlassModifier(RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showMealHistory = !showMealHistory }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = "Historial",
                    tint = if (showMealHistory) Color.White else TextSecundario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.nut_meals_today),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (showMealHistory) Color.White else TextSecundario,
                    modifier = Modifier.weight(1f)
                )
                if (totalMeals > 0) {
                    Text(
                        text = stringResource(R.string.nut_records_count, totalMeals),
                        fontSize = 11.sp,
                        color = TextSecundario
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = if (showMealHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecundario,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = showMealHistory) {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    MealHistoryContent(
                        meals = meals,
                        mealFoodsMap = mealFoodsMap,
                        onDeleteMeal = { mealId -> viewModel.deleteMeal(mealId) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))

        if (showApiDialog) {
            ApiKeyDialog(
                onDismiss = { showApiDialog = false },
                onNavigateToSettings = { showApiDialog = false }
            )
        }
    }
}

@Composable
fun MacroProgressWidget(
    title: String,
    amount: String,
    target: String,
    ratio: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
                Text(text = title, fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = amount, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.nut_of_target, target), fontSize = 10.sp, color = TextSecundario, modifier = Modifier.padding(bottom = 1.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color.White,
            trackColor = ProgressTrackColor,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun HorizontalDatePicker(
    selectedDate: String,
    onDateSelect: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val dates = remember { generateDatesAroundToday() }

    // Scroll to position matching selected dates
    LaunchedEffect(selectedDate) {
        val idx = dates.indexOfFirst { it.dateString == selectedDate }
        if (idx != -1) {
            listState.animateScrollToItem((idx - 2).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().testTag("horizontal_date_picker"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(dates) { item ->
            val isSelected = item.dateString == selectedDate
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.97f))
                        } else {
                            Modifier.liquidGlassModifier(RoundedCornerShape(12.dp))
                        }
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDateSelect(item.dateString) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.dayOfWeek,
                        color = if (isSelected) Color.Black else TextSecundario,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.dayNumber,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Custom amoled weekly bar chart
@Composable
fun WeeklyNutritionBarChart(weeklyCaloriesMap: Map<String, Int>) {
    val dates = weeklyCaloriesMap.keys.sorted().takeLast(7)
    val values = dates.map { weeklyCaloriesMap[it] ?: 0 }
    val maxValue = values.maxOrNull()?.coerceAtLeast(2000) ?: 2000

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(70.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        dates.forEachIndexed { i, dateString ->
            val calories = weeklyCaloriesMap[dateString] ?: 0
            val fraction = calories.toFloat() / maxValue
            val heightPercent = fraction.coerceIn(0.05f, 1f)

            val displayDay = getShortDayName(dateString)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (calories > 0) "${calories}" else "-",
                    fontSize = 10.sp,
                    color = if (calories > 0) Color.White else TextSecundario,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // The bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(40.dp * heightPercent)
                ) {
                    drawRect(
                        color = if (calories > 0) Color.White else BorderColorSubtle,
                        size = size
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = displayDay,
                    fontSize = 11.sp,
                    color = TextSecundario,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ─── Unified Meal Registration Card ──────────────────────────────────────────

@Composable
fun UnifiedMealCard(
    meals: List<Meal>,
    mealFoodsMap: Map<String, List<com.example.data.database.Food>>,
    selectedDate: String,
    mealAnalysisLoading: Boolean,
    onLogMeal: (category: String, description: String) -> Unit,
    onDeleteMeal: (String) -> Unit,
    showHistory: Boolean = true
) {
    val categories = listOf("Desayuno", "Almuerzo", "Cena", "Snack")
    val categoryIcons = mapOf(
        "Desayuno" to Icons.Default.LightMode,
        "Almuerzo" to Icons.Default.WbSunny,
        "Cena" to Icons.Default.NightlightRound,
        "Snack" to Icons.Default.RestaurantMenu
    )

    var selectedCategory by remember { mutableStateOf("Desayuno") }
    var descriptionInput by remember { mutableStateOf("") }

    // Group meals by category for display
    val mealsByCategory = categories.associateWith { cat -> meals.filter { it.category == cat } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supercardGlassModifier(RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        Text(
            text = stringResource(R.string.nut_log_food_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Category chip selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.97f))
                            } else {
                                Modifier.metricCellGlassModifier(RoundedCornerShape(8.dp))
                            }
                        )
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getTranslatedCategory(cat),
                        color = if (isSelected) Color.Black else TextSecundario,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Natural language text field
        OutlinedTextField(
            value = descriptionInput,
            onValueChange = { descriptionInput = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.nut_example_placeholder),
                    fontSize = 11.sp,
                    color = TextSecundario,
                    lineHeight = 16.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .testTag("unified_meal_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.White,
                cursorColor = Color.White,
                focusedContainerColor = Color(0x05FFFFFF),
                unfocusedContainerColor = Color(0x05FFFFFF)),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 4
        )

        // AI analyze button
        Button(
            onClick = {
                if (descriptionInput.trim().isNotEmpty()) {
                    onLogMeal(selectedCategory, descriptionInput.trim())
                    descriptionInput = ""
                }
            },
            enabled = !mealAnalysisLoading && descriptionInput.trim().isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .supercardGlassModifier(RoundedCornerShape(10.dp))
                .testTag("unified_log_meal_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = AccentPurple,
                disabledContainerColor = BorderColorSubtle,
                disabledContentColor = TextSecundario
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (mealAnalysisLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AccentPurple,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "IA",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.nut_analyze_ai),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Logged meals per category (only when showHistory=true)
        if (showHistory) {
            val hasAnyMeal = meals.isNotEmpty()
            if (hasAnyMeal) {
                HorizontalDivider(color = BorderColorSubtle, thickness = 1.dp)
                Text(
                    text = stringResource(R.string.nut_meals_of_day),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            categories.forEach { cat ->
                val catMeals = mealsByCategory[cat] ?: emptyList()
                if (catMeals.isNotEmpty()) {
                    MealCategorySection(
                        categoryName = cat,
                        icon = categoryIcons[cat] ?: Icons.Default.RestaurantMenu,
                        meals = catMeals,
                        mealFoodsMap = mealFoodsMap,
                        onDeleteMeal = onDeleteMeal
                    )
                }
            }
        }
    }
}

@Composable
fun MealHistoryContent(
    meals: List<Meal>,
    mealFoodsMap: Map<String, List<com.example.data.database.Food>>,
    onDeleteMeal: (String) -> Unit
) {
    val categories = listOf("Desayuno", "Almuerzo", "Cena", "Snack")
    val categoryIcons = mapOf(
        "Desayuno" to Icons.Default.LightMode,
        "Almuerzo" to Icons.Default.WbSunny,
        "Cena" to Icons.Default.NightlightRound,
        "Snack" to Icons.Default.RestaurantMenu
    )
    val mealsByCategory = categories.associateWith { cat -> meals.filter { it.category == cat } }
    val hasAny = meals.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supercardGlassModifier(RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hasAny) {
            Text(
                text = stringResource(R.string.nut_no_meals_today),
                fontSize = 12.sp,
                color = TextSecundario,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            categories.forEach { cat ->
                val catMeals = mealsByCategory[cat] ?: emptyList()
                if (catMeals.isNotEmpty()) {
                    MealCategorySection(
                        categoryName = cat,
                        icon = categoryIcons[cat] ?: Icons.Default.RestaurantMenu,
                        meals = catMeals,
                        mealFoodsMap = mealFoodsMap,
                        onDeleteMeal = onDeleteMeal
                    )
                }
            }
        }
    }
}

@Composable
fun MealCategorySection(
    categoryName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    meals: List<Meal>,
    mealFoodsMap: Map<String, List<com.example.data.database.Food>>,
    onDeleteMeal: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val totalCal = meals.sumOf { it.totalCalories }

    val catColor = when(categoryName) {
        "Desayuno" -> Color(0xFFFFEB3B)
        "Almuerzo" -> AccentAmber
        "Cena" -> AccentGreen
        "Snack" -> Color(0xFF4CAF50)
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Category header (collapsible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .metricCellGlassModifier(RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(6.dp).height(44.dp).background(catColor))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = categoryName,
                    tint = catColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = getTranslatedCategory(categoryName), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(
                text = "$totalCal kcal",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(end = 12.dp)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Colapsar" else "Expandir",
                tint = TextSecundario,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                meals.forEach { meal ->
                    val foods = mealFoodsMap[meal.id] ?: emptyList()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BorderColorSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meal.inputText,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = stringResource(R.string.nut_macro_summary, meal.totalCalories, meal.totalProtein.toInt(), meal.totalCarbs.toInt(), meal.totalFat.toInt()),
                                    fontSize = 11.sp,
                                    color = TextSecundario,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeleteMeal(meal.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.btn_delete),
                                    tint = TextSecundario,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (foods.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            foods.forEach { food ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = food.name,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${food.calories} kcal",
                                        fontSize = 11.sp,
                                        color = TextSecundario,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers
@Composable
fun getTranslatedCategory(cat: String): String {
    return when(cat) {
        "Desayuno" -> stringResource(R.string.nut_breakfast)
        "Almuerzo" -> stringResource(R.string.nut_lunch)
        "Cena" -> stringResource(R.string.nut_dinner)
        "Snack" -> stringResource(R.string.nut_snack)
        else -> cat
    }
}

data class DateItem(
    val dateString: String,
    val dayOfWeek: String,
    val dayNumber: String
)

fun generateDatesAroundToday(): List<DateItem> {
    val list = mutableListOf<DateItem>()
    val cal = Calendar.getInstance()
    // Go -15 days back and +15 days forward
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US) // Standard for databases, but keep safe
    val dayNumFormat = SimpleDateFormat("d", java.util.Locale.getDefault())
    val dayOfWeekFormat = SimpleDateFormat("EEE", java.util.Locale.getDefault()) // "lun", "mar"

    cal.add(Calendar.DAY_OF_YEAR, -15)
    for (i in 0 until 31) {
        list.add(
            DateItem(
                dateString = format.format(cal.time),
                dayOfWeek = dayOfWeekFormat.format(cal.time).uppercase().take(3),
                dayNumber = dayNumFormat.format(cal.time)
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}

fun getFormattedToday(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

fun getShortDayName(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString) ?: Date()
        val format = SimpleDateFormat("EE", java.util.Locale.getDefault()) // "Lu", "Ma", "Mi"
        format.format(date).take(3).uppercase()
    } catch (e: Exception) {
        ""
    }
}
