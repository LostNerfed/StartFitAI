package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Session
import com.example.data.database.SessionLog
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import com.example.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: FitnessViewModel
) {
    val sessions by viewModel.sessions.collectAsState()
    val logs by viewModel.allLogs.collectAsState()

    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedWorkDate by remember { mutableStateOf<Calendar>(Calendar.getInstance()) }
    var expandedSessions by remember { mutableStateOf(setOf<String>()) }

    val formattedSelectedDateString = remember(selectedWorkDate) {
        val format = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        format.format(selectedWorkDate.time)
    }

    // Sessions logged on the selected calendar date
    val selectedDaySessions = remember(sessions, formattedSelectedDateString) {
        sessions.filter {
            val sDateStr = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date(it.dateMillis))
            sDateStr == formattedSelectedDateString
        }
    }

    // Days with saved workouts in the visible month
    val daysWithWorkoutsInMonthSet = remember(sessions, calendarMonth) {
        val set = mutableSetOf<Int>()
        val formatMonth = SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val visibleMonthStr = formatMonth.format(calendarMonth.time)

        sessions.forEach { s ->
            val sMonthStr = SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(Date(s.dateMillis))
            if (sMonthStr == visibleMonthStr) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = s.dateMillis
                set.add(cal.get(Calendar.DAY_OF_MONTH))
            }
        }
        set
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
                text = stringResource(R.string.cal_training_history),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Calendar header & Month navigator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formatMonthHeader = SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                val monthTitle = formatMonthHeader.format(calendarMonth.time).replaceFirstChar { it.uppercase() }

                Text(
                    text = monthTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val nextMock = calendarMonth.clone() as Calendar
                            nextMock.add(Calendar.MONTH, -1)
                            calendarMonth = nextMock
                        },
                        modifier = Modifier.liquidGlassModifier(CircleShape).size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.cal_prev_month), tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            val nextMock = calendarMonth.clone() as Calendar
                            nextMock.add(Calendar.MONTH, 1)
                            calendarMonth = nextMock
                        },
                        modifier = Modifier.liquidGlassModifier(CircleShape).size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = stringResource(R.string.cal_next_month), tint = Color.White)
                    }
                }
            }
        }

        // Calendar Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .supercardGlassModifier(RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                // Day initials
                Row(modifier = Modifier.fillMaxWidth()) {
                    val initials = stringArrayResource(R.array.cal_day_initials).toList()
                    initials.forEach { d ->
                        Text(
                            text = d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grid mapping dates
                val datesGrid = remember(calendarMonth) { getCalendarMonthGridDates(calendarMonth) }
                datesGrid.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        week.forEach { calVal ->
                            if (calVal == null) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val dayNum = calVal.get(Calendar.DAY_OF_MONTH)
                                val hasWorkout = daysWithWorkoutsInMonthSet.contains(dayNum)

                                val calCompareFormat = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val isSelected = calCompareFormat.format(calVal.time) == formattedSelectedDateString

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(4.dp)
                                        .background(
                                            when {
                                                isSelected -> Color.White.copy(alpha = 0.97f)
                                                hasWorkout -> AccentRed.copy(alpha = 0.17f)
                                                else -> Color.Transparent
                                            },
                                            CircleShape
                                        )
                                        .clip(CircleShape)
                                        .clickable { selectedWorkDate = calVal },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                        if (hasWorkout && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(AccentRed, CircleShape)
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

        // Details of selected Day below
        item {
            val displayTitleDate = SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale.getDefault()).format(selectedWorkDate.time).replaceFirstChar { it.uppercase() }
            Text(
                text = stringResource(R.string.cal_sessions_of, displayTitleDate),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecundario,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (selectedDaySessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .supercardGlassModifier(RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.cal_no_workouts),
                        color = TextSecundario,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(selectedDaySessions) { session ->
                val sessionLogs = logs.filter { it.sessionId == session.id }
                val isExpanded = expandedSessions.contains(session.id)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .supercardGlassModifier(RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            expandedSessions = if (isExpanded) expandedSessions.minus(session.id) else expandedSessions.plus(session.id)
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = stringResource(R.string.cal_active_workout),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = session.routineName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .metricCellGlassModifier(RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.cal_duration_min, session.durationMinutes),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecundario,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Group logs by exercise for neat nested representation
                            val groupedLogs = sessionLogs.groupBy { it.exerciseName }
                            groupedLogs.forEach { (exName, list) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = exName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Print neat sets vertical list inside a liquid glass container
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .metricCellGlassModifier(RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        list.reversed().forEach { sLog ->
                                            val logLabel = if (sLog.isDropset) stringResource(R.string.cal_dropset) else stringResource(R.string.cal_set_index, sLog.setIndex)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = logLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextSecundario
                                                )
                                                Text(
                                                    text = stringResource(R.string.cal_weight_reps, viewModel.formatDisplayWeight(sLog.weightKg), viewModel.getUnitString().lowercase(), sLog.reps),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
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

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

// Generate Calendar Days list for grid
fun getCalendarMonthGridDates(calendar: Calendar): List<Calendar?> {
    val list = mutableListOf<Calendar?>()
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday
    // Add null items for offset weeks
    for (i in 1 until firstDayOfWeek) {
        list.add(null)
    }

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..maxDays) {
        val dateCal = cal.clone() as Calendar
        dateCal.set(Calendar.DAY_OF_MONTH, i)
        list.add(dateCal)
    }

    // Pad with nulls to multiple of 7 to prevent stretching on the last row
    while (list.size % 7 != 0) {
        list.add(null)
    }

    return list
}
