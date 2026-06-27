package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.data.database.FitSettings
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val scope = rememberCoroutineScope()

    var showCoachSetupSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_app_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 0. PREFERENCES SECTION
            item {
                Text(text = stringResource(R.string.settings_preferences_section), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecundario)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().supercardGlassModifier(RoundedCornerShape(12.dp))
                ) {
                    SettingItemRow(
                        title = stringResource(R.string.settings_language),
                        sub = stringResource(R.string.settings_language_sub),
                        icon = Icons.Default.Translate,
                        iconTint = AccentGreen,
                        onClick = { showLanguageSheet = true }
                    )
                    HorizontalDivider(color = BorderColorSubtle)
                    SettingItemRow(
                        title = "Sistema de Peso",
                        sub = "Unidad actual: ${settings.weightUnit.uppercase(java.util.Locale.getDefault())} (Toca para cambiar)",
                        icon = Icons.Default.FitnessCenter,
                        iconTint = AccentGreen,
                        onClick = { viewModel.toggleWeightUnit() }
                    )
                }
            }

            // 2. DANGER ZONE
            item {
                Text(text = stringResource(R.string.settings_account_security), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().supercardGlassModifier(RoundedCornerShape(12.dp))
                ) {
                    SettingItemRow(
                        title = stringResource(R.string.settings_logout),
                        sub = stringResource(R.string.settings_logout_sub),
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = AccentAmber,
                        onClick = {
                            viewModel.logout()
                            onBack()
                        }
                    )
                    HorizontalDivider(color = BorderColorSubtle)
                    SettingItemRowDeleted(
                        title = stringResource(R.string.settings_delete_all_data),
                        sub = stringResource(R.string.settings_delete_all_data_sub),
                        icon = Icons.Default.Delete,
                        onClick = { showDeleteConfirmDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // Language selection bottom sheet
        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                containerColor = Color.Transparent,
                dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) }
            ) {
                LanguageSelectionContent(
                    onLanguageSelected = { tag ->
                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                            androidx.core.os.LocaleListCompat.forLanguageTags(tag)
                        )
                        showLanguageSheet = false
                    }
                )
            }
        }


        // Delete all data prompt
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                modifier = Modifier.supercardGlassModifier(RoundedCornerShape(20.dp)),
                title = { Text(text = stringResource(R.string.settings_delete_confirm_title)) },
                text = { Text(text = stringResource(R.string.settings_delete_confirm_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllData()
                            showDeleteConfirmDialog = false
                            onBack()
                        }
                    ) {
                        Text(text = stringResource(R.string.settings_delete_all), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(text = stringResource(R.string.btn_cancel), color = Color.White)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun SettingItemRow(
    title: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bgColor = if (iconTint == Color.White) com.example.ui.theme.AmoledSurface else iconTint.copy(alpha = 0.15f)
        Box(
            modifier = Modifier.background(bgColor, CircleShape).size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = sub, fontSize = 11.sp, color = TextSecundario, maxLines = 2)
        }
    }
}

@Composable
private fun SettingItemRowDeleted(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.background(Color.Red, CircleShape).size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color.Black, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Text(text = sub, fontSize = 11.sp, color = TextSecundario, maxLines = 2)
        }
    }
}

@Composable
fun LanguageSelectionContent(onLanguageSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supercardGlassModifier(RoundedCornerShape(16.dp))
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp).padding(bottom = 16.dp)
        )
        
        val languages = listOf(
            "es" to stringResource(R.string.language_es),
            "en" to stringResource(R.string.language_en),
            "pt-BR" to stringResource(R.string.language_pt),
            "fr" to stringResource(R.string.language_fr)
        )
        
        val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
        
        languages.forEach { (tag, langName) ->
            val isSelected = currentLocale.contains(tag) || (currentLocale.isEmpty() && tag == "es")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.97f))
                        } else {
                            Modifier.metricCellGlassModifier(RoundedCornerShape(12.dp))
                        }
                    )
                    .clickable { onLanguageSelected(tag) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = langName,
                    color = if (isSelected) Color.Black else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
