package com.example


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FitnessViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: FitnessViewModel = viewModel()
                val isInitializing by viewModel.isInitializing.collectAsState()
                val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
                val activeSession by viewModel.activeSession.collectAsState()

                // Custom navigation state
                var currentMainTab by remember { mutableStateOf("home") }
                var activeMealDetailCategory by remember { mutableStateOf<String?>(null) }
                var activeMealDetailDate by remember { mutableStateOf<String?>(null) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isProfileOpen by remember { mutableStateOf(false) }
                var isWorkoutMinimized by remember { mutableStateOf(false) }
                var isShowingProfileLoading by remember { mutableStateOf(false) }

                val islandPadding by animateDpAsState(
                    targetValue = if (activeSession != null && isWorkoutMinimized) 34.dp else 0.dp,
                    label = "island_padding_anim"
                )

                // Check deep link intents for widgets
                LaunchedEffect(intent) {
                    handleDeepLinkIntent(intent) { route ->
                        when {
                            route.startsWith("add-meal:") -> {
                                currentMainTab = "nutrition"
                                activeMealDetailCategory = route.removePrefix("add-meal:")
                                activeMealDetailDate = getFormattedToday()
                            }
                            route == "add-meal-general" -> {
                                currentMainTab = "nutrition"
                                activeMealDetailCategory = "Desayuno" // Default category, user can change inside
                                activeMealDetailDate = getFormattedToday()
                            }
                            route == "tab-plan" -> {
                                currentMainTab = "plan"
                            }
                            route == "tab-progress" -> {
                                currentMainTab = "progress"
                            }
                            route == "ask-ai" -> {
                                // TODO: We can open settings or AI setup sheet, wait, let's open settings for now
                                isSettingsOpen = true
                            }
                        }
                    }
                }

                val mealCategory = activeMealDetailCategory
                val mealDate = activeMealDetailDate

                // Handle system back button for all navigation states
                BackHandler(enabled = isSettingsOpen) {
                    isSettingsOpen = false
                }
                BackHandler(enabled = isProfileOpen) {
                    isProfileOpen = false
                }
                BackHandler(enabled = mealCategory != null) {
                    activeMealDetailCategory = null
                    activeMealDetailDate = null
                }
                BackHandler(enabled = currentMainTab != "home" && mealCategory == null && !isSettingsOpen && !isProfileOpen) {
                    currentMainTab = "home"
                }

                SynergyBackground {
                    if (isInitializing) {
                        // Empty loading screen (black/background color)
                        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
                    } else if (!isUserLoggedIn) {
                        // Registration / Landing
                        AuthScreen(
                            onCheckExistingProfile = {
                                viewModel.checkAndDownloadProfileSuspend()
                            },
                            onExistingLogin = {
                                // MainActivity automáticamente cambiará de pantalla al detectar isUserLoggedIn = true
                            },
                            onLoginSuccess = { name, isLbs, gender, age, height, activity, weight, targetCals, goal ->
                                val weightUnit = if (isLbs) "lb" else "kg"
                                val heightUnit = "cm"
                                isShowingProfileLoading = true
                                viewModel.loginLocalUser(name, weightUnit, heightUnit, gender, age, height, activity, weight, targetCals, goal)
                            }
                        )
                    } else if (isShowingProfileLoading) {
                        ProfileCreationLoadingScreen(
                            onFinished = {
                                isShowingProfileLoading = false
                            }
                        )
                    } else if (activeSession != null && !isWorkoutMinimized) {
                        // High-Priority fullscreen tracker
                        ActiveWorkoutScreen(
                            viewModel = viewModel,
                            onMinimize = {
                                isWorkoutMinimized = true
                            },
                            onFinish = {
                                isWorkoutMinimized = false
                                // returns to plan routines
                                currentMainTab = "plan"
                            }
                        )
                    } else if (isSettingsOpen) {
                        // Fullscreen Settings
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { isSettingsOpen = false }
                        )
                    } else if (isProfileOpen) {
                        // Fullscreen Profile
                        ProfileScreen(
                            viewModel = viewModel,
                            onBack = { isProfileOpen = false }
                        )
                    } else if (mealCategory != null && mealDate != null) {
                        // Fullscreen nutrition log
                        MealDetailsScreen(
                            category = mealCategory,
                            dateString = mealDate,
                            viewModel = viewModel,
                            onBack = {
                                activeMealDetailCategory = null
                                activeMealDetailDate = null
                            }
                        )
                    } else {
                        // Standard view with Bottom Tab Navigation amoled bar
                        Scaffold(
                            containerColor = Color.Transparent,
                            bottomBar = {
                                val isImeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
                                if (!isImeVisible) {
                                    Box(
                                    modifier = Modifier
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .supercardGlassModifier(RoundedCornerShape(40.dp))
                                ) {
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.clip(RoundedCornerShape(40.dp)),
                                        windowInsets = WindowInsets(0.dp)
                                    ) {
                                        NavigationBarItem(
                                            selected = currentMainTab == "home",
                                            onClick = { currentMainTab = "home" },
                                            icon = { TabBarIcon(selected = currentMainTab == "home", icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Inicio") }) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.Black,
                                                unselectedIconColor = TextSecundario,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentMainTab == "nutrition",
                                            onClick = { currentMainTab = "nutrition" },
                                            icon = { TabBarIcon(selected = currentMainTab == "nutrition", icon = { Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = "Nutrición") }) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.Black,
                                                unselectedIconColor = TextSecundario,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentMainTab == "plan",
                                            onClick = { currentMainTab = "plan" },
                                            icon = { TabBarIcon(selected = currentMainTab == "plan", icon = { Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = "Rutinas") }) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.Black,
                                                unselectedIconColor = TextSecundario,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentMainTab == "progress",
                                            onClick = { currentMainTab = "progress" },
                                            icon = { TabBarIcon(selected = currentMainTab == "progress", icon = { Icon(imageVector = Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Progreso") }) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.Black,
                                                unselectedIconColor = TextSecundario,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentMainTab == "calendar",
                                            onClick = { currentMainTab = "calendar" },
                                            icon = { TabBarIcon(selected = currentMainTab == "calendar", icon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Historial") }) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.Black,
                                                unselectedIconColor = TextSecundario,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AnimatedContent(
                                    modifier = Modifier.padding(top = islandPadding),
                                    targetState = currentMainTab,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                    },
                                    label = "tab_transition"
                                ) { targetTab ->
                                    when (targetTab) {
                                        "home" -> HomeScreen(
                                            viewModel = viewModel,
                                            onNavigateToSettings = { isSettingsOpen = true },
                                            onOpenProfile = { isProfileOpen = true },
                                            onStartActiveWorkout = { routine ->
                                                viewModel.startActiveWorkout(routine)
                                                isWorkoutMinimized = false
                                            },
                                            onStartCustomWorkout = {
                                                viewModel.startCustomActiveWorkout()
                                                isWorkoutMinimized = false
                                            },
                                            onNavigateToNutrition = { currentMainTab = "nutrition" }
                                        )
                                        "nutrition" -> NutritionHomeScreen(
                                            viewModel = viewModel,
                                            onNavigateToMealDetails = { category, date ->
                                                activeMealDetailCategory = category
                                                activeMealDetailDate = date
                                            }
                                        )
                                        "plan" -> PlanDaysScreen(
                                            viewModel = viewModel,
                                            onStartRoutineWorkout = { routine ->
                                                viewModel.startActiveWorkout(routine)
                                            }
                                        )
                                        "progress" -> ProgressScreen(
                                            viewModel = viewModel
                                        )
                                        "calendar" -> CalendarScreen(
                                            viewModel = viewModel
                                        )
                                    }
                                }

                                // Dynamic Island for Active Workout
                                val currentSession = activeSession
                                if (currentSession != null && isWorkoutMinimized) {
                                    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
                                    val h = elapsedSeconds / 3600
                                    val m = (elapsedSeconds % 3600) / 60
                                    val s = elapsedSeconds % 60
                                    val timeStr = if (h > 0) String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s) else String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.93f), RoundedCornerShape(32.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                                                .clip(RoundedCornerShape(32.dp))
                                                .bounceClick { isWorkoutMinimized = false }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.main_training_status, currentSession.routineName, timeStr),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

    private fun handleDeepLinkIntent(intent: Intent?, onNavigate: (String) -> Unit) {
        if (intent == null) return
        val isFromSystem = intent.`package` == null ||
            intent.`package` == "android" ||
            packageManager.getLaunchIntentForPackage(intent.`package` ?: "") != null
        if (!isFromSystem) return

        val action = intent.action
        val data: Uri? = intent.data
        if (Intent.ACTION_VIEW == action && data != null) {
            if ("startfit" == data.scheme) {
                when (data.host) {
                    "add-meal" -> {
                        val cat = data.getQueryParameter("category")?.take(50) ?: "Desayuno"
                        onNavigate("add-meal:$cat")
                    }
                    "add-meal-general" -> onNavigate("add-meal-general")
                    "tab-plan" -> onNavigate("tab-plan")
                    "tab-progress" -> onNavigate("tab-progress")
                    "ask-ai" -> onNavigate("ask-ai")
                }
            }
        }
    }
}

@Composable
private fun TabBarIcon(
    selected: Boolean,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(
                if (selected) Modifier.background(Color.White, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
