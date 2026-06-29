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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Meal
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import com.example.ui.theme.AppTextStyle
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
                style = AppTextStyle.headlineOswald.copy(color = Color.White)
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
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp)
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
                        style = AppTextStyle.numberMedium.copy(color = Color.White)
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

            Spacer(modifier = Modifier.height(12.dp))

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
                    color = MacroProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressWidget(
                    title = stringResource(R.string.home_carbs),
                    amount = "${totalCarbs.toInt()}g",
                    target = "${settings.targetCarbs}g",
                    ratio = carbsFraction,
                    color = MacroCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressWidget(
                    title = stringResource(R.string.home_fats),
                    amount = "${totalFat.toInt()}g",
                    target = "${settings.targetFat}g",
                    ratio = fatFraction,
                    color = MacroFat,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        

        

                // ── Registro Nativo (Unified Meal Card) ──
        UnifiedMealCard(
            meals = meals,
            mealFoodsMap = mealFoodsMap,
            selectedDate = selectedDate,
            mealAnalysisLoading = mealAnalysisLoading,
            onLogMeal = { category, description ->
                viewModel.logMealFromNaturalLanguage(category, description, selectedDate) { success -> }
            },
            onDeleteMeal = { mealId -> viewModel.deleteMeal(mealId) },
            showHistory = false
        )

        // ── Historial de Comidas de Hoy ──
        Text(
            text = stringResource(R.string.nut_meals_today),
            style = AppTextStyle.statBig.copy(color = Color.White),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
        )
        MealHistoryContent(
            meals = meals,
            mealFoodsMap = mealFoodsMap,
            onDeleteMeal = { mealId -> viewModel.deleteMeal(mealId) }
        )

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
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = "Semanal",
                    tint = AccentAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.nut_weekly_consumption),
                    style = AppTextStyle.statBig.copy(color = Color.White)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            WeeklyNutritionBarChart(weeklyCaloriesMap)
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
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
                Text(text = title, fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = amount, fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.nut_of_target, target), fontSize = 10.sp, color = TextSecundario, modifier = Modifier.padding(bottom = 1.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        style = AppTextStyle.titleOswald.copy(color = if (isSelected) Color.Black else Color.White)
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

                Spacer(modifier = Modifier.height(8.dp))

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
    val keyboardController = LocalSoftwareKeyboardController.current

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
            style = AppTextStyle.statBig.copy(color = Color.White)
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
                .testTag("unified_meal_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 1,
            maxLines = 3
        )

        // AI analyze button
        Button(
            onClick = {
                if (descriptionInput.trim().isNotEmpty()) {
                    keyboardController?.hide()
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
                containerColor = AccentPurple.copy(alpha = 0.15f),
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
                        modifier = Modifier.size(16.dp)
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

    }
}

@Composable
fun MealHistoryContent(
    meals: List<Meal>,
    mealFoodsMap: Map<String, List<com.example.data.database.Food>>,
    onDeleteMeal: (String) -> Unit
) {
    val hasAny = meals.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supercardGlassModifier(RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
    ) {
        if (!hasAny) {
            Text(
                text = stringResource(R.string.nut_no_meals_today),
                fontSize = 12.sp,
                color = TextSecundario,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            meals.forEachIndexed { index, meal ->
                MealListItem(meal = meal, onDeleteMeal = onDeleteMeal)
                if (index < meals.lastIndex) {
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun MealListItem(meal: Meal, onDeleteMeal: (String) -> Unit) {
    val catColor = when(meal.category) {
        "Desayuno" -> Color(0xFFFFEB3B)
        "Almuerzo" -> AccentAmber
        "Cena" -> AccentPrimary
        "Snack" -> GreenSecondary
        else -> Color.White
    }
    val icon = when(meal.category) {
        "Desayuno" -> Icons.Default.LightMode
        "Almuerzo" -> Icons.Default.WbSunny
        "Cena" -> Icons.Default.NightlightRound
        else -> Icons.Default.RestaurantMenu
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical color bar
        Box(modifier = Modifier.width(4.dp).height(40.dp).background(catColor, RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = meal.category, tint = catColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = getTranslatedCategory(meal.category), style = AppTextStyle.statSmall.copy(color = Color.White))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = meal.inputText,
                fontSize = 11.sp,
                color = TextSecundario,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${meal.totalCalories} kcal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = { onDeleteMeal(meal.id) }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextSecundario, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                text = "P:${meal.totalProtein.toInt()}g C:${meal.totalCarbs.toInt()}g G:${meal.totalFat.toInt()}g",
                fontSize = 10.sp,
                color = TextSecundario
            )
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
