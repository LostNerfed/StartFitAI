package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.launch
import com.example.data.database.PlanExercise
import com.example.data.database.Routine
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import com.example.ui.components.getTranslatedCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDaysScreen(
    viewModel: FitnessViewModel,
    onStartRoutineWorkout: (Routine) -> Unit
) {
    val routines by viewModel.routines.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val jsonStr = inputStream?.bufferedReader()?.use { it.readText() }
                    if (jsonStr != null) {
                        val result = viewModel.importRoutinesJson(jsonStr)
                        Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.plan_error_read_file), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonStr = viewModel.exportRoutinesJson()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonStr.toByteArray())
                    }
                    Toast.makeText(context, context.getString(R.string.plan_export_success), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.plan_error_save_file), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Sub-UI state routing
    var selectedRoutineForModelDetails by remember { mutableStateOf<Routine?>(null) }
    var showCreateRoutineDialog by remember { mutableStateOf(false) }
    var activePlanTab by remember { mutableStateOf(0) } // 0 = Tutinas, 1 = Lista Madre de Ejercicios
    var showAddCustomExerciseDialog by remember { mutableStateOf(false) }

    // Handle system back button when deep in routine details
    BackHandler(enabled = selectedRoutineForModelDetails != null) {
        selectedRoutineForModelDetails = null
    }

    val groupedExercises = remember(allExercises) {
        allExercises.groupBy { it.category }.toSortedMap()
    }


    val routinesListState = rememberLazyListState()
    val exercisesListState = rememberLazyListState()
    val currentListState = if (activePlanTab == 0) routinesListState else exercisesListState
    val isFabVisible by remember { derivedStateOf { !currentListState.isScrollInProgress } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Spacer(modifier = Modifier.height(10.dp))

        val selectedRoutine = selectedRoutineForModelDetails
        if (selectedRoutine == null) {
            // --- MAIN TABS (Rutinas vs Lista Madre) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .supercardGlassModifier(RoundedCornerShape(32.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (activePlanTab == 0) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .clickable { activePlanTab = 0 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.plan_my_routines),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activePlanTab == 0) Color.White else TextSecundario
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (activePlanTab == 1) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .clickable { activePlanTab = 1 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.plan_exercise_list),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activePlanTab == 1) Color.White else TextSecundario
                    )
                }
            }

            if (activePlanTab == 0) {
                // --- CATEGORY 1: PLAN DAYS VIEW ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.plan_your_routines),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.home_routine_templates),
                            fontSize = 11.sp,
                            color = TextSecundario
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier
                                .liquidGlassModifier(CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = stringResource(R.string.plan_import_routines), tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { exportLauncher.launch("mis_rutinas.json") },
                            modifier = Modifier
                                .liquidGlassModifier(CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = stringResource(R.string.plan_export_routines), tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                    }
                }

                if (routines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .supercardGlassModifier(RoundedCornerShape(12.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.plan_no_routines),
                            color = TextSecundario,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = routinesListState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(routines) { routine ->
                            RoutineRowItem(
                                routine = routine,
                                viewModel = viewModel,
                                onClick = { selectedRoutineForModelDetails = routine },
                                onStartWorkout = { onStartRoutineWorkout(routine) },
                                onDelete = { viewModel.deleteRoutine(routine) }
                            )
                        }
                    }
                }
            } else {
                // --- CATEGORY 2: LISTA MADRE VIEW ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.plan_exercises_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.plan_exercise_list),
                            fontSize = 11.sp,
                            color = TextSecundario
                        )
                    }


                }

                LazyColumn(
                    state = exercisesListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (allExercises.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.plan_no_exercises_category),
                                color = TextSecundario,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        groupedExercises.forEach { (category, exercises) ->
                            item {
                                Text(
                                    text = getTranslatedCategory(category),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                                )
                                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = Color.White.copy(alpha = 0.2f))
                            }
                            items(exercises.sortedBy { it.name }) { ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .metricCellGlassModifier(RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = ex.name, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(text = stringResource(R.string.plan_category_label, getTranslatedCategory(ex.category)), fontSize = 11.sp, color = TextSecundario, maxLines = 1)
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomExercise(ex) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = TextSecundario, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // --- DIALOG: ADD CUSTOM EXERCISE TO MOTHER LIST ---
            if (showAddCustomExerciseDialog) {
                var newExName by remember { mutableStateOf("") }
                var selectedMuscle by remember { mutableStateOf("") }
                var dropdownExpanded by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showAddCustomExerciseDialog = false },
                    modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    title = { Text(text = stringResource(R.string.plan_new_exercise), fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = stringResource(R.string.plan_add_exercise_desc), fontSize = 11.sp, color = TextSecundario)

                            OutlinedTextField(
                                value = newExName,
                                onValueChange = { newExName = it },
                                label = { Text(stringResource(R.string.plan_ex_name), color = TextSecundario) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                    cursorColor = Color.White,
                                    focusedContainerColor = Color(0x05FFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF))
                            )

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
                                    modifier = Modifier.background(AmoledBg)
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
                        }
                    },
                    confirmButton = {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                if (selectedMuscle.isEmpty()) {
                                    android.widget.Toast.makeText(context, "Por favor, selecciona una categoría.", android.widget.Toast.LENGTH_SHORT).show()
                                } else if (newExName.trim().isEmpty()) {
                                    android.widget.Toast.makeText(context, "Por favor, escribe el nombre del ejercicio.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addCustomExercise(newExName, selectedMuscle)
                                    newExName = ""
                                    showAddCustomExerciseDialog = false
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.btn_save), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCustomExerciseDialog = false }) {
                            Text(text = stringResource(R.string.btn_cancel), color = TextSecundario)
                        }
                    },
                    containerColor = AmoledSurface,
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }

        } else {
            // --- DAY TEMPLATE VIEW (Selected Routine Details) ---
            val activeDetailedRoutine = selectedRoutine
            val routineExercises by viewModel.getExercisesForRoutine(activeDetailedRoutine.id).collectAsState(initial = emptyList())

            var showAddExerciseDialogInDetails by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedRoutineForModelDetails = null },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = activeDetailedRoutine.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(text = stringResource(R.string.plan_edit_sets_exercises), fontSize = 11.sp, color = TextSecundario)
                }
            }

            // Quick action to start workout immediately
            Button(
                onClick = { onStartRoutineWorkout(activeDetailedRoutine) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_workout_from_routine_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.plan_start_active_session), fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = BorderColor)

            // Exercises editing list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routineExercises) { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .metricCellGlassModifier(RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = ex.exerciseName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            // Target set counts
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.plan_target_sets_count, ex.targetSets),
                                    fontSize = 12.sp,
                                    color = TextSecundario
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                // Quick action to adjust target set counts
                                Box(
                                    modifier = Modifier
                                        .background(BorderColorSubtle, RoundedCornerShape(4.dp))
                                        .clickable {
                                            val nextSets = if (ex.targetSets >= 10) 1 else ex.targetSets + 1
                                            viewModel.addExerciseToRoutine(activeDetailedRoutine.id, ex.exerciseName, nextSets)
                                            viewModel.removeExerciseFromRoutine(ex) // replace it
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = stringResource(R.string.plan_plus_one_set), fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.removeExerciseFromRoutine(ex) }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextSecundario, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialogInDetails = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("add_exercise_to_routine_details_button"),
                        border = BorderStroke(1.dp, com.example.ui.theme.BorderColorSubtle),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.plan_add_exercise), fontSize = 12.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            // Dialog inside detailed Day view to add exercise
            if (showAddExerciseDialogInDetails) {
                var selectedMuscle by remember { mutableStateOf("") }
                var dropdownExpanded by remember { mutableStateOf(false) }
                val allExercises by viewModel.allExercises.collectAsState()
                var tempExerciseName by remember { mutableStateOf("") }
                var tempSetsCount by remember { mutableStateOf("3") }

                AlertDialog(
                    onDismissRequest = { showAddExerciseDialogInDetails = false },
                    modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    title = { Text(text = stringResource(R.string.plan_add_exercise), fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Category filter chips
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
                                    modifier = Modifier.background(AmoledBg)
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

                            // Vertical exercise list grouped by category
                            if (allExercises.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.plan_no_exercises_in_category),
                                    fontSize = 11.sp,
                                    color = TextSecundario
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .metricCellGlassModifier(RoundedCornerShape(12.dp))
                                ) {
                                    val listScroll = androidx.compose.foundation.rememberScrollState()
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
                                                val isSelected = tempExerciseName == exObj.name
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { 
                                                            tempExerciseName = exObj.name
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

                            // Manual name override
                            OutlinedTextField(
                                value = tempExerciseName,
                                onValueChange = { tempExerciseName = it },
                                label = { Text(stringResource(R.string.prog_manual), color = TextSecundario) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                    cursorColor = Color.White,
                                    focusedContainerColor = Color(0x05FFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF))
                            )

                            OutlinedTextField(
                                value = tempSetsCount,
                                onValueChange = { tempSetsCount = it },
                                label = { Text(stringResource(R.string.plan_target_sets), color = TextSecundario) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                    cursorColor = Color.White,
                                    focusedContainerColor = Color(0x05FFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF))
                            )
                        }
                    },
                    confirmButton = {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                if (selectedMuscle.isEmpty()) {
                                    android.widget.Toast.makeText(context, "Por favor, selecciona una categoría.", android.widget.Toast.LENGTH_SHORT).show()
                                } else if (tempExerciseName.trim().isEmpty()) {
                                    android.widget.Toast.makeText(context, "Por favor, escribe el nombre del ejercicio.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val setsValue = tempSetsCount.toIntOrNull() ?: 3
                                    viewModel.addExerciseToRoutine(activeDetailedRoutine.id, tempExerciseName, setsValue, selectedMuscle)
                                    tempExerciseName = ""
                                    showAddExerciseDialogInDetails = false
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.btn_add), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            tempExerciseName = ""
                            showAddExerciseDialogInDetails = false
                        }) {
                            Text(text = stringResource(R.string.btn_apply), color = TextSecundario)
                        }
                    },
                    containerColor = AmoledSurface,
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }
        }
    }

    // --- DIALOG: CREATE NEW ROUTINE ---
    if (showCreateRoutineDialog) {
        var tempRoutineName by remember { mutableStateOf("") }
        var tempRoutineDesc by remember { mutableStateOf("") }
        
        val allExercises by viewModel.allExercises.collectAsState()

        val selectedExercises = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showCreateRoutineDialog = false },
            modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            title = { Text(text = stringResource(R.string.plan_new_routine), fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name field
                    OutlinedTextField(
                        value = tempRoutineName,
                        onValueChange = { tempRoutineName = it },
                        label = { Text(stringResource(R.string.plan_routine_name), color = TextSecundario) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.White,
                            cursorColor = Color.White,
                            focusedContainerColor = Color(0x05FFFFFF),
                            unfocusedContainerColor = Color(0x05FFFFFF))
                    )

                    OutlinedTextField(
                        value = tempRoutineDesc,
                        onValueChange = { tempRoutineDesc = it },
                        label = { Text(stringResource(R.string.plan_routine_desc), color = TextSecundario) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.White,
                            cursorColor = Color.White,
                            focusedContainerColor = Color(0x05FFFFFF),
                            unfocusedContainerColor = Color(0x05FFFFFF))
                    )

                    // Exercise selector
                    Text(text = "Añadir ejercicios a la rutina (Opcional)", fontSize = 12.sp, color = TextSecundario, fontWeight = FontWeight.Bold)

                    // Scrollable vertical grouped exercise list
                    if (allExercises.isEmpty()) {
                        Text(
                            text = stringResource(R.string.plan_no_exercises),
                            fontSize = 11.sp,
                            color = TextSecundario
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .metricCellGlassModifier(RoundedCornerShape(12.dp))
                        ) {
                            val listScroll = androidx.compose.foundation.rememberScrollState()
                            Column(
                                modifier = Modifier
                                            .verticalScroll(listScroll)
                                    .padding(bottom = 8.dp)
                            ) {
                                val exerciseCategories = groupedExercises.entries.toList()
                                exerciseCategories.forEachIndexed { catIdx, (category, exercises) ->
                                    Text(
                                        text = category,
                                        fontSize = 11.sp,
                                        color = TextSecundario,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 4.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 4.dp), color = Color.White.copy(alpha = 0.2f))
                                    
                                    exercises.sortedBy { it.name }.forEachIndexed { index, exObj ->
                                        val isChecked = selectedExercises.contains(exObj.name)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isChecked) Color.White.copy(alpha = 0.07f) else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (isChecked) selectedExercises.remove(exObj.name)
                                                    else selectedExercises.add(exObj.name)
                                                }
                                                .padding(start = 28.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = exObj.name,
                                                fontSize = 13.sp,
                                                color = if (isChecked) Color.White else TextSecundario,
                                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isChecked) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
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

                    // Summary of selected
                    if (selectedExercises.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.plan_selected_count, selectedExercises.size),
                            fontSize = 11.sp,
                            color = TextSecundario
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempRoutineName.trim().isNotEmpty()) {
                            viewModel.addRoutine(tempRoutineName, tempRoutineDesc, selectedExercises.toList())
                            showCreateRoutineDialog = false
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.plan_create_routine), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoutineDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel), color = TextSecundario)
                }
            },
            containerColor = AmoledSurface,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    } // Close Column

        // FAB logic
        if (selectedRoutineForModelDetails == null) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isFabVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 100 }),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { 100 }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (activePlanTab == 0) showCreateRoutineDialog = true
                        else showAddCustomExerciseDialog = true
                    },
                    containerColor = AccentPrimary,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
                }
            }
        }
    } // Close Box wrapper
}

@Composable
fun RoutineRowItem(
    routine: Routine,
    viewModel: FitnessViewModel,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val exerciseList by viewModel.getExercisesForRoutine(routine.id).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supercardGlassModifier(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Workout",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = routine.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .metricCellGlassModifier(CircleShape)
                        .size(32.dp)
                        .testTag("routine_row_item_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.plan_start_workout),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Routine", tint = TextSecundario, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
                title = { Text(stringResource(R.string.plan_delete_routine), color = Color.White) },
                text = { Text(stringResource(R.string.plan_delete_confirm), color = TextSecundario) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }) { Text(stringResource(R.string.btn_delete), color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.btn_cancel), color = Color.White) }
                },
                containerColor = AmoledSurface
            )
        }

        if (routine.description.isNotEmpty()) {
            Text(
                text = routine.description,
                fontSize = 12.sp,
                color = TextSecundario,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Show expandable list of exercises
        if (expanded && exerciseList.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exerciseList.forEach { ex ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ex.exerciseName, 
                            fontSize = 13.sp, 
                            color = Color.White,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(text = stringResource(R.string.plan_target_sets_value, ex.targetSets), fontSize = 11.sp, color = TextSecundario)
                    }
                }
            }
        }
    }
}
