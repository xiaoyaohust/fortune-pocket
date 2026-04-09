package com.fortunepocket.feature.home

import com.fortunepocket.feature.home.R
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.core.model.DailyQuote
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToTarot: () -> Unit,
    onNavigateToAstrology: () -> Unit,
    onNavigateToBazi: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Header ──────────────────────────────────────────────────────
            HomeHeader()

            Spacer(Modifier.height(28.dp))

            // ── Daily Quote ──────────────────────────────────────────────────
            if (uiState.isLoading) {
                QuoteCardShimmer()
            } else {
                uiState.dailyQuote?.let { QuoteCard(quote = it) }
            }

            Spacer(Modifier.height(36.dp))

            // ── Section title ────────────────────────────────────────────────
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
                    text = stringResource(R.string.today_section_title).uppercase(Locale.getDefault()),
                    style = FortunePocketTypography.labelSmall.copy(letterSpacing = 2.sp),
                    color = AppColors.textSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Feature cards ────────────────────────────────────────────────
            FeatureEntryCard(
                icon = Icons.Outlined.AutoStories,
                iconColor = AppColors.accentGold,
                title = stringResource(R.string.home_tarot_title),
                subtitle = stringResource(R.string.home_tarot_desc),
                onClick = onNavigateToTarot
            )
            Spacer(Modifier.height(14.dp))
            FeatureEntryCard(
                icon = Icons.Outlined.Stars,
                iconColor = AppColors.accentPurple,
                title = stringResource(R.string.home_astrology_title),
                subtitle = stringResource(R.string.home_astrology_desc),
                onClick = onNavigateToAstrology
            )
            Spacer(Modifier.height(14.dp))
            FeatureEntryCard(
                icon = Icons.Outlined.Eco,
                iconColor = AppColors.accentRose,
                title = stringResource(R.string.home_bazi_title),
                subtitle = stringResource(R.string.home_bazi_desc),
                onClick = onNavigateToBazi
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// MARK: - Header

@Composable
private fun HomeHeader() {
    val dateStr = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(Date())

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            text = stringResource(R.string.app_name_zh),
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

// MARK: - Quote Card

@Composable
private fun QuoteCard(quote: DailyQuote) {
    val isZh = Locale.getDefault().language == "zh"
    val text = if (isZh) quote.textZh else quote.textEn

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.backgroundElevated,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ornament
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppColors.accentGold.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
                Text(
                    text = "✦ ✧ ✦",
                    style = FortunePocketTypography.labelSmall,
                    color = AppColors.accentGold.copy(alpha = 0.6f)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppColors.accentGold.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
            }

            Spacer(Modifier.height(18.dp))

            // Quote text
            Text(
                text = text,
                style = FortunePocketTypography.bodyLarge.copy(
                    fontFeatureSettings = "\"smcp\"",
                    lineHeight = 26.sp
                ),
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Attribution
            Text(
                text = "— Fortune Pocket",
                style = FortunePocketTypography.labelSmall,
                color = AppColors.accentGold.copy(alpha = 0.7f)
            )
        }
    }
}

// MARK: - Shimmer (loading)

@Composable
private fun QuoteCardShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.backgroundElevated.copy(alpha = alpha))
    )
}

// MARK: - Feature Entry Card

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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.backgroundElevated,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
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

            // Chevron
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
