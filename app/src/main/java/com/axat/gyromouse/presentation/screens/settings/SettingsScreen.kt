package com.axat.gyromouse.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.axat.gyromouse.R

/**
 * Settings screen with sliders, toggles, and an about section.
 * All changes are persisted immediately via DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Mouse Section ─────────────────────────────────────
            SectionTitle(title = stringResource(R.string.settings_mouse_section))

            SettingsCard {
                // Sensitivity slider
                SliderSetting(
                    title = stringResource(R.string.settings_sensitivity),
                    value = settings.sensitivity,
                    valueLabel = String.format("%.1fx", settings.sensitivity),
                    valueRange = 0.5f..3.0f,
                    onValueChange = { viewModel.setSensitivity(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                // Pointer acceleration toggle
                SwitchSetting(
                    title = stringResource(R.string.settings_pointer_acceleration),
                    description = stringResource(R.string.settings_pointer_acceleration_desc),
                    checked = settings.pointerAcceleration,
                    onCheckedChange = { viewModel.setPointerAcceleration(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Scrolling Section ─────────────────────────────────
            SectionTitle(title = stringResource(R.string.settings_scroll_section))

            SettingsCard {
                // Scroll speed slider
                SliderSetting(
                    title = stringResource(R.string.settings_scroll_speed),
                    value = settings.scrollSpeed,
                    valueLabel = String.format("%.1fx", settings.scrollSpeed),
                    valueRange = 0.5f..3.0f,
                    onValueChange = { viewModel.setScrollSpeed(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                // Natural scroll toggle
                SwitchSetting(
                    title = stringResource(R.string.settings_natural_scroll),
                    description = stringResource(R.string.settings_natural_scroll_desc),
                    checked = settings.naturalScroll,
                    onCheckedChange = { viewModel.setNaturalScroll(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Gestures Section ──────────────────────────────────
            SectionTitle(title = stringResource(R.string.settings_gestures_section))

            SettingsCard {
                // Tap to click toggle
                SwitchSetting(
                    title = stringResource(R.string.settings_tap_to_click),
                    description = stringResource(R.string.settings_tap_to_click_desc),
                    checked = settings.tapToClick,
                    onCheckedChange = { viewModel.setTapToClick(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                // Show gesture hints toggle
                SwitchSetting(
                    title = stringResource(R.string.settings_gesture_hints),
                    description = stringResource(R.string.settings_gesture_hints_desc),
                    checked = settings.showGestureHints,
                    onCheckedChange = { viewModel.setShowGestureHints(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Display Section ───────────────────────────────────
            SectionTitle(title = stringResource(R.string.settings_display_section))

            SettingsCard {
                // Keep screen awake toggle
                SwitchSetting(
                    title = stringResource(R.string.settings_keep_screen_awake),
                    description = stringResource(R.string.settings_keep_screen_awake_desc),
                    checked = settings.keepScreenAwake,
                    onCheckedChange = { viewModel.setKeepScreenAwake(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── About Section ─────────────────────────────────────
            SectionTitle(title = stringResource(R.string.settings_about))

            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(stringResource(R.string.settings_version), "1.0"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.settings_github),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Reusable Components ───────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
