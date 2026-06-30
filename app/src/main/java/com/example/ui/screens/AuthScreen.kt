package com.example.ui.screens

import kotlinx.coroutines.launch

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.BackHandler

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthViewModel
import com.example.ui.AuthState
import com.example.R

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: (name: String, isLbs: Boolean, gender: String, age: Int, height: Double, activity: String, weight: Double, targetCals: Int, fitnessGoal: String) -> Unit,
    onCheckExistingProfile: suspend () -> Boolean = { false },
    onExistingLogin: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()
    var isCheckingProfile by remember { mutableStateOf(false) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var selectedLanguageTag by rememberSaveable { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLbs by remember { mutableStateOf(false) }
    var isWeightLbs by remember { mutableStateOf(false) }
    var isHeightFt by remember { mutableStateOf(false) }
    
    val heightCmItems = (100..250).map { "$it cm" }
    val heightFtItems = mutableListOf<String>().apply {
        for (f in 3..8) {
            for (i in 0..11) {
                add("${f}'${i}\"")
            }
        }
    }
    var currentHeightCmIndex by remember { mutableStateOf(heightCmItems.indexOf("170 cm")) }
    var currentHeightFtIndex by remember { mutableStateOf(heightFtItems.indexOf("5'7\"")) }

    val weightKgItems = (30..200).map { "$it kg" }
    val weightLbItems = (60..450).map { "$it lbs" }
    var currentWeightKgIndex by remember { mutableStateOf(weightKgItems.indexOf("70 kg")) }
    var currentWeightLbIndex by remember { mutableStateOf(weightLbItems.indexOf("150 lbs")) }
    var gender by remember { mutableStateOf("Hombre") }
    var ageStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("Sedentario (0 entrenamientos/sem)") }
    val activityOptions = listOf(
        "Sedentario (0 entrenamientos/sem)",
        "Ligero (1-3 entrenamientos/sem)",
        "Moderado (3-5 entrenamientos/sem)",
        "Activo (6-7 entrenamientos/sem)",
        "Muy Activo (Doble turno o trabajo físico exigente)"
    )
    var weightStr by remember { mutableStateOf("") }
    var targetCaloriesStr by remember { mutableStateOf("") }
    val goalsList = listOf(
        "Hipertrofia", 
        "Pérdida de Grasa", 
        "Fuerza", 
        "Mantenimiento"
    )
    var selectedGoal by remember { mutableStateOf(goalsList[0]) }

    val activityOptionsMap = mapOf(
        "Sedentario (0 entrenamientos/sem)" to stringResource(R.string.activity_sedentary),
        "Ligero (1-3 entrenamientos/sem)" to stringResource(R.string.activity_light),
        "Moderado (3-5 entrenamientos/sem)" to stringResource(R.string.activity_moderate),
        "Activo (6-7 entrenamientos/sem)" to stringResource(R.string.activity_active),
        "Muy Activo (Doble turno o trabajo físico exigente)" to stringResource(R.string.activity_very_active)
    )
    val goalsMap = mapOf(
        "Hipertrofia" to stringResource(R.string.goal_hypertrophy),
        "Pérdida de Grasa" to stringResource(R.string.goal_fatloss),
        "Fuerza" to stringResource(R.string.goal_strength),
        "Mantenimiento" to stringResource(R.string.goal_maintenance)
    )

    LaunchedEffect(Unit) {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            selectedLanguageTag = locales.get(0)?.toLanguageTag() ?: ""
        }
        authViewModel.refreshAuthState()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            isCheckingProfile = true
            val exists = onCheckExistingProfile()
            isCheckingProfile = false
            if (exists) {
                onExistingLogin()
            } else {
                step = 2
            }
        }
    }

    BackHandler(enabled = step > 0) {
        step--
        error = null
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        val animatedProgress by animateFloatAsState(targetValue = (step.toFloat() + 1f) / 6f, animationSpec = tween(500))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).statusBarsPadding().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        Column(

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Logo tipográfico
            AnimatedVisibility(visible = step < 3) {
                Image(
                    painter = painterResource(id = R.drawable.start_fit_3),
                    contentDescription = "StartFit AI Logo",
                    modifier = Modifier.size(240.dp).padding(bottom = 24.dp)
                )
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "auth_steps"
            ) { currentStep ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when (currentStep) {
                        0 -> {
                            // Step 0: Selección de idioma
                            Text(
                                text = stringResource(R.string.select_language),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val languages = listOf(
                                "es" to stringResource(R.string.language_es),
                                "en" to stringResource(R.string.language_en),
                                "pt-BR" to stringResource(R.string.language_pt),
                                "fr" to stringResource(R.string.language_fr)
                            )

                            languages.forEach { (tag, langName) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .then(
                                            if (selectedLanguageTag == tag) Modifier
                                                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp))
                                            else Modifier
                                                .liquidGlassModifier(RoundedCornerShape(12.dp))
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedLanguageTag = tag
                                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                                        }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = langName, 
                                        color = if (selectedLanguageTag == tag) Color.Black else Color.White, 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(0.5.dp)
                                        .background(Color.White.copy(alpha = 0.15f))
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { step = 1 },
                                enabled = selectedLanguageTag.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .supercardGlassModifier(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    contentColor = Color.White,
                                    disabledContentColor = TextSecundario
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.continue_btn), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        1 -> {
                            // Step 1: Bienvenida local antes de iniciar el formulario
                            Text(
                                text = stringResource(R.string.auth_welcome),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )


                            // BOTON DE INICIO DE SESIÓN CON GOOGLE
                            if (authState is AuthState.Loading || isCheckingProfile) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
                                if (isCheckingProfile) {
                                    Text("Buscando tu perfil en la nube...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                                }
                            } else if (authState is AuthState.Success) {
                                Text(
                                    "¡Sesión de Google iniciada! Configurando tu perfil...",
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                Button(
                                    onClick = { authViewModel.signInWithGoogle(context) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text("Continuar con Google", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
                                }
                                
                                if (authState is AuthState.Error) {
                                    Text(
                                        text = (authState as AuthState.Error).message,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            if (authState !is AuthState.Success && !isCheckingProfile) {
                                Button(
                                    onClick = { step = 2 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .supercardGlassModifier(RoundedCornerShape(12.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Usuario anónimo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                        2 -> {
                            // Step 2: Nombre (perfil local)
                            Text(stringResource(R.string.auth_step1_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    error = null
                                },
                                placeholder = { Text(stringResource(R.string.auth_step1_placeholder), color = TextSecundario) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlassModifier(RoundedCornerShape(12.dp))
                                    .testTag("local_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            AnimatedVisibility(visible = error != null) {
                                Text(
                                    text = error ?: "",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { 
                                    if (name.trim().isEmpty()) {
                                        error = context.getString(R.string.auth_step1_error)
                                    } else {
                                        error = null
                                        step = 3
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .supercardGlassModifier(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.btn_next), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        3 -> {
                            // Step 3: Perfil Físico Completo (Género, Edad, Altura, Peso)
                            Text("Perfil físico", style = AppTextStyle.headlineOswald.copy(color = Color.White), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp))
                            
                            // Género
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { gender = "Hombre" }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(156.dp)
                                                .clip(CircleShape)
                                                .border(4.dp, if (gender == "Hombre") Color(0xFF00B0FF) else Color.Transparent, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.maleicon),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().padding(8.dp).clip(CircleShape),
                                                colorFilter = if (gender == "Hombre") null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(stringResource(R.string.auth_gender_m), style = AppTextStyle.statSmall.copy(color = Color.White))
                                    }
                                }
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { gender = "Mujer" }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(156.dp)
                                                .clip(CircleShape)
                                                .border(4.dp, if (gender == "Mujer") Color(0xFF00B0FF) else Color.Transparent, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.femaleicon),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().padding(8.dp).clip(CircleShape),
                                                colorFilter = if (gender == "Mujer") null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(stringResource(R.string.auth_gender_f), style = AppTextStyle.statSmall.copy(color = Color.White))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Edad
                            OutlinedTextField(
                                value = ageStr,
                                onValueChange = { 
                                    ageStr = it.filter { char -> char.isDigit() }.take(3) 
                                    error = null
                                },
                                placeholder = { Text(stringResource(R.string.auth_age_placeholder), color = TextSecundario, fontSize = 14.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().liquidGlassModifier(RoundedCornerShape(12.dp)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Altura
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Altura", color = Color.White, style = AppTextStyle.statBig, modifier = Modifier.weight(1f))
                                Row(
                                    modifier = Modifier.background(Color.White.copy(alpha=0.1f), RoundedCornerShape(20.dp)).padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { 
                                        if (isHeightFt) {
                                            val parts = heightFtItems[currentHeightFtIndex].split("'")
                                            val f = parts[0].toDoubleOrNull() ?: 0.0
                                            val i = parts.getOrNull(1)?.replace("\"", "")?.trim()?.toDoubleOrNull() ?: 0.0
                                            val cm = (f * 30.48) + (i * 2.54)
                                            val roundedCm = Math.round(cm).toInt()
                                            currentHeightCmIndex = heightCmItems.indexOfFirst { it == "$roundedCm cm" }.takeIf { it >= 0 } ?: 0
                                            isHeightFt = false 
                                        }
                                    }.background(if (!isHeightFt) Color.White else Color.Transparent).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("CM", color = if (!isHeightFt) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { 
                                        if (!isHeightFt) {
                                            val cm = heightCmItems[currentHeightCmIndex].replace(" cm", "").toDoubleOrNull() ?: 0.0
                                            val totalInches = cm / 2.54
                                            val f = Math.floor(totalInches / 12).toInt()
                                            val i = Math.round(totalInches % 12).toInt()
                                            val finalF = if (i == 12) f + 1 else f
                                            val finalI = if (i == 12) 0 else i
                                            val ftStr = "${finalF}'${finalI}\""
                                            currentHeightFtIndex = heightFtItems.indexOfFirst { it == ftStr }.takeIf { it >= 0 } ?: 0
                                            isHeightFt = true 
                                        }
                                    }.background(if (isHeightFt) Color.White else Color.Transparent).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("FT", color = if (isHeightFt) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isHeightFt) {
                                WheelPicker(items = heightFtItems, selectedIndex = currentHeightFtIndex, onIndexChanged = { currentHeightFtIndex = it })
                            } else {
                                WheelPicker(items = heightCmItems, selectedIndex = currentHeightCmIndex, onIndexChanged = { currentHeightCmIndex = it })
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Peso
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Peso", color = Color.White, style = AppTextStyle.statBig, modifier = Modifier.weight(1f))
                                Row(
                                    modifier = Modifier.background(Color.White.copy(alpha=0.1f), RoundedCornerShape(20.dp)).padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { 
                                        if (isWeightLbs) {
                                            val currentLbs = weightLbItems[currentWeightLbIndex].replace(" lbs", "").toDoubleOrNull() ?: 0.0
                                            val kg = currentLbs / 2.20462
                                            val roundedKg = Math.round(kg).toInt()
                                            currentWeightKgIndex = weightKgItems.indexOfFirst { it == "$roundedKg kg" }.takeIf { it >= 0 } ?: 0
                                            isWeightLbs = false
                                        }
                                    }.background(if (!isWeightLbs) Color.White else Color.Transparent).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("KG", color = if (!isWeightLbs) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { 
                                        if (!isWeightLbs) {
                                            val currentKg = weightKgItems[currentWeightKgIndex].replace(" kg", "").toDoubleOrNull() ?: 0.0
                                            val lbs = currentKg * 2.20462
                                            val roundedLbs = Math.round(lbs).toInt()
                                            currentWeightLbIndex = weightLbItems.indexOfFirst { it == "$roundedLbs lbs" }.takeIf { it >= 0 } ?: 0
                                            isWeightLbs = true
                                        }
                                    }.background(if (isWeightLbs) Color.White else Color.Transparent).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("LB", color = if (isWeightLbs) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isWeightLbs) {
                                WheelPicker(items = weightLbItems, selectedIndex = currentWeightLbIndex, onIndexChanged = { currentWeightLbIndex = it })
                            } else {
                                WheelPicker(items = weightKgItems, selectedIndex = currentWeightKgIndex, onIndexChanged = { currentWeightKgIndex = it })
                            }

                            AnimatedVisibility(visible = error != null) {
                                Text(
                                    text = error ?: "",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { 
                                    val ageInt = ageStr.toIntOrNull() ?: 0
                                    if (ageInt <= 0 || ageInt > 120) {
                                        error = context.getString(R.string.auth_err_age)
                                    } else {
                                        error = null
                                        step = 4
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .supercardGlassModifier(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.btn_next), style = AppTextStyle.titleOswald.copy(color = Color.White))
                            }
                        }
                        4 -> {
                            // Step 7: Objetivo de fitness
                            Text(stringResource(R.string.auth_step6_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                goalsList.forEach { goal ->
                                    val isSelected = goal == selectedGoal
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isSelected) Modifier
                                                    .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                                                    .clip(RoundedCornerShape(8.dp))
                                                else Modifier
                                                    .liquidGlassModifier(RoundedCornerShape(8.dp))
                                            )
                                            .clickable { selectedGoal = goal }
                                            .padding(vertical = 16.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = goalsMap[goal] ?: goal,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { step = 5 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .supercardGlassModifier(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.btn_next), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        5 -> {
                            // Step 8: Calorías objetivo + finalizar
                            Text(stringResource(R.string.auth_step7_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                            
                            OutlinedTextField(
                                value = targetCaloriesStr,
                                onValueChange = { 
                                    targetCaloriesStr = it.filter { char -> char.isDigit() }.take(4) 
                                    error = null
                                },
                                placeholder = { Text(stringResource(R.string.auth_cals_placeholder), color = TextSecundario, fontSize = 14.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().liquidGlassModifier(RoundedCornerShape(12.dp)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            AnimatedVisibility(visible = error != null) {
                                Text(
                                    text = error ?: "",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Términos
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .supercardGlassModifier(RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.auth_terms),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.auth_privacy_desc),
                                        fontSize = 11.sp,
                                        color = TextSecundario,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { 
                                    val targetCals = targetCaloriesStr.toIntOrNull() ?: 0
                                    if (targetCaloriesStr.isNotEmpty() && (targetCals <= 500 || targetCals > 8000)) {
                                        error = context.getString(R.string.auth_err_cals)
                                    } else {
                                        error = null
                                        val ageInt = ageStr.toIntOrNull() ?: 0
                                        
                                        val hDouble = if (isHeightFt) {
                                            val parts = heightFtItems[currentHeightFtIndex].split("'")
                                            val f = parts[0].toDoubleOrNull() ?: 0.0
                                            val i = parts.getOrNull(1)?.replace("\"", "")?.trim()?.toDoubleOrNull() ?: 0.0
                                            val inches = (f * 12) + i
                                            if (isWeightLbs) inches else inches * 2.54
                                        } else {
                                            val cm = heightCmItems[currentHeightCmIndex].replace(" cm", "").toDoubleOrNull() ?: 0.0
                                            if (isWeightLbs) cm / 2.54 else cm
                                        }

                                        val wDouble = if (isWeightLbs) {
                                            weightLbItems[currentWeightLbIndex].replace(" lbs", "").toDoubleOrNull() ?: 0.0
                                        } else {
                                            weightKgItems[currentWeightKgIndex].replace(" kg", "").toDoubleOrNull() ?: 0.0
                                        }
                                        
                                        val heightDouble = hDouble
                                        val weightDouble = wDouble
                                        isLbs = isWeightLbs // Synced!
                                        
                                        var finalTargetCals = targetCals
                                        if (finalTargetCals == 0 && ageInt > 0 && heightDouble > 0.0 && weightDouble > 0.0) {
                                            val bmr = (10.0 * weightDouble) + (6.25 * heightDouble) - (5.0 * ageInt) + if (gender == "Hombre") 5.0 else -161.0
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
                                            finalTargetCals = (tdee * goalMultiplier).toInt()
                                        }
                                        
                                        onLoginSuccess(name.trim(), isLbs, gender, ageInt, heightDouble, activityLevel, weightDouble, finalTargetCals, selectedGoal)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .supercardGlassModifier(RoundedCornerShape(12.dp))
                                    .testTag("local_submit_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.auth_finish_btn), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemWidth = 90.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedIndex))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress, listState.firstVisibleItemIndex) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        
        val layoutInfo = listState.layoutInfo
        val unpaddedCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val centerItem = layoutInfo.visibleItemsInfo.minByOrNull {
            val itemCenter = it.offset + (it.size / 2f)
            kotlin.math.abs(itemCenter - unpaddedCenter)
        }?.index
        
        if (centerItem != null && centerItem in items.indices) {
            if (selectedIndex != centerItem) {
                onIndexChanged(centerItem)
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val containerWidth = screenWidth - 48.dp
    val horizontalPadding = (containerWidth - itemWidth) / 2

    Box(modifier = modifier.height(76.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.lazy.LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = maxOf(0.dp, horizontalPadding)),
            verticalAlignment = Alignment.Bottom
        ) {
            items(items.size) { index ->
                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        androidx.compose.material3.Text(
                            text = items[index],
                            fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).graphicsLayer {
                                val layoutInfo = listState.layoutInfo
                                val unpaddedCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                                if (itemInfo != null) {
                                    val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
                                    val distance = kotlin.math.abs(itemCenter - unpaddedCenter)
                                    val maxDistance = itemInfo.size.toFloat() * 1.5f
                                    val fraction = 1f - (distance / maxDistance).coerceIn(0f, 1f)
                                    
                                    scaleX = 0.85f + (fraction * 0.45f)
                                    scaleY = 0.85f + (fraction * 0.45f)
                                    alpha = 0.35f + (fraction * 0.65f)
                                    translationY = -(fraction * 28f)
                                } else {
                                    scaleX = 0.85f
                                    scaleY = 0.85f
                                    alpha = 0.35f
                                }
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(modifier = Modifier.width(2.dp).height(12.dp).background(Color.White.copy(alpha=0.3f)))
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.width(2.dp).height(16.dp).background(Color.White.copy(alpha=0.3f)))
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.width(2.dp).height(24.dp).background(Color.White.copy(alpha=0.5f), RoundedCornerShape(1.dp)))
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.width(2.dp).height(16.dp).background(Color.White.copy(alpha=0.3f)))
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.width(2.dp).height(12.dp).background(Color.White.copy(alpha=0.3f)))
                        }
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(4.dp)
                .height(34.dp)
                .background(Color(0xFF00C2FF), RoundedCornerShape(2.dp))
        )
    }
}
