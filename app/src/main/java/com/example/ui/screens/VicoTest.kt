package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer

@Composable
fun TestAxisItemPlacer() {
    val a = AxisItemPlacer.Vertical.default(maxItemCount = 8)
}
