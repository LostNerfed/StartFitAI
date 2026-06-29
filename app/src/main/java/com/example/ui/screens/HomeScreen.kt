package com.example.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.example.data.database.FitSettings
import com.example.data.database.Routine
import com.example.data.database.WeightEntry
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import com.example.ui.theme.bounceClick
import com.example.ui.components.ApiKeyDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: FitnessViewModel,
    onNavigateToSettings: () -> Unit,
    onStartActiveWorkout: (Routine) -> Unit,
    onStartCustomWorkout: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activeLogs by viewModel.activeLogs.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val maintenanceCalories by viewModel.maintenanceCalories.collectAsState()
    val chatLoading by viewModel.chatLoading.collectAsState()
    val selectedDateMeals by viewModel.selectedDateMeals.collectAsState()
    val routines by viewModel.routines.collectAsState(initial = emptyList())
    val activeSession by viewModel.activeSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState(initial = emptyList())

    var showPersonalizeSheet by remember { mutableStateOf(false) }
    var showCoachHistorySheet by remember { mutableStateOf(false) }
    var isCoachExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showStartWorkoutSheet by remember { mutableStateOf(false) }
    var showWeightHistorySheet by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    // Calculate metrics
    val consumedCalories = selectedDateMeals.sumOf { it.totalCalories }
    val targetCalories = settings.targetCalories

    val today = java.time.LocalDate.now()
    val logsByDate = sessions.groupBy { 
        java.time.Instant.ofEpochMilli(it.dateMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate() 
    }
    
    var currentStreak = 0
    val activeDates = logsByDate.keys.sortedDescending()
    if (activeDates.isNotEmpty()) {
        val mostRecent = activeDates.first()
        if (java.time.temporal.ChronoUnit.DAYS.between(mostRecent, today) <= 7) {
            currentStreak = 1
            for (i in 0 until activeDates.size - 1) {
                val current = activeDates[i]
                val previous = activeDates[i + 1]
                if (java.time.temporal.ChronoUnit.DAYS.between(previous, current) <= 7) {
                    currentStreak++
                } else {
                    break
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        containerColor = Color.Transparent,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Iniciar rutina", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    showStartWorkoutSheet = true
                                },
                                containerColor = AccentPrimary,
                                contentColor = Color.Black
                            ) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = "Iniciar rutina")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Registrar alimento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onNavigateToNutrition()
                                },
                                containerColor = AccentPrimary,
                                contentColor = Color.Black
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = "Registrar alimento")
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = AccentPrimary,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Expandir menú",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = maxOf(0.dp, innerPadding.calculateTopPadding() - 10.dp),
                    bottom = innerPadding.calculateBottomPadding()
                )
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    val streakBrush = androidx.compose.runtime.remember(currentStreak) {
                        when {
                            currentStreak < 7 -> Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF4A460), // Sandy Brown
                                    Color(0xFFFFDAB9), // Peach
                                    Color(0xFFCD853F), // Peru/Bronze
                                    Color(0xFF8B4513)  // Saddle Brown
                                )
                            )
                            currentStreak < 21 -> Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE5E4E2), // Platinum
                                    Color(0xFFFFFFFF),
                                    Color(0xFF9E9E9E),
                                    Color(0xFFF5F5F5)
                                )
                            )
                            currentStreak < 50 -> Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700), // Gold
                                    Color(0xFFFFFFE0),
                                    Color(0xFFDAA520),
                                    Color(0xFFB8860B)
                                )
                            )
                            else -> Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF), // TikTok Blue
                                    Color(0xFFB388FF), // Light Purple
                                    Color(0xFFFF0050), // TikTok Red/Pink
                                    Color(0xFF7C4DFF)  // Deep Purple
                                )
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Racha",
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(alpha = 0.99f)
                                    .drawWithCache {
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(streakBrush, blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop)
                                        }
                                    }
                            )
                        }
                        Text(
                            text = "$currentStreak",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            style = androidx.compose.ui.text.TextStyle(brush = streakBrush)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier
                                .liquidGlassModifier(CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.home_physical_profile),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .liquidGlassModifier(CircleShape)
                                .size(40.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.home_general_settings),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Supercard: daily calories, streak, weight and routine launcher
            item {
                val lastSessionForCals = sessions.maxByOrNull { it.dateMillis }
                val burnedCals = lastSessionForCals?.burnedCalories ?: 0

                HomeDailySupercard(
                    modifier = Modifier.fillMaxWidth(),
                    consumedCalories = consumedCalories,
                    burnedCalories = burnedCals,
                    targetCalories = targetCalories,
                    settings = settings
                )
            }

            // Coach Chat module
            item {
                var chatInput by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassModifier(RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Coach AI",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Coach AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.createNewChat() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, "Nuevo Chat", tint = TextSecundario)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showCoachHistorySheet = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.History, "Historial", tint = TextSecundario)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { isCoachExpanded = !isCoachExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isCoachExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimizar",
                                tint = TextSecundario
                            )
                        }
                    }

                    if (isCoachExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                    // Conversational Bubble FIFO elements
                    if (chatMessages.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            chatMessages.forEach { msg ->
                                if (msg.role == "user") {
                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .background(BorderColorSubtle, RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(text = msg.content, color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .metricCellGlassModifier(RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                                                .padding(10.dp)
                                        ) {
                                            val textContent = msg.content
                                            val parts = textContent.split(Regex("\\*\\*(.*?)\\*\\*"))
                                            val matches = Regex("\\*\\*(.*?)\\*\\*").findAll(textContent).toList()
                                            
                                            androidx.compose.material3.Text(
                                                text = androidx.compose.ui.text.buildAnnotatedString {
                                                    parts.forEachIndexed { index, part ->
                                                        append(part)
                                                        if (index < matches.size) {
                                                            withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = AccentPrimary)) {
                                                                append(matches[index].groupValues[1])
                                                            }
                                                        }
                                                    }
                                                },
                                                color = Color.White, fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (chatLoading) {
                        LinearProgressIndicator(
                            color = Color.White,
                            trackColor = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text(stringResource(R.string.chat_placeholder), color = TextSecundario, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("coach_chat_input")
                                .liquidGlassModifier(RoundedCornerShape(12.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                cursorColor = Color.White,
                                focusedContainerColor = Color(0xFF1C1C1E),
                                unfocusedContainerColor = Color(0xFF1C1C1E)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val thinkingStr = stringResource(R.string.chat_thinking)
                        IconButton(
                            onClick = {
                                if (chatInput.trim().isNotEmpty()) {
                                    viewModel.askCoach(chatInput, thinkingStr)
                                    chatInput = ""
                                }
                            },
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .size(56.dp)
                                .testTag("send_question_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    } // end isCoachExpanded
                }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

    }

        
        // Bottom Sheet: Weight History
        if (showWeightHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showWeightHistorySheet = false },
                containerColor = Color.Transparent,
                dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.supercardGlassModifier(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                    val weightEntries by viewModel.weightEntries.collectAsState()
                    WeightHistorySheetContent(weightEntries = weightEntries, onClear = {
                        viewModel.clearWeightHistory()
                    })
                }
            }
        }
        
        // Bottom Sheet: Customize IA / Coach Setup
        if (showPersonalizeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPersonalizeSheet = false },
                containerColor = Color.Transparent,
                dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.supercardGlassModifier(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                    CoachSetupContent(
                        settings = settings,
                        onSave = { updated ->
                            viewModel.updateSettings(updated)
                            showPersonalizeSheet = false
                        }
                    )
                }
            }
        }

        // Bottom Sheet: Coach History
        if (showCoachHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showCoachHistorySheet = false },
                containerColor = Color.Transparent,
                dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.supercardGlassModifier(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                    CoachHistorySheetContent(
                        chatSessions = chatSessions,
                        onSelectSession = { 
                            viewModel.loadChatSession(it.id)
                            showCoachHistorySheet = false 
                        },
                        onDeleteSession = { viewModel.deleteChatSession(it.id) },
                        onNewChat = { 
                            viewModel.createNewChat()
                            showCoachHistorySheet = false 
                        }
                    )
                }
            }
        }

        // Bottom Sheet: Select Saved Routine to Start
        if (showStartWorkoutSheet) {
            ModalBottomSheet(
                onDismissRequest = { showStartWorkoutSheet = false },
                containerColor = Color.Transparent,
                dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.supercardGlassModifier(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                    StartWorkoutMenuContent(
                        routines = routines,
                        onSelectRoutine = { r ->
                            showStartWorkoutSheet = false
                            onStartActiveWorkout(r)
                        },
                        onSelectCustom = {
                            showStartWorkoutSheet = false
                            onStartCustomWorkout()
                        }
                    )
                }
            }
        }

        // Bottom Sheet: Profile Setup
        if (showProfileSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProfileSheet = false },
                containerColor = Color.Transparent,
                dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.supercardGlassModifier(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                    ProfileSetupContent(
                        settings = settings,
                        onSave = { updated ->
                            viewModel.updateSettings(updated)
                            showProfileSheet = false
                        }
                    )
                }
            }
        }

    }
}

@Composable
private fun HomeDailySupercard(
    modifier: Modifier = Modifier,
    consumedCalories: Int,
    burnedCalories: Int,
    targetCalories: Int,
    settings: FitSettings
) {
    val green = AccentPrimary
    val red = AccentRed
    val netCalories = consumedCalories - burnedCalories
    val progress = if (targetCalories > 0) {
        (netCalories.coerceAtLeast(0).toFloat() / targetCalories).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .supercardGlassModifier(RoundedCornerShape(24.dp))
            .padding(vertical = 24.dp, horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Consumed
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = green, modifier = Modifier.size(24.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(stringResource(R.string.home_consumed_cals), color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "%,d kcal".format(Locale.US, consumedCalories),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Burned
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = red, modifier = Modifier.size(24.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(stringResource(R.string.home_burned_cals), color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "%,d kcal".format(Locale.US, burnedCalories),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            SupercardProgressRing(
                progress = progress,
                netCalories = netCalories,
                color = green,
                modifier = Modifier.size(130.dp)
            )
        }
    }
}

@Composable
private fun SupercardProgressRing(
    progress: Float,
    netCalories: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color.White.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "%,d".format(Locale.US, netCalories),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 26.sp
            )
            Text(
                text = "kcal netas",
                color = TextSecundario,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CoachSetupContent(
    settings: FitSettings,
    onSave: (FitSettings) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(settings.iaProvider) }
    
    val providers = listOf("Gemini", "Groq", "DeepSeek", "OpenAI")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.home_personalize_coach),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.home_personalize_coach_desc),
            fontSize = 12.sp,
            color = TextSecundario,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Provider Selector
        Text(text = stringResource(R.string.home_ai_provider), fontSize = 13.sp, color = TextSecundario)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            providers.forEach { provider ->
                val isSelected = provider == selectedProvider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.97f))
                            } else {
                                Modifier.metricCellGlassModifier(RoundedCornerShape(12.dp))
                            }
                        )
                        .bounceClick { selectedProvider = provider }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSave(
                    settings.copy(
                            iaProvider = selectedProvider
                        )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_coach_setup_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.settings_save), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ProfileSetupContent(
    settings: com.example.data.database.FitSettings,
    onSave: (com.example.data.database.FitSettings) -> Unit
) {
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
    val goalsList = listOf(stringResource(R.string.goal_hypertrophy), stringResource(R.string.goal_fat_loss), stringResource(R.string.goal_strength), stringResource(R.string.goal_maintenance), stringResource(R.string.prog_manual))
    var selectedGoal by remember { mutableStateOf(settings.fitnessGoal) }
    
    var activityLevel by remember { mutableStateOf(settings.activityLevel) }
    var activityExpanded by remember { mutableStateOf(false) }

    val calculatedCalories = remember(currentAge, currentHeight, currentWeight, gender, activityLevel, selectedGoal) {
        if (currentAge <= 0 || currentHeight <= 0.0 || currentWeight <= 0.0) return@remember 0
        
        val heightInCm = if (heightUnit == "in") currentHeight * 2.54 else currentHeight
        val weightInKg = if (weightUnit == "lb") currentWeight / 2.20462 else currentWeight
        val bmr = (10.0 * weightInKg) + (6.25 * heightInCm) - (5.0 * currentAge) + if (gender == "Hombre") 5.0 else -161.0
        val pal = when {
            activityLevel.contains("Sedentario") -> 1.2
            activityLevel.contains("Ligero") -> 1.375
            activityLevel.contains("Moderado") -> 1.55
            activityLevel.contains("Activo") -> 1.725
            activityLevel.contains("Muy Activo") -> 1.9
            else -> 1.2
        }
        val tdee = bmr * pal
        val goalMultiplier = when {
            selectedGoal.contains("Hipertrofia") -> 1.15
            selectedGoal.contains("Pérdida de Grasa") -> 0.85
            selectedGoal.contains("Fuerza") -> 1.10
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .imePadding()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.home_hello_user, settings.username),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            style = androidx.compose.ui.text.TextStyle(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color.White, AccentPrimary)
                )
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_physical_profile),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.home_profile_desc),
            fontSize = 12.sp,
            color = TextSecundario
        )
        
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
                        if (gender == context.getString(R.string.auth_gender_m)) {
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.97f))
                        } else {
                            Modifier.metricCellGlassModifier(RoundedCornerShape(8.dp))
                        }
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
                        if (gender == context.getString(R.string.auth_gender_f)) {
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.97f))
                        } else {
                            Modifier.metricCellGlassModifier(RoundedCornerShape(8.dp))
                        }
                    )
                    .clickable { gender = context.getString(R.string.auth_gender_f) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.auth_gender_f), color = if (gender == context.getString(R.string.auth_gender_f)) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Age, Height, Weight fields
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.home_age), fontSize = 13.sp, color = TextSecundario)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().metricCellGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it.filter { char -> char.isDigit() }.take(3) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.home_height_cm).replace("cm", heightUnit) + " ▼", fontSize = 13.sp, color = TextSecundario, modifier = Modifier.clickable { heightUnit = if (heightUnit == "cm") "in" else "cm" }.padding(vertical = 2.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().metricCellGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it.filter { char -> char.isDigit() || char == '.' }.take(5) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.home_weight_kglb).replace("kg/lb", weightUnit) + " ▼", fontSize = 13.sp, color = TextSecundario, modifier = Modifier.clickable { weightUnit = if (weightUnit == "kg") "lb" else "kg" }.padding(vertical = 2.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().metricCellGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it.filter { char -> char.isDigit() || char == '.' }.take(5) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Text(
            text = "Las unidades de medida se pueden cambiar más adelante en los Ajustes.",
            fontSize = 11.sp,
            color = TextSecundario,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Activity level dropdown
        Text(text = stringResource(R.string.home_weekly_activity), fontSize = 13.sp, color = TextSecundario)
        Spacer(modifier = Modifier.height(6.dp))
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
                            if (isSelected) {
                                Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.97f))
                            } else {
                                Modifier.metricCellGlassModifier(RoundedCornerShape(12.dp))
                            }
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

        Spacer(modifier = Modifier.height(16.dp))
        
        // Target Calories
        if (selectedGoal == context.getString(R.string.prog_manual)) {
            Text(text = stringResource(R.string.home_daily_cal_manual), fontSize = 13.sp, color = TextSecundario)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth().metricCellGlassModifier(RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (targetCaloriesStr.isEmpty()) {
                    Text(stringResource(R.string.auth_cals_placeholder), color = TextSecundario, fontSize = 12.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = targetCaloriesStr,
                    onValueChange = { targetCaloriesStr = it.filter { char -> char.isDigit() }.take(4) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Text(text = stringResource(R.string.home_daily_cal_auto), fontSize = 13.sp, color = TextSecundario)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .metricCellGlassModifier(RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (calculatedCalories > 0) stringResource(R.string.home_kcal_per_day, calculatedCalories) else stringResource(R.string.home_missing_data),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Fitness Goal Selector
        Text(text = stringResource(R.string.home_main_fitness_goal), fontSize = 13.sp, color = TextSecundario)
        Spacer(modifier = Modifier.height(6.dp))
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
                            if (isSelected) {
                                Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.97f))
                            } else {
                                Modifier.metricCellGlassModifier(RoundedCornerShape(12.dp))
                            }
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

        Spacer(modifier = Modifier.height(16.dp))

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

                onSave(
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.settings_save), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun StartWorkoutMenuContent(
    routines: List<Routine>,
    onSelectRoutine: (Routine) -> Unit,
    onSelectCustom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.home_start_workout_session),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.home_start_workout_desc),
            fontSize = 12.sp,
            color = TextSecundario,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Option 1: Custom quick workout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .metricCellGlassModifier(RoundedCornerShape(16.dp))
                .bounceClick { onSelectCustom() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Custom Quick",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_free_session),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.home_free_session_desc),
                    fontSize = 12.sp,
                    color = TextSecundario
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_your_routine_templates),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecundario,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (routines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.home_no_templates),
                    color = TextSecundario,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                routines.forEach { routine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .metricCellGlassModifier(RoundedCornerShape(16.dp))
                            .bounceClick { onSelectRoutine(routine) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .metricCellGlassModifier(CircleShape)
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = "Routine",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = routine.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                if (routine.description.isNotEmpty()) {
                                    Text(
                                        text = routine.description,
                                        fontSize = 11.sp,
                                        color = TextSecundario
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Start",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun WeightHistorySheetContent(weightEntries: List<WeightEntry>, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial de Peso",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (weightEntries.isNotEmpty()) {
                Text(
                    text = "Limpiar",
                    fontSize = 14.sp,
                    color = AccentPrimary,
                    modifier = Modifier.bounceClick { onClear() }.padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (weightEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No hay registros", color = TextSecundario, fontSize = 14.sp)
            }
        } else {
            val format = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
            val sorted = weightEntries.sortedByDescending { it.dateMillis }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sorted.size) { index ->
                    val entry = sorted[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = format.format(java.util.Date(entry.dateMillis)), color = TextSecundario, fontSize = 14.sp)
                        Text(text = "${entry.weight} kg", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}



@Composable
fun CoachHistorySheetContent(
    chatSessions: List<com.example.data.database.ChatSession>,
    onSelectSession: (com.example.data.database.ChatSession) -> Unit,
    onDeleteSession: (com.example.data.database.ChatSession) -> Unit,
    onNewChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial Coach AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            androidx.compose.material3.TextButton(onClick = onNewChat) {
                Text(text = "Nuevo Chat", color = AccentPrimary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (chatSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text(text = "No hay historial de chats.", color = TextSecundario, fontSize = 14.sp)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatSessions.size) { index ->
                    val session = chatSessions[index]
                    val format = java.text.SimpleDateFormat("dd MMM yy", java.util.Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { onSelectSession(session) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = format.format(java.util.Date(session.dateMillis)), color = TextSecundario, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { onDeleteSession(session) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = AccentRed.copy(alpha = 0.85f))
                        }
                    }
                }
            }
        }
    }
}
