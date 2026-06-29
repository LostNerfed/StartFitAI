package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.R
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AppTextStyle
import com.example.ui.theme.TextSecundario
import kotlinx.coroutines.delay

@Composable
fun ProfileCreationLoadingScreen(
    onFinished: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Step 0: "Cargando perfil..."
        delay(1500)
        // Step 1: "Configurando entorno de StartFit AI..."
        step = 1
        delay(1500)
        // Step 2: "Haz que tu entrenamiento de hoy cuente."
        step = 2
        delay(2000)
        // Finish
        onFinished()
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animated2))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(300.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "loading_text_anim"
        ) { targetStep ->
            when (targetStep) {
                0 -> Text(
                    text = "Cargando perfil...",
                    style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Normal),
                    color = TextSecundario,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                1 -> Text(
                    text = "Configurando entorno de StartFit AI...",
                    style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Normal),
                    color = TextSecundario,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                2 -> Text(
                    text = "Haz que tu entrenamiento de hoy cuente.",
                    style = AppTextStyle.headlineOswald.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
