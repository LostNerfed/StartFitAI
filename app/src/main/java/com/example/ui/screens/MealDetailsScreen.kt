package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Food
import com.example.data.database.Meal
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import com.example.ui.theme.AppTextStyle
import com.example.ui.components.ApiKeyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailsScreen(
    category: String,
    dateString: String,
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    var mealDescriptionInput by remember { mutableStateOf("") }
    val meals by viewModel.selectedDateMeals.collectAsState()
    val mealFoodsMap by viewModel.mealFoods.collectAsState()
    val mealAnalysisLoading by viewModel.mealAnalysisLoading.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showApiDialog by remember { mutableStateOf(false) }
    var showAiAnalysisDialog by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    var selectedCategory by remember(category) { mutableStateOf(category) }

    // Filter meals matching the active category
    val currentCategoryMeals = remember(meals, selectedCategory) {
        meals.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = selectedCategory,
                    style = AppTextStyle.headlineOswald.copy(color = Color.White)
                )
                Text(
                    text = "$dateString",
                    fontSize = 11.sp,
                    color = TextSecundario
                )
            }
        }

        // Category Selector Chips
        val categories = listOf("Desayuno", "Almuerzo", "Cena", "Snack")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isSelected) Modifier.background(AccentPrimary, RoundedCornerShape(12.dp)) else Modifier.liquidGlassModifier(RoundedCornerShape(12.dp)))
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // AI Analysis Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .liquidGlassModifier(RoundedCornerShape(16.dp))
                .clickable { showAiAnalysisDialog = true }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mealAnalysisLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.meal_analyzing), style = AppTextStyle.statBig.copy(color = Color.White))
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = AccentPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.meal_analyze_ai),
                        style = AppTextStyle.statBig.copy(color = Color.White)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.meal_logged_foods),
            style = AppTextStyle.statBig.copy(color = Color.White)
        )

        // List of logged foods
        if (currentCategoryMeals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .liquidGlassModifier(RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.meal_no_records),
                    color = TextSecundario,
                    fontSize = 11.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentCategoryMeals) { meal ->
                    val foods = mealFoodsMap[meal.id] ?: emptyList()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .supercardGlassModifier(RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (meal.inputText.length > 40) "${meal.inputText.take(40)}..." else meal.inputText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total: ${meal.totalCalories} kcal  |  P: ${meal.totalProtein.toInt()}g  |  C: ${meal.totalCarbs.toInt()}g  |  G: ${meal.totalFat.toInt()}g",
                                    style = AppTextStyle.statSmall.copy(color = TextSecundario)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteMeal(meal.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Meal", tint = TextSecundario, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Nested Food list parsed
                        if (foods.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = BorderColorSubtle)
                            Spacer(modifier = Modifier.height(8.dp))

                            foods.forEach { food ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = food.name,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${food.calories} kcal  (P: ${food.protein.toInt()}g  C: ${food.carbs.toInt()}g)",
                                        fontSize = 10.sp,
                                        color = TextSecundario
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showAiAnalysisDialog) {
        AlertDialog(
            onDismissRequest = { showAiAnalysisDialog = false },
            modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(R.string.meal_natural_language),
                    style = AppTextStyle.numberSmall.copy(color = Color.White)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.meal_ai_desc),
                        fontSize = 12.sp,
                        color = TextSecundario,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextField(
                        value = mealDescriptionInput,
                        onValueChange = { mealDescriptionInput = it },
                        placeholder = { Text(stringResource(R.string.meal_placeholder), fontSize = 14.sp, color = TextSecundario) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AmoledBg,
                            unfocusedContainerColor = AmoledBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AccentPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logMealFromNaturalLanguage(
                            category = selectedCategory,
                            inputText = mealDescriptionInput,
                            dateStr = dateString
                        ) { success ->
                            if (success) {
                                mealDescriptionInput = ""
                            }
                        }
                        showAiAnalysisDialog = false
                    },
                    enabled = mealDescriptionInput.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.meal_analyze), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiAnalysisDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel), color = TextSecundario)
                }
            }
        )
    }

    if (showApiDialog) {
        ApiKeyDialog(
            onDismiss = { showApiDialog = false },
            onNavigateToSettings = {
                showApiDialog = false
                onBack() // Or navigate to settings directly if possible. Let's just pop back.
            }
        )
    }
}
