package com.fortunepocket.feature.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenDailyRitual: () -> Unit,
    onNavigateToTarot: () -> Unit,
    onNavigateToAstrology: () -> Unit,
    onNavigateToBazi: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isZh = Locale.getDefault().language == "zh"

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
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            HomeHeader(isZh = isZh)

            if (uiState.isLoading) {
                HeroSkeleton()
            } else {
                uiState.dailyRitual?.let { ritual ->
                    RitualHeroCard(
                        ritual = ritual,
                        isZh = isZh,
                        onOpenDailyRitual = onOpenDailyRitual
                    )
                    DailyPreviewColumn(ritual = ritual, isZh = isZh)
                    ExploreSection(
                        isZh = isZh,
                        onNavigateToTarot = onNavigateToTarot,
                        onNavigateToAstrology = onNavigateToAstrology,
                        onNavigateToBazi = onNavigateToBazi
                    )
                }
            }

            if (uiState.error != null) {
                Text(
                    text = if (isZh)
                        "今日仪式暂时没有准备好，请稍后再试。"
                    else
                        "Today's ritual is not ready yet. Please try again in a moment.",
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(isZh: Boolean) {
    val dateStr = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(Date())

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "✦",
                style = FortunePocketTypography.labelSmall,
                color = AppColors.accentGold.copy(alpha = 0.7f)
            )
            Text(
                text = "FORTUNE POCKET",
                style = FortunePocketTypography.labelSmall.copy(letterSpacing = 3.sp),
                color = AppColors.textMuted
            )
        }
        Text(
            text = if (isZh) "今晚想先看哪一道光？" else "Which light do you want to follow tonight?",
            style = FortunePocketTypography.displayMedium,
            color = AppColors.textPrimary
        )
        Text(
            text = dateStr,
            style = FortunePocketTypography.bodyMedium,
            color = AppColors.textSecondary
        )
    }
}

@Composable
private fun RitualHeroCard(
    ritual: DailyRitual,
    isZh: Boolean,
    onOpenDailyRitual: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = AppColors.backgroundElevated
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.NightsStay,
                    contentDescription = null,
                    tint = AppColors.accentGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isZh) "今日仪式" else "Today's Ritual",
                    style = FortunePocketTypography.labelSmall.copy(letterSpacing = 1.8.sp),
                    color = AppColors.accentGold
                )
            }

            Text(
                text = ritual.ritualTitle.resolve(isZh),
                style = FortunePocketTypography.headlineLarge,
                color = AppColors.textPrimary
            )

            Text(
                text = ritual.ritualSummary.resolve(isZh),
                style = FortunePocketTypography.bodyMedium,
                color = AppColors.textSecondary
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AppColors.backgroundBase
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ritual.destination.icon(),
                        contentDescription = null,
                        tint = AppColors.accentGold
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ritual.destination.title(isZh),
                            style = FortunePocketTypography.titleMedium,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = ritual.destinationReason.resolve(isZh),
                            style = FortunePocketTypography.bodySmall,
                            color = AppColors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.textSecondary
                    )
                }
            }

            Button(
                onClick = onOpenDailyRitual,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold)
            ) {
                Text(
                    text = if (isZh) "进入今日仪式" else "Enter Today's Ritual",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.backgroundDeep
                )
            }
        }
    }
}

@Composable
fun DailyPreviewColumn(ritual: DailyRitual, isZh: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionEyebrow(text = if (isZh) "今晚先感受这些讯号" else "Read These Signals First")

        DailyPreviewCard(
            title = ritual.tarot.title.resolve(isZh),
            detail = ritual.tarot.insight.resolve(isZh),
            accent = AppColors.accentGold,
            icon = Icons.Outlined.AutoStories
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallTarotPreview(ritual = ritual, isZh = isZh)
            }
        }

        DailyPreviewCard(
            title = ritual.energyTitle.resolve(isZh),
            detail = ritual.energyBody.resolve(isZh),
            accent = AppColors.accentPurple,
            icon = Icons.Outlined.BubbleChart
        )

        DailyPreviewCard(
            title = ritual.transit.title.resolve(isZh),
            detail = ritual.transit.summary.resolve(isZh),
            accent = AppColors.accentRose,
            icon = Icons.Outlined.Stars
        ) {
            ritual.transit.sign?.let { sign ->
                Text(
                    text = "${sign.symbol} ${sign.localizedName(isZh)}",
                    style = FortunePocketTypography.bodySmall,
                    color = AppColors.accentGold
                )
            }
        }
    }
}

