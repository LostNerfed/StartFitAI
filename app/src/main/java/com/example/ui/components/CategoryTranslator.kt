package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun getTranslatedCategory(dbCategory: String): String {
    return when (dbCategory.lowercase().trim()) {
        // Headers
        "pecho" -> stringResource(R.string.cat_header_chest)
        "espalda" -> stringResource(R.string.cat_header_back)
        "piernas" -> stringResource(R.string.cat_header_legs)
        "brazos" -> stringResource(R.string.cat_header_arms)
        "hombros" -> stringResource(R.string.cat_header_shoulders)
        "core" -> stringResource(R.string.cat_header_core)
        "general" -> stringResource(R.string.cat_header_general)
        
        // Muscles
        "pectoral" -> stringResource(R.string.cat_pectoral)
        "dorsal" -> stringResource(R.string.cat_dorsal)
        "trapecio y romboides" -> stringResource(R.string.cat_traps_rhomboids)
        "lumbar" -> stringResource(R.string.cat_lumbar)
        "cuádriceps" -> stringResource(R.string.cat_quads)
        "femorales" -> stringResource(R.string.cat_hamstrings)
        "glúteo" -> stringResource(R.string.cat_glutes)
        "pantorrilla" -> stringResource(R.string.cat_calves)
        "bíceps" -> stringResource(R.string.cat_biceps)
        "tríceps" -> stringResource(R.string.cat_triceps)
        "antebrazo" -> stringResource(R.string.cat_forearms)
        "otros" -> stringResource(R.string.cat_others)
        
        // Fallback for user custom strings or unknown
        else -> dbCategory
    }
}
