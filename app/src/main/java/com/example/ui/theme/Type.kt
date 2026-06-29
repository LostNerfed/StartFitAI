package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val OswaldFontFamily = FontFamily(
    Font(R.font.oswald_regular, FontWeight.Normal),
    Font(R.font.oswald_medium, FontWeight.Medium),
    Font(R.font.oswald_semibold, FontWeight.SemiBold),
    Font(R.font.oswald_bold, FontWeight.Bold)
)

val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = OswaldFontFamily),
    displayMedium = TextStyle(fontFamily = OswaldFontFamily),
    displaySmall = TextStyle(fontFamily = OswaldFontFamily),
    headlineLarge = TextStyle(fontFamily = OswaldFontFamily),
    headlineMedium = TextStyle(fontFamily = OswaldFontFamily),
    headlineSmall = TextStyle(fontFamily = OswaldFontFamily),
    titleLarge = TextStyle(fontFamily = InterFontFamily),
    titleMedium = TextStyle(fontFamily = InterFontFamily),
    titleSmall = TextStyle(fontFamily = InterFontFamily),
    bodyLarge = TextStyle(fontFamily = InterFontFamily),
    bodyMedium = TextStyle(fontFamily = InterFontFamily),
    bodySmall = TextStyle(fontFamily = InterFontFamily),
    labelLarge = TextStyle(fontFamily = InterFontFamily),
    labelMedium = TextStyle(fontFamily = InterFontFamily),
    labelSmall = TextStyle(fontFamily = InterFontFamily)
)

object AppTextStyle {
    val headlineOswald = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )
    val displayOswald = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    )
    val titleOswald = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    val numberLarge = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        fontFeatureSettings = "tnum"
    )
    val numberMedium = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        fontFeatureSettings = "tnum"
    )
    val numberSmall = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        fontFeatureSettings = "tnum"
    )
    val statBig = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        fontFeatureSettings = "tnum"
    )
    val statSmall = TextStyle(
        fontFamily = OswaldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        fontFeatureSettings = "tnum"
    )
    val labelBold = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}
