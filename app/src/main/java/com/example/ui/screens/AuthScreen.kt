package com.example.ui.screens

import kotlinx.coroutines.launch

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        "Hipertrofia / Ganar Músculo", 
        "Pérdida de Grasa / Definición", 
        "Fuerza y Rendimiento", 
        "Mantenimiento / Salud General"
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
        "Hipertrofia / Ganar Músculo" to stringResource(R.string.goal_hypertrophy),
        "Pérdida de Grasa / Definición" to stringResource(R.string.goal_fatloss),
        "Fuerza y Rendimiento" to stringResource(R.string.goal_strength),
        "Mantenimiento / Salud General" to stringResource(R.string.goal_maintenance)
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
            .padding(24.dp)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
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
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val screenWidth = maxWidth
                val synergyFontSize = (screenWidth.value * 0.11f).coerceIn(24f, 44f).sp
                val fitFontSize = (synergyFontSize.value * 0.48f).sp

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.auth_synergy),
                        fontFamily = com.example.ui.theme.PointlessFontFamily,
                        fontSize = synergyFontSize,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = stringResource(R.string.auth_fit),
                        fontFamily = com.example.ui.theme.PointlessFontFamily,
                        fontSize = fitFontSize,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                    )
                }
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
                            Image(
                                painter = painterResource(id = R.drawable.start_fit_3),
                                contentDescription = "StartFit AI Logo",
                                modifier = Modifier.size(120.dp).padding(bottom = 24.dp)
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
                            Text(
                                text = stringResource(R.string.auth_privacy_desc),
                                color = TextSecundario,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 32.dp)
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
                            // Step 3: Unidades
                            Text(stringResource(R.string.auth_step2_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (!isLbs) Modifier
                                                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                            else Modifier
                                                .liquidGlassModifier(RoundedCornerShape(8.dp))
                                        )
                                        .clickable { isLbs = false }
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.auth_metric), color = if (!isLbs) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (isLbs) Modifier
                                                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                            else Modifier
                                                .liquidGlassModifier(RoundedCornerShape(8.dp))
                                        )
                                        .clickable { isLbs = true }
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.auth_imperial), color = if (isLbs) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { step = 4 },
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
                        4 -> {
                            // Step 4: Género & Edad
                            Text(stringResource(R.string.settings_profile), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (gender == "Hombre") Modifier
                                                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                            else Modifier
                                                .liquidGlassModifier(RoundedCornerShape(8.dp))
                                        )
                                        .clickable { gender = "Hombre" }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.auth_gender_m), color = if (gender == "Hombre") Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (gender == "Mujer") Modifier
                                                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                            else Modifier
                                                .liquidGlassModifier(RoundedCornerShape(8.dp))
                                        )
                                        .clickable { gender = "Mujer" }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.auth_gender_f), color = if (gender == "Mujer") Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

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
                                    val ageInt = ageStr.toIntOrNull() ?: 0
                                    if (ageInt <= 0 || ageInt > 120) {
                                        error = context.getString(R.string.auth_err_age)
                                    } else {
                                        error = null
                                        step = 5
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
                        5 -> {
                            // Step 5: Talla & Peso
                            Text(stringResource(R.string.settings_measurements), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                            
                            OutlinedTextField(
                                value = heightStr,
                                onValueChange = { 
                                    heightStr = it.filter { char -> char.isDigit() || char == '.' }.take(5) 
                                    error = null
                                },
                                placeholder = { Text(if (isLbs) stringResource(R.string.auth_height_in_ph) else stringResource(R.string.auth_height_cm_ph), color = TextSecundario, fontSize = 14.sp) },
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

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = weightStr,
                                onValueChange = { 
                                    weightStr = it.filter { char -> char.isDigit() || char == '.' }.take(5) 
                                    error = null
                                },
                                placeholder = { Text(if (isLbs) stringResource(R.string.auth_weight_lbs_ph) else stringResource(R.string.auth_weight_kg_ph), color = TextSecundario, fontSize = 14.sp) },
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
                            Text(
                                text = "Podrás cambiar entre Kilos/Libras y Centímetros/Pulgadas más adelante en los Ajustes.",
                                color = TextSecundario,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                textAlign = TextAlign.Center
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
                                    val heightDouble = heightStr.toDoubleOrNull() ?: 0.0
                                    val weightDouble = weightStr.toDoubleOrNull() ?: 0.0
                                    
                                    val validHeight = if (isLbs) heightDouble > 20.0 && heightDouble < 100.0 else heightDouble > 50.0 && heightDouble <= 250.0
                                    
                                    if (!validHeight) {
                                        error = context.getString(R.string.auth_err_height)
                                    } else if (weightDouble <= 20.0 || weightDouble > 300.0) {
                                        error = context.getString(R.string.auth_err_weight)
                                    } else {
                                        error = null
                                        step = 6
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
                        6 -> {
                            // Step 6: Nivel de actividad
                            Text(stringResource(R.string.auth_step5_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                activityOptions.forEach { option ->
                                    val isSelected = option == activityLevel
                                    val displayText = activityOptionsMap[option] ?: option
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
                                            .clickable { activityLevel = option }
                                            .padding(vertical = 16.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = displayText,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { step = 7 },
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
                        7 -> {
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
                                onClick = { step = 8 },
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
                        8 -> {
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
                                        val heightDouble = heightStr.toDoubleOrNull() ?: 0.0
                                        val weightDouble = weightStr.toDoubleOrNull() ?: 0.0
                                        
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
