package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.platform.LocalContext
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Session
import com.example.data.database.SessionLog
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import com.example.ui.theme.AppTextStyle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressScreen(
    viewModel: FitnessViewModel
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState()
    val logs by viewModel.allLogs.collectAsState()


    var visibleLimit by remember { mutableStateOf(5) }

    // 1. Calculate Grid 2x2 Stats
    val totalVolume = logs.sumOf { it.weightKg * it.reps }
    val uniqueExercisesCount = logs.map { it.exerciseName.lowercase().trim() }.distinct().size

    // Weekly session count
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val oneWeekAgo = calendar.timeInMillis
    val weeklySessionsCount = sessions.filter { it.dateMillis >= oneWeekAgo }.size

    // Active days and streaks
    val datesTrained = sessions.map {
        SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date(it.dateMillis))
    }.distinct().sortedDescending()

    val activeDaysCount = datesTrained.size
    val activeStreak = calculateStreak(datesTrained)

    // 2. Personal Records (Based on Volume: weight x reps)
    val personalRecordsMap = remember(logs) {
        val records = mutableMapOf<String, RecordData>()
        logs.forEach { log ->
            val exName = log.exerciseName
            val existing = records[exName]
            val currentWeight = log.weightKg
            val currentReps = log.reps
            val isNewPR = existing == null || 
                          currentWeight > existing.weight || 
                          (currentWeight == existing.weight && currentReps > existing.reps)
            if (isNewPR) {
                records[exName] = RecordData(
                    weight = currentWeight,
                    reps = currentReps,
                    dateMillis = sessions.firstOrNull { it.id == log.sessionId }?.dateMillis ?: System.currentTimeMillis()
                )
            }
        }
        records.entries.toList().sortedWith(compareByDescending<Map.Entry<String, RecordData>> { it.value.weight }.thenByDescending { it.value.reps })
    }

    // 3. Exercise lists
    val exercisesGroupedByLogName = remember(logs) {
        logs.groupBy { it.exerciseName }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Title
        item {
            Text(
                text = stringResource(R.string.prog_analytics),
                style = AppTextStyle.headlineOswald.copy(color = Color.White)
            )
        }
        // Heatmap Matrix
        item {
            val targetTotalTrainingsThisYear = sessions.count { 
                java.time.Instant.ofEpochMilli(it.dateMillis).atZone(java.time.ZoneId.systemDefault()).year == java.time.LocalDate.now().year
            }
            
            val today = java.time.LocalDate.now()
            val trainingDates = sessions.map { 
                java.time.Instant.ofEpochMilli(it.dateMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }.toSet()
            
            val daysPassed = today.dayOfYear
            val targetCompletionPercentage = if (daysPassed > 0) ((targetTotalTrainingsThisYear.toFloat() / daysPassed) * 100).toInt() else 0
            
            var animateStats by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { animateStats = true }
            
            val totalTrainingsThisYear by animateIntAsState(
                targetValue = if (animateStats) targetTotalTrainingsThisYear else 0,
                animationSpec = tween(800),
                label = "trainings_anim"
            )
            
            val completionPercentage by animateIntAsState(
                targetValue = if (animateStats) targetCompletionPercentage else 0,
                animationSpec = tween(800),
                label = "completion_anim"
            )
            
            val monthsToDisplay = remember {
                val list = mutableListOf<java.time.YearMonth>()
                var ym = java.time.YearMonth.now()
                list.add(ym)
                ym = ym.minusMonths(1)
                list.add(ym)
                ym = ym.minusMonths(1)
                list.add(ym)
                list.reverse()
                list
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .supercardGlassModifier(RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                // Compact Header and Stats
                Box(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text = stringResource(R.string.prog_days_this_year, totalTrainingsThisYear), fontSize = 11.sp, color = TextSecundario)
                            Text(text = stringResource(R.string.prog_completion, completionPercentage), fontSize = 11.sp, color = TextSecundario)
                        }
                    }
                }
                
                // Heatmap: months side by side, day labels only on the first
                val dayLabels = listOf(
                    stringResource(R.string.day_sun),
                    stringResource(R.string.day_mon),
                    stringResource(R.string.day_tue),
                    stringResource(R.string.day_wed),
                    stringResource(R.string.day_thu),
                    stringResource(R.string.day_fri),
                    stringResource(R.string.day_sat)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    monthsToDisplay.forEachIndexed { index, ym ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Month title centered
                            Text(
                                text = ym.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                                style = AppTextStyle.statSmall.copy(color = Color.White),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Row(verticalAlignment = Alignment.Top) {
                                // Day labels only on the first month
                                if (index == 0) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        dayLabels.forEach { label ->
                                            Box(
                                                modifier = Modifier.size(14.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = AppTextStyle.statSmall.copy(color = TextSecundario),
                                                    lineHeight = 9.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Grid cells
                                val firstDayOfMonth = ym.atDay(1)
                                val lastDayOfMonth = ym.atEndOfMonth()
                                val firstSunday = firstDayOfMonth.minusDays((if (firstDayOfMonth.dayOfWeek.value == 7) 0 else firstDayOfMonth.dayOfWeek.value).toLong())
                                val lastSaturday = lastDayOfMonth.plusDays((6 - (if (lastDayOfMonth.dayOfWeek.value == 7) 0 else lastDayOfMonth.dayOfWeek.value)).toLong())
                                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstSunday, lastSaturday).toInt() + 1
                                val weeksInMonth = daysBetween / 7
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    for (w in 0 until weeksInMonth) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            for (d in 0..6) {
                                                val cellDate = firstSunday.plusDays((w * 7 + d).toLong())
                                                
                                                if (cellDate.monthValue != ym.monthValue) {
                                                    Box(modifier = Modifier.size(14.dp))
                                                } else if (cellDate.isAfter(today)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .background(SurfaceDark, RoundedCornerShape(3.dp))
                                                    )
                                                } else {
                                                    val didTrain = trainingDates.contains(cellDate)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .background(
                                                                color = if (didTrain) AccentRed else Color(0xFF2C2C2E),
                                                                shape = RoundedCornerShape(3.dp)
                                                            )
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
                
            }
        }

        // Analíticas de Volumen y Series integradas en el Mapa Muscular
        item {
            val volumeConverted = if (totalVolume > 0) viewModel.formatDisplayWeight(totalVolume) else "--"
            val totalSets = logs.size
            
                val heatmapData by viewModel.heatmapData.collectAsState()
                val muscleVolumes = remember(heatmapData) {
                    val map = mutableMapOf<String, Double>()
                    heatmapData.forEach {
                        map[it.muscleGroup] = it.accumulatedVolume
                    }
                    map
                }
                
                val topMuscle = muscleVolumes.maxByOrNull { it.value }
                val topMuscleName = topMuscle?.key?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } ?: "N/A"
                val topMuscleVolumeStr = viewModel.formatDisplayWeight(topMuscle?.value ?: 0.0)

                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })

                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        when(page) {
                            0 -> {
                                MuscleHeatmap(
                                    muscleVolumes = muscleVolumes,
                                    volumeConverted = volumeConverted,
                                    totalSets = totalSets,
                                    volumeUnit = viewModel.getUnitString().lowercase(),
                                    modifier = Modifier
                                        .height(280.dp)
                                        .padding(bottom = 12.dp)
                                )
                            }
                            1 -> {
                                val sortedSessions = sessions.sortedBy { it.dateMillis }
                                var runningTotal = 0.0
                                val cumulativeData = sortedSessions.map { session ->
                                    val sessionVolume = logs.filter { it.sessionId == session.id }.sumOf { 
                                        val w = if (it.weightKg > 0.0) it.weightKg else 20.0
                                        w * it.reps 
                                    }
                                    runningTotal += sessionVolume
                                    runningTotal
                                }

                                val recentCumulative = cumulativeData.takeLast(7)
                                val paddedCumulative = if (recentCumulative.size < 7) {
                                    List(7 - recentCumulative.size) { 0.0 } + recentCumulative
                                } else {
                                    recentCumulative
                                }
                                val chartEntries = paddedCumulative.mapIndexed { index, vol ->
                                    val displayVol = viewModel.getDisplayWeight(vol)
                                    entryOf(index.toFloat(), displayVol.toFloat())
                                }
                                val chartEntryModel = entryModelOf(chartEntries)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .padding(bottom = 12.dp)
                                        .supercardGlassModifier(RoundedCornerShape(20.dp))
                                        .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 4.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text(stringResource(R.string.prog_accumulated_volume), style = AppTextStyle.statSmall.copy(color = Color.White, fontSize = 14.sp))
                                        Spacer(modifier = Modifier.height(9.dp))
                                        
                                        // Vico chart natively stretched to fill the width
                                        Chart(
                                            chart = com.patrykandpatrick.vico.compose.chart.line.lineChart(
                                                lines = listOf(
                                                    com.patrykandpatrick.vico.compose.chart.line.lineSpec(
                                                        lineColor = AccentPrimary,
                                                        lineThickness = 2.dp,
                                                        lineBackgroundShader = com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders.fromBrush(
                                                            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(AccentPrimary.copy(alpha = 0.3f), Color.Transparent))
                                                        ),
                                                        point = com.patrykandpatrick.vico.compose.component.shapeComponent(
                                                            shape = com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape, 
                                                            color = Color.White, 
                                                            strokeWidth = 2.dp, 
                                                            strokeColor = AccentPrimary
                                                        ),
                                                        pointSize = 8.dp,
                                                        pointConnector = com.patrykandpatrick.vico.core.chart.DefaultPointConnector(cubicStrength = 0.5f)
                                                    )
                                                )
                                            ),
                                            model = chartEntryModel,
                                            startAxis = com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis(
                                                label = com.patrykandpatrick.vico.compose.axis.axisLabelComponent(
                                                    color = TextSecundario, 
                                                    textSize = 8.sp,
                                                    horizontalMargin = 3.dp // 3dp distance from scales
                                                ),
                                                axis = null, 
                                                tick = null, 
                                                guideline = com.patrykandpatrick.vico.compose.component.lineComponent(
                                                    color = Color.White.copy(alpha = 0.05f), 
                                                    thickness = 1.dp
                                                ),
                                                itemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Vertical.default(maxItemCount = 8),
                                                valueFormatter = com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter { value, _ -> value.toInt().toString() }
                                            ),
                                            endAxis = null,
                                            bottomAxis = com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis(
                                                label = null,
                                                tick = null,
                                                axis = null,
                                                guideline = com.patrykandpatrick.vico.compose.component.lineComponent(
                                                    color = Color.White.copy(alpha = 0.05f), 
                                                    thickness = 1.dp
                                                )
                                            ),
                                            chartScrollState = rememberChartScrollState(),
                                            chartScrollSpec = com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec(isScrollEnabled = false),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(end = 9.dp, top = 9.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(11.dp))
                                        
                                        // Top Muscle info at the bottom
                                        Text(text = "Músculo Más Trabajado:", fontSize = 12.sp, color = TextSecundario)
                                        Text(
                                            text = "$topMuscleName ($topMuscleVolumeStr ${viewModel.getUnitString().lowercase()})", 
                                            fontSize = 15.sp, 
                                            fontWeight = FontWeight.Bold, 
                                            color = AccentAmber
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dot indicators
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(2) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.DarkGray
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
        }

        // Historial de Ejercicios y Récords
        item {
            Text(
                text = stringResource(R.string.prog_exercises_records),
                style = AppTextStyle.statBig.copy(color = Color.White),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
        }

        if (personalRecordsMap.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .supercardGlassModifier(RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.prog_complete_workouts), color = TextSecundario, fontSize = 12.sp)
                }
            }
        } else {
            val sortedExercises = personalRecordsMap.toList()
            val visibleExercises = sortedExercises.take(visibleLimit)
            
            itemsIndexed(visibleExercises) { index, pair ->
                val exerciseName = pair.key
                val bestRecord = pair.value
                val currentLogs = exercisesGroupedByLogName[exerciseName] ?: emptyList()
                
                val logsBySession = currentLogs.groupBy { it.sessionId }
                val recentSessions = logsBySession.entries.sortedByDescending { sessionEntry -> 
                    sessions.firstOrNull { it.id == sessionEntry.key }?.dateMillis ?: 0L 
                }.take(3)

                var isExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .supercardGlassModifier(RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val trophyColor = when (index) {
                                0 -> AccentAmber
                                1 -> Color(0xFFC0C0C0)
                                2 -> Color(0xFFCD7F32)
                                else -> AccentAmber
                            }
                            Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Exercise", tint = trophyColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = exerciseName, 
                                    style = AppTextStyle.statBig.copy(color = Color.White),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                val dateStr = SimpleDateFormat("dd MMM, yy", java.util.Locale.getDefault()).format(Date(bestRecord.dateMillis))
                                Text(text = dateStr, fontSize = 11.sp, color = TextSecundario)
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = stringResource(R.string.prog_record), style = AppTextStyle.statSmall.copy(color = TextSecundario))
                                Text(text = stringResource(R.string.prog_record_value, viewModel.formatDisplayWeight(bestRecord.weight), viewModel.getUnitString().lowercase(), bestRecord.reps), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = Color.White
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.prog_latest_workouts),
                                fontSize = 11.sp,
                                color = TextSecundario,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            recentSessions.forEach { sessionEntry ->
                                val sessionLogs = sessionEntry.value
                                val sessionDate = sessions.firstOrNull { it.id == sessionEntry.key }?.dateMillis ?: 0L
                                val dateString = SimpleDateFormat("dd MMM, yy", java.util.Locale.getDefault()).format(Date(sessionDate))
                                
                                val bestSetOfSession = sessionLogs.maxWithOrNull(compareBy<com.example.data.database.SessionLog> { it.weightKg }.thenBy { it.reps })
                                
                                val isPRSession = bestSetOfSession != null && 
                                                  bestSetOfSession.weightKg == bestRecord.weight && 
                                                  bestSetOfSession.reps == bestRecord.reps &&
                                                  sessionDate == bestRecord.dateMillis

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidGlassModifier(RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = dateString, fontSize = 12.sp, color = TextSecundario)
                                        if (isPRSession) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = "PR", tint = AccentRed, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                    if (bestSetOfSession != null) {
                                        Text(
                                            text = stringResource(R.string.prog_best, viewModel.formatDisplayWeight(bestSetOfSession.weightKg), viewModel.getUnitString().lowercase(), bestSetOfSession.reps),
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (visibleLimit < personalRecordsMap.size) {
                item {
                    TextButton(
                        onClick = { visibleLimit += 5 },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentPrimary)
                    ) {
                        Text(text = stringResource(R.string.prog_view_more), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (personalRecordsMap.size > 5) {
                item {
                    TextButton(
                        onClick = { visibleLimit = 5 },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentPrimary)
                    ) {
                        Text(text = stringResource(R.string.prog_view_less), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .supercardGlassModifier(RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.Medium)
            Icon(imageVector = icon, contentDescription = title, tint = TextSecundario, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = AppTextStyle.numberSmall.copy(color = Color.White))
        Text(text = sub, fontSize = 10.sp, color = TextSecundario)
    }
}

// Draw a simple sparkline on Canvas for the exercise trend
@Composable
fun ProgressLineSparkline(weights: List<Double>) {
    val maxWeight = weights.maxOrNull() ?: 1.0
    val minWeight = weights.minOrNull() ?: 0.0
    val weightDiff = (maxWeight - minWeight).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        val width = size.width
        val height = size.height
        val points = mutableListOf<Offset>()

        weights.forEachIndexed { idx, wt ->
            val fractionX = idx.toFloat() / (weights.size - 1).coerceAtLeast(1)
            val x = fractionX * width

            val fractionY = (wt - minWeight) / weightDiff
            // Invert Y direction
            val y = height - (fractionY.toFloat() * height * 0.8f) - (height * 0.1f)
            points.add(Offset(x, y))
        }

        // Draw connections
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color.White,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

// Calculate streak based on consecutive unique dates
fun calculateStreak(dates: List<String>): Int {
    if (dates.isEmpty()) return 0
    val format = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val parsedDates = dates.mapNotNull {
        try { format.parse(it) } catch (e: Exception) { null }
    }.sortedDescending()

    if (parsedDates.isEmpty()) return 0

    var streak = 0
    val calMock = Calendar.getInstance()
    // Check if the latest completed training is today or yesterday
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val lastTrained = Calendar.getInstance()
    lastTrained.time = parsedDates[0]
    lastTrained.set(Calendar.HOUR_OF_DAY, 0)
    lastTrained.set(Calendar.MINUTE, 0)
    lastTrained.set(Calendar.SECOND, 0)
    lastTrained.set(Calendar.MILLISECOND, 0)

    val diff = (today.timeInMillis - lastTrained.timeInMillis) / (1000 * 60 * 60 * 24)
    if (diff > 1) {
        return 0 // Streak broken because they didn't train today or yesterday
    }

    streak = 1
    calMock.time = parsedDates[0]

    for (i in 1 until parsedDates.size) {
        val prevDay = Calendar.getInstance()
        prevDay.time = parsedDates[i]

        calMock.add(Calendar.DAY_OF_YEAR, -1)

        val isConsecutive = (calMock.get(Calendar.YEAR) == prevDay.get(Calendar.YEAR)) &&
                (calMock.get(Calendar.DAY_OF_YEAR) == prevDay.get(Calendar.DAY_OF_YEAR))

        if (isConsecutive) {
            streak++
        } else {
            break
        }
    }

    return streak
}

data class RecordData(
    val weight: Double,
    val reps: Int,
    val dateMillis: Long
)

