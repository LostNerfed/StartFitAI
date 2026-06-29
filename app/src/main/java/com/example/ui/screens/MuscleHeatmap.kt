package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.min
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSecundario
import com.example.ui.theme.liquidGlassModifier

@Composable
fun MuscleHeatmap(
    muscleVolumes: Map<String, Double>,
    volumeConverted: String,
    totalSets: Int,
    volumeUnit: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassModifier(RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp)
    ) {
        // Dual Canvas View (Front & Back)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Front Body
                Box(
                    modifier = Modifier.weight(1f).height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnatomyCanvas(
                        paths = MuscleAnatomyFront.paths,
                        muscleVolumes = muscleVolumes
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                // Back Body
                Box(
                    modifier = Modifier.weight(1f).height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnatomyCanvas(
                        paths = MuscleAnatomyBack.paths,
                        muscleVolumes = muscleVolumes
                    )
                }
            }
            
            // Volume and Sets between the heads
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = "Volume", tint = TextSecundario, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "$volumeConverted $volumeUnit", fontSize = 12.sp, color = TextSecundario)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Repeat, contentDescription = "Sets", tint = TextSecundario, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "$totalSets", fontSize = 12.sp, color = TextSecundario)
                }
            }
        }
    }
}

@Composable
fun AnatomyCanvas(paths: List<Pair<String, String>>, muscleVolumes: Map<String, Double>) {
    val strokeColor = SurfaceDark

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // The Affinity SVG export has a viewBox of 288x288, but the body only uses ~270 height
        val scale = canvasHeight / 275f 
        
        val offsetX = (canvasWidth - (288f * scale)) / 2f
        val offsetY = 0f // Align strictly to top to prevent massive bottom padding

        val matrix = androidx.compose.ui.graphics.Matrix()
        matrix.translate(offsetX, offsetY)
        matrix.scale(scale, scale)

        for ((id, pathString) in paths) {
            val color = getColorForMuscleId(id, muscleVolumes)
            val path = PathParser().parsePathString(pathString).toPath()
            path.transform(matrix)
            
            drawPath(path, color = color, style = Fill)
            // Add a subtle border to mimic separation (if desired, or leave solid)
            // If the user wants no borders, they can just be filled, but drawing stroke ensures gap visibility if paths touch.
            // Leaving stroke to prevent bleeding, can be removed if user wants absolute flat.
            // drawPath(path, color = strokeColor, style = Stroke(width = 1f))
        }
    }
}

private fun getColorForMuscleId(id: String, muscleVolumes: Map<String, Double>): Color {
    val idLower = id.lowercase()
    val groupKey = when {
        idLower.contains("pectoral") || idLower.contains("serrato") -> "pectoral"
        idLower.contains("abdom") || idLower.contains("oblicuo") -> "core"
        idLower.contains("bicep") -> "biceps"
        idLower.contains("tricep") -> "triceps"
        idLower.contains("antebrazo") -> "antebrazo"
        idLower.contains("hombro") -> "hombros"
        idLower.contains("cuadri") -> "cuadriceps"
        idLower.contains("femor") -> "femorales"
        idLower.contains("gluteo") -> "gluteo"
        idLower.contains("pantorr") || idLower.contains("gemel") -> "pantorrilla"
        idLower.contains("dorsal") -> "dorsal"
        idLower.contains("trapecio") || idLower.contains("nuca") || idLower.contains("romboid") -> "trapecio y romboides"
        idLower.contains("lumbar") -> "lumbar"
        else -> null
    }

    if (groupKey == null) return Color(0xFF6E6E74)
    val intensity = muscleVolumes[groupKey] ?: 0.0
    if (intensity > 0.0) {
        return getColorForIntensity(intensity)
    }
    
    // Default base color matching the user's hex #6E6E74
    return Color(0xFF6E6E74)
}

private fun getColorForIntensity(intensity: Double): Color {
    return when {
        intensity >= 15.0 -> Color(0xFFFF5252) // Very high - Neon Red
        intensity >= 10.0 -> Color(0xFFFF7B7B) // High
        intensity >= 5.0 -> Color(0xFFFFA4A4)  // Medium
        intensity > 0.0 -> Color(0xFFFFCDCD)   // Low
        else -> Color(0xFF6E6E74)            // Inactive Base Color
    }
}
