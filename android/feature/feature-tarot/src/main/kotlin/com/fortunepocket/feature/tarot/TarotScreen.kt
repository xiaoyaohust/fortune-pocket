package com.fortunepocket.feature.tarot

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.core.model.DrawnCard
import com.fortunepocket.core.model.ReadingPresentationBuilder
import com.fortunepocket.core.model.TarotQuestionTheme
import com.fortunepocket.core.model.TarotReading
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import com.fortunepocket.feature.tarot.components.CardSize
import com.fortunepocket.feature.tarot.components.TarotCardView
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TarotScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TarotViewModel = hiltViewModel()
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
        uiState.reading?.let { reading ->
            ResultScreen(
                reading = reading,
                onSaveAndClose = { viewModel.saveAndReset() }
            )
        } ?: DrawPromptScreen(
            state = uiState,
            onThemeSelected = viewModel::onThemeSelected,
            onDraw = viewModel::draw
        )
    }
}

@Composable
private fun DrawPromptScreen(
    state: TarotUiState,
    onThemeSelected: (TarotQuestionTheme) -> Unit,
    onDraw: () -> Unit
) {
    val isZh = Locale.getDefault().language == "zh"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✦", style = FortunePocketTypography.titleLarge, color = AppColors.accentGold)
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.selectedTheme.localizedSpreadName(isZh),
            style = FortunePocketTypography.headlineLarge,
            color = AppColors.textPrimary
        )
        Text(
            text = state.selectedTheme.localizedSpreadSubtitle(isZh),
            style = FortunePocketTypography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.selectedTheme.localizedPromptDescription(isZh),
            style = FortunePocketTypography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        ThemeRow(
            themes = listOf(TarotQuestionTheme.GENERAL, TarotQuestionTheme.LOVE),
            selectedTheme = state.selectedTheme,
            isZh = isZh,
            onThemeSelected = onThemeSelected
        )
        Spacer(Modifier.height(10.dp))
        ThemeRow(
            themes = listOf(TarotQuestionTheme.CAREER, TarotQuestionTheme.WEALTH),
            selectedTheme = state.selectedTheme,
            isZh = isZh,
            onThemeSelected = onThemeSelected
        )

        Spacer(Modifier.height(34.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                TarotCardView(faceDown = true, size = CardSize.MEDIUM)
            }
        }

        Spacer(Modifier.height(44.dp))

        Text(
            text = if (isZh)
                "先确认你想询问的主题，再静心抽出三张牌。\n结果会围绕牌阵位置与主题本身来展开。"
            else
                "Choose your focus first, then draw three cards.\nThe reading will follow the spread positions and your selected theme.",
            style = FortunePocketTypography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        if (state.isGenerating) {
            CircularProgressIndicator(color = AppColors.accentGold)
        } else {
            Button(
                onClick = onDraw,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.tarot_begin_btn),
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.backgroundDeep
                )
            }
        }

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = FortunePocketTypography.bodyMedium,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ThemeRow(
    themes: List<TarotQuestionTheme>,
    selectedTheme: TarotQuestionTheme,
    isZh: Boolean,
    onThemeSelected: (TarotQuestionTheme) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        themes.forEach { theme ->
            val selected = theme == selectedTheme
            Button(
                onClick = { onThemeSelected(theme) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) AppColors.accentGold else AppColors.backgroundElevated
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Text(
                    text = theme.localizedName(isZh),
                    style = FortunePocketTypography.bodyMedium,
                    color = if (selected) AppColors.backgroundDeep else AppColors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(reading: TarotReading, onSaveAndClose: () -> Unit) {
    val context = LocalContext.current
    val isZh = Locale.getDefault().language == "zh"
    val dateStr = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(Date(reading.timestamp))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✦", style = FortunePocketTypography.titleLarge, color = AppColors.accentGold)
            Spacer(Modifier.height(8.dp))
            Text(reading.spreadName, style = FortunePocketTypography.headlineLarge, color = AppColors.textPrimary)
            Text(reading.theme.localizedName(isZh), style = FortunePocketTypography.bodyMedium, color = AppColors.accentGold)
            Text(dateStr, style = FortunePocketTypography.bodyMedium, color = AppColors.textSecondary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = reading.spreadDescription,
                style = FortunePocketTypography.bodyMedium,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            reading.drawnCards.forEach { drawn ->
                DrawnCardColumn(drawn = drawn)
            }
        }

        Spacer(Modifier.height(24.dp))
        GoldDivider()
        ReadingSection(title = stringResource(R.string.tarot_section_overall), content = reading.overallEnergy)
        GoldDivider()
        ReadingSection(title = reading.theme.localizedFocusTitle(isZh), content = reading.focusInsight)

        reading.positionReadings.forEach { item ->
            GoldDivider()
            ReadingSection(title = item.positionLabel, content = item.interpretation)
        }

        GoldDivider()
        ReadingSection(title = stringResource(R.string.tarot_section_advice), content = reading.advice)
        GoldDivider()
        LuckySection(reading = reading)
        GoldDivider()

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, ReadingPresentationBuilder.shareText(reading, isZh))
                            },
                            if (isZh) "分享占卜结果" else "Share Reading"
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.backgroundElevated),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(
                    text = if (isZh) "分享结果" else "Share",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
            }

            Button(
                onClick = onSaveAndClose,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(
                    text = if (isZh) "保存并完成" else "Save & Finish",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.backgroundDeep
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DrawnCardColumn(drawn: DrawnCard) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TarotCardView(card = drawn.card, isUpright = drawn.isUpright, size = CardSize.SMALL)
        Spacer(Modifier.height(8.dp))
        Text(
            text = drawn.positionLabel,
            style = FortunePocketTypography.labelSmall,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReadingSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = FortunePocketTypography.titleMedium,
            color = AppColors.accentGold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = content,
            style = FortunePocketTypography.bodyMedium.copy(
                color = AppColors.textPrimary,
                lineHeight = 22.sp
            )
        )
    }
}

@Composable
private fun LuckySection(reading: TarotReading) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = stringResource(R.string.tarot_lucky_title),
            style = FortunePocketTypography.titleMedium,
            color = AppColors.accentGold
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LuckyCell(label = stringResource(R.string.tarot_lucky_charm), value = reading.luckyItemName)
            VerticalGoldLine()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.tarot_lucky_color),
                    style = FortunePocketTypography.labelSmall,
                    color = AppColors.textSecondary
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(reading.luckyColorHex))
                                } catch (_: Exception) {
                                    AppColors.accentGold
                                },
                                RoundedCornerShape(50)
                            )
                    )
                    Text(
                        text = reading.luckyColorName,
                        style = FortunePocketTypography.bodyMedium,
                        color = AppColors.textPrimary
                    )
                }
            }
            VerticalGoldLine()
            LuckyCell(
                label = stringResource(R.string.tarot_lucky_number),
                value = reading.luckyNumber.toString(),
                valueColor = AppColors.accentGold,
                valueLarge = true
            )
        }
    }
}

@Composable
private fun LuckyCell(
    label: String,
    value: String,
    valueColor: Color = AppColors.textPrimary,
    valueLarge: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = FortunePocketTypography.labelSmall, color = AppColors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = if (valueLarge) FortunePocketTypography.headlineMedium else FortunePocketTypography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoldDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = AppColors.accentGold.copy(alpha = 0.15f)
    )
}

@Composable
private fun VerticalGoldLine() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(36.dp)
            .background(AppColors.accentGold.copy(alpha = 0.25f))
    )
}
