package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Session
import com.example.data.database.SessionLog
import com.example.ui.components.getTranslatedCategory
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: FitnessViewModel,
    onMinimize: () -> Unit,
    onFinish: () -> Unit
) {
    val safeContext = androidx.compose.ui.platform.LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val activeLogs by viewModel.activeLogs.collectAsState()
    val activeExercisePRs by viewModel.activeExercisePRs.collectAsState()
    val completedSets by viewModel.completedSets.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    var backdateOffsetDays by remember { mutableStateOf(0) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSaveWarnDialog by remember { mutableStateOf(false) }
    var tempAddExerciseName by remember { mutableStateOf("") }
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    val currentSession = activeSession
    if (currentSession == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(R.string.aw_loading), color = Color.White)
        }
        return
    }



    // Group logs by exercise name
    val groupedLogs = remember(activeLogs) {
        activeLogs.groupBy { it.exerciseName }
    }

    BackHandler {
        onMinimize()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .imePadding(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSession.routineName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            ChronometerText(viewModel = viewModel)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDiscardDialog = true },
                        modifier = Modifier.testTag("discard_workout_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel), tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
            androidx.compose.animation.AnimatedVisibility(
                visible = restTimerSeconds > 0,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .background(AccentGreen, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { viewModel.adjustRestTimer(30) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val min = restTimerSeconds / 60
                    val sec = restTimerSeconds % 60
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d", min, sec),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "+30s", color = Color.White.copy(alpha=0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White.copy(alpha=0.7f), modifier = Modifier.size(14.dp).clickable { viewModel.cancelRestTimer() })
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Backdate picker
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .supercardGlassModifier(RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.aw_log_date),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val dateLabels = listOf(
                                0 to stringResource(R.string.aw_date_today),
                                1 to stringResource(R.string.aw_date_yesterday),
                                2 to stringResource(R.string.aw_date_2days)
                            )
                            dateLabels.forEach { (offset, label) ->
                                val isSelected = backdateOffsetDays == offset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
                                            } else {
                                                Modifier.metricCellGlassModifier(RoundedCornerShape(8.dp))
                                            }
                                        )
                                        .clickable { backdateOffsetDays = offset }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Exercise cards
                items(groupedLogs.keys.toList(), key = { it }) { exName ->
                    val logsForEx = groupedLogs[exName]?.sortedWith(compareBy({ it.setIndex }, { it.isDropset })) ?: emptyList()
                    val prLog = activeExercisePRs[exName]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                if (prLog != null && prLog.weightKg > 0) {
                                    val prWeightStr = viewModel.formatDisplayWeight(prLog.weightKg)
                                    val prUnit = viewModel.getUnitString()
                                    val rm1 = viewModel.calculate1RM(prLog.weightKg, prLog.reps)
                                    val rm1Str = viewModel.formatDisplayWeight(rm1)
                                    Text(
                                        text = "${stringResource(R.string.aw_pr_record, prWeightStr, prUnit, prLog.reps, prLog.setIndex)}  •  1RM: $rm1Str $prUnit",
                                        fontSize = 11.sp,
                                        color = AccentAmber.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Add exercise set shortcut
                            IconButton(onClick = { viewModel.addActiveSet(exName) }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Suma Serie", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Column heads
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(text = stringResource(R.string.aw_set_header), modifier = Modifier.width(60.dp), fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.Bold)
                            Text(
                                text = settings.weightUnit.uppercase(Locale.getDefault()) + " ▼", 
                                modifier = Modifier.weight(1f).clickable { viewModel.toggleWeightUnit() }, 
                                fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                            )
                            Text(text = stringResource(R.string.aw_reps_header), modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.width(36.dp)) // space for delete action
                        }

                        val logsBySet = logsForEx.groupBy { it.setIndex }.toSortedMap()
                        logsBySet.forEach { (setIndex, logsInSet) ->
                            logsInSet.forEach { sLog ->
                            val isCompleted = completedSets.contains(sLog.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(
                                        if (isCompleted) AccentGreen.copy(alpha = 0.15f) else Color.Transparent, 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 4.dp), // inner padding after background
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Label
                                Box(
                                    modifier = Modifier.width(60.dp).padding(start = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (sLog.isDropset) {
                                            Icon(imageVector = Icons.Default.SubdirectoryArrowRight, contentDescription = "Dropset", tint = TextSecundario, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = stringResource(R.string.aw_dropset, sLog.setIndex), fontSize = 11.sp, color = if(isCompleted) TextSecundario else Color.White, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(text = stringResource(R.string.aw_set_num, sLog.setIndex), fontSize = 12.sp, color = TextSecundario, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // KG/LBS Input
                                var weightText by remember(sLog.id, settings.weightUnit) { mutableStateOf(viewModel.formatDisplayWeight(sLog.weightKg)) }
                                LaunchedEffect(weightText) {
                                    delay(400)
                                    val parseWt = weightText.toDoubleOrNull() ?: 0.0
                                    val storageWt = viewModel.getStorageWeight(parseWt)
                                    if (kotlin.math.abs(storageWt - sLog.weightKg) > 0.01) {
                                        viewModel.updateActiveSetWeight(sLog.id, storageWt)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .height(36.dp)
                                        .liquidGlassModifier(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = weightText,
                                        onValueChange = { input -> weightText = input },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(
                                            color = if(isCompleted) AccentGreen else Color.White,
                                            textAlign = TextAlign.Center,
                                            fontSize = 15.sp,
                                            fontWeight = if(isCompleted) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Reps Input
                                var repsText by remember(sLog.id) { mutableStateOf(if (sLog.reps > 0) sLog.reps.toString() else "") }
                                LaunchedEffect(repsText) {
                                    delay(400)
                                    val parseReps = repsText.toIntOrNull() ?: 0
                                    if (parseReps != sLog.reps) {
                                        viewModel.updateActiveSetReps(sLog.id, parseReps)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .height(36.dp)
                                        .liquidGlassModifier(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = repsText,
                                        onValueChange = { input -> repsText = input },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(
                                            color = if(isCompleted) AccentGreen else Color.White,
                                            textAlign = TextAlign.Center,
                                            fontSize = 15.sp,
                                            fontWeight = if(isCompleted) FontWeight.Bold else FontWeight.Normal
                                         ),
                                         cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                         modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                // Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(end = 4.dp)
                                        .then(
                                            if (isCompleted) {
                                                Modifier.background(AccentGreen, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
                                            } else {
                                                Modifier.metricCellGlassModifier(RoundedCornerShape(8.dp))
                                            }
                                        )
                                        .clickable { viewModel.toggleSetComplete(sLog.id, safeContext) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Completado", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }

                                // Delete Set Button
                                IconButton(
                                    onClick = { viewModel.deleteActiveSetLog(sLog.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Set", tint = TextSecundario, modifier = Modifier.size(14.dp))
                                }
                            }

                            }

                            // Add Dropset Button at the end of the group
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.addActiveDropset(exName, setIndex) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Dropset", modifier = Modifier.size(10.dp), tint = TextSecundario)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(R.string.aw_add_dropset), fontSize = 11.sp, color = TextSecundario)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BorderColorSubtle)
                        )
                    }
                }

                // Add random exercise button
                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_exercise_to_active_button"),
                        border = BorderStroke(1.dp, com.example.ui.theme.BorderColorSubtle),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Exercise", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.aw_add_alt_exercise), fontSize = 13.sp)
                        }
                    }
                }

                // Finish Workout prominent bottom button
                item {
                    Button(
                        onClick = {
                            // Check if there are any incomplete sets
                            val isIncomplete = activeLogs.any { it.weightKg <= 0 || it.reps <= 0 }
                            if (isIncomplete) {
                                showSaveWarnDialog = true
                            } else {
                                viewModel.finishActiveWorkout(backdateOffsetDays) {
                                    onFinish()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("finish_workout_button")
                            .supercardGlassModifier(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.aw_finish_workout), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        // Dialogs: Discard workout confirmation
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(text = stringResource(R.string.aw_cancel_title)) },
                text = { Text(text = stringResource(R.string.aw_cancel_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.discardActiveWorkout()
                            showDiscardDialog = false
                            onFinish()
                        }
                    ) {
                        Text(text = stringResource(R.string.aw_discard), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(text = stringResource(R.string.aw_go_back), color = Color.White)
                    }
                },
                modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                containerColor = AmoledSurface,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }

        // Dialogs: Empty sets Warning
        if (showSaveWarnDialog) {
            AlertDialog(
                onDismissRequest = { showSaveWarnDialog = false },
                title = { Text(text = stringResource(R.string.aw_incomplete_sets_title)) },
                text = { Text(text = stringResource(R.string.aw_incomplete_sets_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSaveWarnDialog = false
                            viewModel.finishActiveWorkout(backdateOffsetDays) {
                                onFinish()
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.aw_save), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveWarnDialog = false }) {
                        Text(text = stringResource(R.string.aw_correct), color = TextSecundario)
                    }
                },
                modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                containerColor = AmoledSurface,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }

        // Dialogs: Add exercise in real time
        if (showAddExerciseDialog) {
            val allExercises by viewModel.allExercises.collectAsState()
            val muscleGroups = listOf("Pectoral", "Dorsal", "Trapecio", "Romboides", "Lumbar", "Cuádriceps", "Femorales", "Glúteo", "Pantorrilla", "Bíceps", "Tríceps", "Antebrazo", "Hombro Frontal", "Hombro Trasero", "Abdomen", "Oblicuo", "Serrato", "Otros")
            var selectedMuscle by remember { mutableStateOf("") }
            var dropdownExpanded by remember { mutableStateOf(false) }

            val displayedExercises = remember(allExercises, selectedMuscle) {
                if (selectedMuscle.isEmpty()) allExercises else allExercises.filter { it.category == selectedMuscle }
            }
            
            val groupedExercises = remember(displayedExercises) {
                displayedExercises.groupBy { it.category }.toSortedMap()
            }

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAddExerciseDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .supercardGlassModifier(RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.aw_new_exercise),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        
                        // Category Selector
                        Text(text = "Músculo Principal *", fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.Bold)
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = if (selectedMuscle.isNotEmpty()) getTranslatedCategory(selectedMuscle) else "Selecciona un músculo...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = BorderColor,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0x05FFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(AmoledSurface)
                            ) {
                                val groupedMuscleOptions = mapOf(
                                    "Pecho" to listOf("Pectoral"),
                                    "Espalda" to listOf("Dorsal", "Trapecio y Romboides", "Lumbar"),
                                    "Piernas" to listOf("Cuádriceps", "Femorales", "Glúteo", "Pantorrilla"),
                                    "Brazos" to listOf("Bíceps", "Tríceps", "Antebrazo"),
                                    "Hombros" to listOf("Hombros"),
                                    "Core" to listOf("Core"),
                                    "General" to listOf("Otros")
                                )
                                val muscleRegions = groupedMuscleOptions.entries.toList()
                                muscleRegions.forEachIndexed { idx, (region, muscles) ->
                                    DropdownMenuItem(
                                        text = { Text(getTranslatedCategory(region), color = TextSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { },
                                        enabled = false
                                    )
                                    muscles.forEach { muscle ->
                                        DropdownMenuItem(
                                            text = { Text(getTranslatedCategory(muscle), color = Color.White, modifier = Modifier.padding(start = 16.dp)) },
                                            onClick = {
                                                selectedMuscle = muscle
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                    if (idx < muscleRegions.lastIndex) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                                    }
                                }
                            }
                        }

                        // Vertical grouped exercise list
                        if (displayedExercises.isEmpty()) {
                            Text(text = stringResource(R.string.aw_empty_list), fontSize = 11.sp, color = TextSecundario)
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .supercardGlassModifier(RoundedCornerShape(12.dp))
                            ) {
                                val listScroll = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(listScroll)
                                        .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                                ) {
                                    val exerciseCategories = groupedExercises.entries.toList()
                                    exerciseCategories.forEachIndexed { catIdx, (category, exercises) ->
                                        Text(
                                            text = getTranslatedCategory(category),
                                            fontSize = 11.sp,
                                            color = TextSecundario,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp), color = Color.White.copy(alpha = 0.2f))
                                        exercises.sortedBy { it.name }.forEachIndexed { index, exObj ->
                                            val isSelected = tempAddExerciseName == exObj.name
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        tempAddExerciseName = exObj.name
                                                        selectedMuscle = exObj.category
                                                    }
                                                    .padding(start = 28.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = exObj.name,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) Color.White else TextSecundario,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            if (index < exercises.lastIndex) {
                                                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                                            }
                                        }
                                        if (catIdx < exerciseCategories.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.15f))
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tempAddExerciseName,
                            onValueChange = { tempAddExerciseName = it },
                            placeholder = { Text(stringResource(R.string.prog_manual)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().liquidGlassModifier(RoundedCornerShape(12.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                cursorColor = Color.White
                            , focusedContainerColor = Color(0x05FFFFFF), unfocusedContainerColor = Color(0x05FFFFFF))
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                tempAddExerciseName = ""
                                showAddExerciseDialog = false
                            }
                        ) {
                            Text(text = stringResource(R.string.aw_apply), color = TextSecundario)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                if (selectedMuscle.isEmpty()) {
                                    android.widget.Toast.makeText(context, "Por favor, selecciona una categoría.", android.widget.Toast.LENGTH_SHORT).show()
                                } else if (tempAddExerciseName.trim().isEmpty()) {
                                    android.widget.Toast.makeText(context, "Por favor, escribe el nombre del ejercicio.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addExerciseToActiveWorkout(tempAddExerciseName, selectedMuscle)
                                    tempAddExerciseName = ""
                                    showAddExerciseDialog = false
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.aw_add), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChronometerText(viewModel: FitnessViewModel) {
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val cronometerStr = remember(elapsedSeconds) {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }
    Text(
        text = stringResource(R.string.aw_live, cronometerStr),
        fontSize = 12.sp,
        color = TextSecundario,
        fontWeight = FontWeight.Medium
    )
}
