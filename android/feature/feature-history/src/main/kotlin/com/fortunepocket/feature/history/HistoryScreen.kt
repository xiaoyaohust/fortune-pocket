package com.fortunepocket.feature.history

import android.content.Intent
import com.fortunepocket.feature.history.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.core.model.ReadingPresentationBuilder
import com.fortunepocket.core.model.ReadingRecord
import com.fortunepocket.core.model.HistoryTrajectorySnapshot
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isZh = Locale.getDefault().language == "zh"
    val emptyText = stringResource(R.string.history_empty)
    val deleteRecordText = stringResource(R.string.history_delete_record)
    val shareButtonText = stringResource(R.string.history_share_button)
    val shareChooserText = stringResource(R.string.history_share_chooser)
    val deleteButtonText = stringResource(R.string.history_delete_button)
    val deleteConfirmText = stringResource(R.string.history_delete_confirm)
    val deleteConfirmButtonText = stringResource(R.string.history_delete_confirm_button)
    val cancelText = stringResource(R.string.history_cancel)
    var pendingDelete by remember { mutableStateOf<ReadingRecord?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AppColors.backgroundDeep, AppColors.backgroundBase)
                )
            )
    ) {
        if (uiState.records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("✦", style = FortunePocketTypography.titleLarge, color = AppColors.accentGold)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = emptyText,
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.trajectorySnapshot?.let { snapshot ->
                    item(key = "trajectory") {
                        HistoryTrajectoryCard(snapshot = snapshot, isZh = isZh)
                    }
                }
                items(uiState.records, key = { it.id }) { record ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectRecord(record) },
                        shape = RoundedCornerShape(18.dp),
                        color = AppColors.backgroundElevated
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = when (record.type.key) {
                                        "tarot" -> "🃏"
                                        "astrology" -> "✨"
                                        else -> "☯"
                                    },
                                    style = FortunePocketTypography.titleLarge
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.title,
                                        style = FortunePocketTypography.titleMedium,
                                        color = AppColors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = java.text.DateFormat.getDateInstance(
                                            java.text.DateFormat.MEDIUM,
                                            Locale.getDefault()
                                        ).format(java.util.Date(record.createdAt)),
                                        style = FortunePocketTypography.bodyMedium,
                                        color = AppColors.textSecondary
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = record.summary,
                                style = FortunePocketTypography.bodyMedium,
                                color = AppColors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { pendingDelete = record },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.error),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = deleteRecordText,
                                    color = AppColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        uiState.selectedRecord?.let { record ->
            val presentation = ReadingPresentationBuilder.build(record, isZh)
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectRecord(null) },
                containerColor = AppColors.backgroundBase
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(record.title, style = FortunePocketTypography.headlineMedium, color = AppColors.textPrimary)
                    Text(
                        java.text.DateFormat.getDateInstance(
                            java.text.DateFormat.LONG,
                            Locale.getDefault()
                        ).format(java.util.Date(record.createdAt)),
                        style = FortunePocketTypography.bodyMedium,
                        color = AppColors.textSecondary
                    )

                    if (presentation != null) {
                        presentation.sections.forEach { section ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppColors.backgroundElevated
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(
                                        text = section.title,
                                        style = FortunePocketTypography.titleMedium,
                                        color = AppColors.accentGold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = section.body,
                                        style = FortunePocketTypography.bodyMedium,
                                        color = AppColors.textPrimary
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                setType("text/plain")
                                                putExtra(Intent.EXTRA_TEXT, presentation.shareText)
                                            },
                                            shareChooserText
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.backgroundElevated)
                            ) {
                                Text(shareButtonText, color = AppColors.textPrimary)
                            }
                            Button(
                                onClick = { pendingDelete = record },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.error)
                            ) {
                                Text(deleteButtonText, color = AppColors.textPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        pendingDelete?.let { record ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRecord(record)
                            pendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.error)
                    ) {
                        Text(deleteConfirmButtonText, color = AppColors.textPrimary)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { pendingDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.backgroundElevated)
                    ) {
                        Text(cancelText, color = AppColors.textPrimary)
                    }
                },
                title = {
                    Text(deleteConfirmText)
                },
                text = {
                    Text(record.title)
                },
                containerColor = AppColors.backgroundBase,
                textContentColor = AppColors.textPrimary,
                titleContentColor = AppColors.textPrimary
            )
        }
    }
}

@Composable
private fun HistoryTrajectoryCard(snapshot: HistoryTrajectorySnapshot, isZh: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.backgroundElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isZh) "成长轨迹" else "Growth Trajectory",
                style = FortunePocketTypography.bodyMedium,
                color = AppColors.accentGold
            )
            Text(
                text = snapshot.headline,
                style = FortunePocketTypography.headlineMedium,
                color = AppColors.textPrimary
            )
            snapshot.insights.forEach { insight ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.backgroundBase
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = insight.title,
                            style = FortunePocketTypography.titleMedium,
                            color = AppColors.accentGold
                        )
                        Text(
                            text = insight.body,
                            style = FortunePocketTypography.bodyMedium,
                            color = AppColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
