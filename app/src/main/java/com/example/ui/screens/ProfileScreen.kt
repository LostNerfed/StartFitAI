package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current

    var gender by remember { mutableStateOf(settings.gender) }
    var ageStr by remember { mutableStateOf(settings.age.toString()) }
    var heightStr by remember { mutableStateOf(settings.heightCm.toString()) }
    var weightStr by remember { mutableStateOf(settings.bodyWeight.toString()) }
    var heightUnit by remember { mutableStateOf(settings.heightUnit) }
    var weightUnit by remember { mutableStateOf(settings.weightUnit) }
    var targetCaloriesStr by remember { mutableStateOf(if (settings.targetCalories > 0) settings.targetCalories.toString() else "") }
    
    val currentAge = ageStr.toIntOrNull() ?: settings.age
    val currentHeight = heightStr.toDoubleOrNull() ?: settings.heightCm
    val currentWeight = weightStr.toDoubleOrNull() ?: settings.bodyWeight
    val goalsList = listOf(
        stringResource(R.string.goal_hypertrophy),
        stringResource(R.string.goal_fat_loss),
        stringResource(R.string.goal_strength),
        stringResource(R.string.goal_maintenance),
        stringResource(R.string.prog_manual)
    )
    var selectedGoal by remember { mutableStateOf(settings.fitnessGoal) }
    
    var activityLevel by remember { mutableStateOf(settings.activityLevel) }

    val calculatedCalories = remember(currentAge, currentHeight, currentWeight, gender, activityLevel, selectedGoal) {
        if (currentAge <= 0 || currentHeight <= 0.0 || currentWeight <= 0.0) return@remember 0
        
        val heightInCm = if (heightUnit == "in") currentHeight * 2.54 else currentHeight
        val weightInKg = if (weightUnit == "lb") currentWeight / 2.20462 else currentWeight
        val bmr = (10.0 * weightInKg) + (6.25 * heightInCm) - (5.0 * currentAge) + if (gender == "Hombre" || gender == context.getString(R.string.auth_gender_m)) 5.0 else -161.0
        val pal = when {
            activityLevel.contains("Sedentario") || activityLevel.contains(context.getString(R.string.activity_sedentary)) -> 1.2
            activityLevel.contains("Ligero") || activityLevel.contains(context.getString(R.string.activity_light)) -> 1.375
            activityLevel.contains("Moderado") || activityLevel.contains(context.getString(R.string.activity_moderate)) -> 1.55
            activityLevel.contains("Activo") || activityLevel.contains(context.getString(R.string.activity_active)) -> 1.725
            activityLevel.contains("Muy Activo") || activityLevel.contains(context.getString(R.string.activity_very_active)) -> 1.9
            else -> 1.2
        }
        val tdee = bmr * pal
        val goalMultiplier = when {
            selectedGoal.contains("Hipertrofia") || selectedGoal.contains(context.getString(R.string.goal_hypertrophy)) -> 1.15
            selectedGoal.contains("Pérdida de Grasa") || selectedGoal.contains(context.getString(R.string.goal_fat_loss)) -> 0.85
            selectedGoal.contains("Fuerza") || selectedGoal.contains(context.getString(R.string.goal_strength)) -> 1.10
            else -> 1.0
        }
        (tdee * goalMultiplier).toInt()
    }

    val activityOptions = listOf(
        stringResource(R.string.activity_sedentary),
        stringResource(R.string.activity_light),
        stringResource(R.string.activity_moderate),
        stringResource(R.string.activity_active),
        stringResource(R.string.activity_very_active)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color.Transparent),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_physical_profile), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_hello_user, settings.username),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color.White, AccentGreen)
                    )
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_profile_desc),
                fontSize = 12.sp,
                color = TextSecundario
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.White.copy(alpha = 0.15f))

            // Gender selector
            Text(text = stringResource(R.string.home_gender), fontSize = 13.sp, color = TextSecundario)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (gender == context.getString(R.string.auth_gender_m)) Modifier.background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                            else Modifier.liquidGlassModifier(RoundedCornerShape(8.dp))
                        )
                        .clickable { gender = context.getString(R.string.auth_gender_m) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.auth_gender_m), color = if (gender == context.getString(R.string.auth_gender_m)) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (gender == context.getString(R.string.auth_gender_f)) Modifier.background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                            else Modifier.liquidGlassModifier(RoundedCornerShape(8.dp))
                        )
                        .clickable { gender = context.getString(R.string.auth_gender_f) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.auth_gender_f), color = if (gender == context.getString(R.string.auth_gender_f)) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.White.copy(alpha = 0.15f))

            // Age, Height, Weight fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.home_age), 
                    fontSize = 13.sp, 
                    color = TextSecundario, 
                    modifier = Modifier.weight(1f),
                    maxLines = 1, 
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.home_height_cm).substringBefore(" (").trim(), 
                    fontSize = 13.sp, 
                    color = TextSecundario, 
                    modifier = Modifier.weight(1.2f), 
                    maxLines = 1, 
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.home_weight_kglb).substringBefore(" (").trim(), 
                    fontSize = 13.sp, 
                    color = TextSecundario, 
                    modifier = Modifier.weight(1.2f), 
                    maxLines = 1, 
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.weight(1f).supercardGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it.filter { char -> char.isDigit() }.take(3) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier.weight(1.2f).supercardGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it.filter { char -> char.isDigit() || char == '.' }.take(5) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier.clickable { heightUnit = if (heightUnit == "cm") "in" else "cm" }.padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = heightUnit, fontSize = 11.sp, color = TextSecundario)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Cambiar", tint = TextSecundario, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1.2f).supercardGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it.filter { char -> char.isDigit() || char == '.' }.take(5) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier.clickable { weightUnit = if (weightUnit == "kg") "lb" else "kg" }.padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = weightUnit, fontSize = 11.sp, color = TextSecundario)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Cambiar", tint = TextSecundario, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.White.copy(alpha = 0.15f))

            // Activity level dropdown
            Text(text = stringResource(R.string.home_weekly_activity), fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activityOptions.forEach { option ->
                    val isSelected = option == activityLevel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) Modifier.background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                                else Modifier.liquidGlassModifier(RoundedCornerShape(8.dp))
                            )
                            .bounceClick { activityLevel = option }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.White.copy(alpha = 0.15f))

            // Fitness Goal Selector
            Text(text = stringResource(R.string.home_main_fitness_goal), fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goalsList.forEach { goal ->
                    val isSelected = goal == selectedGoal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) Modifier.background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                                else Modifier.liquidGlassModifier(RoundedCornerShape(8.dp))
                            )
                            .bounceClick { selectedGoal = goal }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = goal,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.White.copy(alpha = 0.15f))

            // Target Calories
            if (selectedGoal == context.getString(R.string.prog_manual)) {
                Text(text = stringResource(R.string.home_daily_cal_manual), fontSize = 13.sp, color = TextSecundario)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().supercardGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (targetCaloriesStr.isEmpty()) {
                        Text(stringResource(R.string.auth_cals_placeholder), color = TextSecundario, fontSize = 12.sp)
                    }
                    BasicTextField(
                        value = targetCaloriesStr,
                        onValueChange = { targetCaloriesStr = it.filter { char -> char.isDigit() }.take(4) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(text = stringResource(R.string.home_daily_cal_auto), fontSize = 13.sp, color = TextSecundario)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .supercardGlassModifier(RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (calculatedCalories > 0) stringResource(R.string.home_kcal_per_day, calculatedCalories) else stringResource(R.string.home_missing_data),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), color = Color.White.copy(alpha = 0.15f))

            Button(
                onClick = {
                    val ageInt = currentAge
                    val heightDouble = currentHeight
                    val weightDouble = currentWeight
                    val tCals = if (selectedGoal == context.getString(R.string.prog_manual)) {
                        targetCaloriesStr.toIntOrNull() ?: settings.targetCalories
                    } else {
                        calculatedCalories
                    }

                    viewModel.updateSettings(
                        settings.copy(
                            gender = gender,
                            age = ageInt,
                            heightCm = heightDouble,
                            bodyWeight = weightDouble,
                            targetCalories = tCals,
                            fitnessGoal = selectedGoal,
                            activityLevel = activityLevel,
                            weightUnit = weightUnit,
                            heightUnit = heightUnit
                        )
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .supercardGlassModifier(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_save), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