@Composable
private fun SmallTarotPreview(ritual: DailyRitual, isZh: Boolean) {
    val orientation = if (ritual.tarot.isUpright) {
        if (isZh) "正位" else "Upright"
    } else {
        if (isZh) "逆位" else "Reversed"
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppColors.backgroundBase
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = ritual.tarot.card.symbol,
                style = FortunePocketTypography.titleLarge,
                color = AppColors.accentGold
            )
            Text(
                text = ritual.tarot.card.localizedName(isZh),
                style = FortunePocketTypography.bodySmall,
                color = AppColors.textPrimary
            )
            Text(
                text = orientation,
                style = FortunePocketTypography.labelSmall,
                color = AppColors.textSecondary
            )
        }
    }
}

@Composable
fun DailyPreviewCard(
    title: String,
    detail: String,
    accent: Color,
    icon: ImageVector,
    extraContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = AppColors.backgroundElevated
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(10.dp).size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
            }
            extraContent?.invoke()
            Text(
                text = detail,
                style = FortunePocketTypography.bodyMedium,
                color = AppColors.textSecondary
            )
        }
    }
}

@Composable
private fun ExploreSection(
    isZh: Boolean,
    onNavigateToTarot: () -> Unit,
    onNavigateToAstrology: () -> Unit,
    onNavigateToBazi: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionEyebrow(text = if (isZh) "继续探索" else "Explore More")

        FeatureEntryCard(
            icon = Icons.Outlined.AutoStories,
            iconColor = AppColors.accentGold,
            title = stringResource(R.string.home_tarot_title),
            subtitle = stringResource(R.string.home_tarot_desc),
            onClick = onNavigateToTarot
        )
        FeatureEntryCard(
            icon = Icons.Outlined.Stars,
            iconColor = AppColors.accentPurple,
            title = stringResource(R.string.home_astrology_title),
            subtitle = stringResource(R.string.home_astrology_desc),
            onClick = onNavigateToAstrology
        )
        FeatureEntryCard(
            icon = Icons.Outlined.Eco,
            iconColor = AppColors.accentRose,
            title = stringResource(R.string.home_bazi_title),
            subtitle = stringResource(R.string.home_bazi_desc),
            onClick = onNavigateToBazi
        )
    }
}

@Composable
fun SectionEyebrow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "✦",
            style = FortunePocketTypography.labelSmall,
            color = AppColors.accentGold
        )
        Text(
            text = text,
            style = FortunePocketTypography.labelSmall.copy(letterSpacing = 1.6.sp),
            color = AppColors.textSecondary
        )
    }
}

@Composable
fun HeroSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = AppColors.backgroundElevated
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(14.dp)
                    .background(AppColors.backgroundBase, RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(AppColors.backgroundBase, RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(AppColors.backgroundBase, RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(AppColors.accentGold.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            )
        }
    }
}

@Composable
private fun FeatureEntryCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = AppColors.backgroundElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(14.dp).size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.textSecondary
            )
        }
    }
}

private fun DailyRitualDestination.icon(): ImageVector = when (this) {
    DailyRitualDestination.TAROT -> Icons.Outlined.AutoStories
    DailyRitualDestination.ASTROLOGY -> Icons.Outlined.Stars
    DailyRitualDestination.BAZI -> Icons.Outlined.Eco
}
