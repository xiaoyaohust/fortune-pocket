package com.fortunepocket.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyRitualScreen(
    onBack: () -> Unit,
    onContinue: (DailyRitualDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isZh = Locale.getDefault().language == "zh"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppColors.backgroundDeep, AppColors.backgroundBase)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = AppColors.accentGold
                    )
                }
                Text(
                    text = if (isZh) "今日仪式" else "Today's Ritual",
                    style = FortunePocketTypography.titleLarge,
                    color = AppColors.textPrimary
                )
            }

            if (uiState.isLoading) {
                HeroSkeleton()
            } else {
                uiState.dailyRitual?.let { ritual ->
                    val dateStr = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
                        .format(Date(ritual.generatedAtMillis))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = AppColors.backgroundElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isZh) "今晚入口" else "Tonight's Doorway",
                                style = FortunePocketTypography.labelSmall.copy(letterSpacing = 1.8.sp),
                                color = AppColors.accentGold
                            )
                            Text(
                                text = ritual.ritualTitle.resolve(isZh),
                                style = FortunePocketTypography.headlineLarge,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = dateStr,
                                style = FortunePocketTypography.bodySmall,
                                color = AppColors.textSecondary
                            )
                            Text(
                                text = ritual.ritualSummary.resolve(isZh),
                                style = FortunePocketTypography.bodyMedium,
                                color = AppColors.textSecondary
                            )
                        }
                    }

                    DailyPreviewColumn(ritual = ritual, isZh = isZh)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = AppColors.backgroundElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionEyebrow(text = ritual.promptTitle.resolve(isZh))
                            Text(
                                text = ritual.promptBody.resolve(isZh),
                                style = FortunePocketTypography.bodyMedium,
                                color = AppColors.textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Button(
                                onClick = { onContinue(ritual.destination) },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold)
                            ) {
                                Text(
                                    text = if (isZh)
                                        "继续前往${ritual.destination.title(true)}"
                                    else
                                        "Continue to ${ritual.destination.title(false)}",
                                    style = FortunePocketTypography.titleMedium,
                                    color = AppColors.backgroundDeep
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
