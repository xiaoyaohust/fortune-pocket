package com.fortunepocket.feature.settings

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.feature.settings.R
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notificationPermissionRequiredText = stringResourceCompat(R.string.settings_notification_permission_required)
    var showBirthdayPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onNotificationEnabledChanged(true)
            permissionMessage = null
        } else {
            permissionMessage = notificationPermissionRequiredText
        }
    }

    if (showBirthdayPicker) {
        BirthdayPickerDialog(
            initialMillis = if (uiState.birthdayMillis > 0) uiState.birthdayMillis else System.currentTimeMillis(),
            onDateSelected = {
                viewModel.onBirthdayChanged(it)
                showBirthdayPicker = false
            },
            onDismiss = { showBirthdayPicker = false }
        )
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = uiState.notificationHour,
            initialMinute = uiState.notificationMinute,
            onTimeSelected = { hour, minute ->
                viewModel.onNotificationTimeChanged(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AppColors.backgroundDeep, AppColors.backgroundBase)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsCard {
                Text(
                    text = stringResourceCompat(R.string.settings_reminder),
                    style = FortunePocketTypography.headlineMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResourceCompat(R.string.settings_reminder),
                        style = FortunePocketTypography.titleMedium,
                        color = AppColors.textPrimary
                    )
                    Switch(
                        checked = uiState.notificationEnabled,
                        onCheckedChange = { enabled: Boolean ->
                            if (!enabled) {
                                viewModel.onNotificationEnabledChanged(false)
                                permissionMessage = null
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.onNotificationEnabledChanged(true)
                                permissionMessage = null
                            }
                        }
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResourceCompat(R.string.settings_reminder_time),
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            uiState.notificationHour,
                            uiState.notificationMinute
                        ),
                        color = AppColors.textPrimary
                    )
                }
            }

            SettingsCard {
                Text(
                    text = stringResourceCompat(R.string.settings_language),
                    style = FortunePocketTypography.headlineMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf<Pair<String, String>>(
                        "system" to stringResourceCompat(R.string.settings_follow_system),
                        "zh" to "简体中文",
                        "en" to "English"
                    ).forEach { option ->
                        val (value, label) = option
                        FilterChip(
                            selected = uiState.appLanguage == value,
                            onClick = {
                                viewModel.onAppLanguageChanged(value)
                                AppCompatDelegate.setApplicationLocales(
                                    when (value) {
                                        "zh" -> LocaleListCompat.forLanguageTags("zh")
                                        "en" -> LocaleListCompat.forLanguageTags("en")
                                        else -> LocaleListCompat.getEmptyLocaleList()
                                    }
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (uiState.appLanguage == value) {
                                        AppColors.backgroundDeep
                                    } else {
                                        AppColors.textPrimary
                                    }
                                )
                            }
                        )
                    }
                }
            }

            SettingsCard {
                Text(
                    text = stringResourceCompat(R.string.settings_birthday),
                    style = FortunePocketTypography.headlineMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showBirthdayPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.birthdayMillis > 0) {
                            DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
                                .format(Date(uiState.birthdayMillis))
                        } else {
                            stringResourceCompat(R.string.settings_birthday_choose)
                        },
                        color = AppColors.textPrimary
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResourceCompat(R.string.settings_birthday_helper),
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }

            SettingsCard {
                Text(
                    text = stringResourceCompat(R.string.settings_about),
                    style = FortunePocketTypography.headlineMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text("Fortune Pocket", style = FortunePocketTypography.titleMedium, color = AppColors.accentGold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResourceCompat(R.string.settings_about_body),
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.error),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResourceCompat(R.string.settings_clear_history),
                        color = AppColors.textPrimary
                    )
                }
            }

            permissionMessage?.let { message ->
                Text(
                    text = message,
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.error)
                ) {
                    Text(stringResourceCompat(R.string.confirm), color = AppColors.textPrimary)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.backgroundElevated)
                ) {
                    Text(stringResourceCompat(R.string.cancel), color = AppColors.textPrimary)
                }
            },
            title = { Text(stringResourceCompat(R.string.settings_clear_history)) },
            text = { Text(stringResourceCompat(R.string.settings_clear_confirm)) },
            containerColor = AppColors.backgroundBase,
            titleContentColor = AppColors.textPrimary,
            textContentColor = AppColors.textPrimary
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.backgroundElevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun BirthdayPickerDialog(
    initialMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(initialMillis) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}

@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = true

    DisposableEffect(initialHour, initialMinute) {
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(hour, minute) },
            initialHour,
            initialMinute,
            is24Hour
        )
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int): String = androidx.compose.ui.res.stringResource(id)
