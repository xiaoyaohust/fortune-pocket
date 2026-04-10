package com.fortunepocket.feature.astrology

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.core.content.bazi.BirthCity
import com.fortunepocket.core.model.AstrologyAspect
import com.fortunepocket.core.model.AstrologyHouseFocus
import com.fortunepocket.core.model.AstrologyPlanetPlacement
import com.fortunepocket.core.model.ReadingPresentationBuilder
import com.fortunepocket.core.model.ZodiacSign
import com.fortunepocket.core.ui.share.OracleShareCardPayload
import com.fortunepocket.core.ui.share.ShareCardExporter
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import com.fortunepocket.feature.astrology.R
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AstrologyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AstrologyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isZh = Locale.getDefault().language == "zh"
    val shareChooserText = stringResource(R.string.astrology_share_chooser)
    val shareButtonText = stringResource(R.string.astrology_share_button)
    val savedText = stringResource(R.string.astrology_saved)
    val saveText = stringResource(R.string.astrology_save)
    val inferredSun = remember(uiState.selectedBirthDateMillis, uiState.signs) {
        viewModel.inferSunSign(uiState)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }

    if (showDatePicker) {
        BirthdayPickerDialog(
            initialMillis = uiState.selectedBirthDateMillis,
            onDateSelected = {
                viewModel.onBirthDateSelected(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        BirthTimePickerDialog(
            initialHour = uiState.selectedBirthHour,
            initialMinute = uiState.selectedBirthMinute,
            onTimeSelected = { hour, minute ->
                viewModel.onBirthTimeSelected(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showCityPicker) {
        AstrologyCityPickerSheet(
            cities = uiState.cities,
            selectedCity = uiState.selectedCity,
            searchQuery = citySearchQuery,
            isZh = isZh,
            onSearchQueryChange = { citySearchQuery = it },
            onSelect = { city ->
                viewModel.onCitySelected(city)
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
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
            FortuneCard {
                Text("✦", style = FortunePocketTypography.titleLarge, color = AppColors.accentGold)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isZh) "用出生时间与地点，生成一张真正离线的本命盘" else "Use birth time and place to reveal a fully offline natal chart",
                    style = FortunePocketTypography.headlineMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isZh)
                        "这不是简单的今日运势模板，而是按出生日期、出生时间、IANA 时区与地点经纬度计算太阳、月亮、上升、主要行星、宫位与相位，再生成解读。"
                    else
                        "This is no longer a lightweight daily horoscope template. The reading is built from your birth date, birth time, IANA time zone, location coordinates, planetary placements, houses, and major aspects.",
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }

            FortuneCard {
                Text(
                    text = if (isZh) "出生资料" else "Birth Details",
                    style = FortunePocketTypography.headlineMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isZh) "出生日期" else "Birth Date",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(Date(uiState.selectedBirthDateMillis)),
                        color = AppColors.textPrimary
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isZh) "出生时间" else "Birth Time",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", uiState.selectedBirthHour, uiState.selectedBirthMinute),
                        color = AppColors.textPrimary
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isZh) "出生城市" else "Birth City",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showCityPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiState.selectedCity?.let { if (isZh) it.nameZh else it.nameEn }
                            ?: if (isZh) "选择城市" else "Choose a city",
                        color = AppColors.textPrimary
                    )
                }

                inferredSun?.let { sign ->
                    Spacer(Modifier.height(16.dp))
                    BadgeRow(
                        badges = listOf(
                            (if (isZh) "太阳星座预览" else "Sun Sign Preview") to "${sign.symbol} ${sign.localizedName(isZh)}"
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))
                uiState.selectedCity?.let { city ->
                    AstrologyInfoPanel(
                        title = if (isZh) "${city.nameZh} · ${city.country}" else "${city.nameEn} · ${city.country}",
                        message = if (isZh)
                            "IANA 时区：${city.timeZoneId}\n法定时区偏移：${utcOffsetLabel(city.utcOffsetHours)}。系统会按这个时区的历史 DST 规则恢复民用出生时。"
                        else
                            "IANA time zone: ${city.timeZoneId}\nStandard legal offset: ${utcOffsetLabel(city.utcOffsetHours)}. The engine restores civil birth time through this zone's historical DST rules."
                    )
                } ?: AstrologyInfoPanel(
                    title = if (isZh) "需要出生城市" else "Birth city required",
                    message = if (isZh)
                        "上升点、宫位和很多角度关系都依赖出生地与法定时区。请选择城市后再生成。"
                    else
                        "The rising sign, houses, and many angular relationships depend on birthplace and legal time zone. Choose a city before generating."
                )

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { viewModel.generate() },
                    enabled = !uiState.isGenerating && uiState.selectedCity != null && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold)
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.backgroundDeep,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = if (isZh) "生成本命盘解读" else "Generate Natal Chart Reading",
                        style = FortunePocketTypography.titleMedium,
                        color = AppColors.backgroundDeep
                    )
                }
            }

            uiState.reading?.let { reading ->
                FortuneCard {
                    Text(
                        text = reading.chartSignature,
                        style = FortunePocketTypography.headlineMedium,
                        color = AppColors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${reading.birthDateText} · ${reading.birthTimeText} · ${reading.birthCityName}",
                        style = FortunePocketTypography.bodyMedium,
                        color = AppColors.textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reading.timeZoneId,
                        style = FortunePocketTypography.bodySmall,
                        color = AppColors.accentGold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = reading.chartSummary,
                        style = FortunePocketTypography.bodyMedium,
                        color = AppColors.textSecondary
                    )

                    Spacer(Modifier.height(16.dp))
                    BadgeRow(
                        badges = listOf(
                            (if (isZh) "太阳" else "Sun") to "${reading.sign.symbol} ${reading.sign.localizedName(isZh)}",
                            (if (isZh) "月亮" else "Moon") to "${reading.moonSign.symbol} ${reading.moonSign.localizedName(isZh)}",
                            (if (isZh) "上升" else "Rising") to "${reading.risingSign?.symbol ?: "↑"} ${reading.risingSign?.localizedName(isZh).orEmpty()}"
                        )
                    )

                    if (reading.planetPlacements.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (isZh) "行星落座与宫位" else "Planet Placements",
                            style = FortunePocketTypography.titleMedium,
                            color = AppColors.textPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        reading.planetPlacements.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { placement ->
                                    PlanetPlacementCard(
                                        placement = placement,
                                        isZh = isZh,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    if (reading.majorAspects.isNotEmpty()) {
                        Text(
                            text = if (isZh) "主要相位" else "Major Aspects",
                            style = FortunePocketTypography.titleMedium,
                            color = AppColors.textPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        reading.majorAspects.forEach { aspect ->
                            AstrologyInfoPanel(
                                title = aspectTitle(aspect, isZh),
                                message = aspectMessage(aspect, isZh)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (reading.houseFocus.isNotEmpty()) {
                        Text(
                            text = if (isZh) "宫位焦点" else "House Focus",
                            style = FortunePocketTypography.titleMedium,
                            color = AppColors.textPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        reading.houseFocus.forEach { focus ->
                            AstrologyInfoPanel(
                                title = focus.localizedTitle(isZh),
                                message = focus.localizedSummary(isZh)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    AstrologySection(title = if (isZh) "整体画像" else "Overall Profile", content = reading.overall)
                    Spacer(Modifier.height(12.dp))
                    AstrologySection(title = if (isZh) "感情与亲密关系" else "Love & Intimacy", content = reading.love)
                    Spacer(Modifier.height(12.dp))
                    AstrologySection(title = if (isZh) "事业与成长方向" else "Career & Direction", content = reading.career)
                    Spacer(Modifier.height(12.dp))
                    AstrologySection(title = if (isZh) "财富与安全感" else "Wealth & Security", content = reading.wealth)
                    Spacer(Modifier.height(12.dp))
                    AstrologySection(title = if (isZh) "社交与沟通" else "Social & Communication", content = reading.social)
                    Spacer(Modifier.height(12.dp))
                    AstrologySection(title = if (isZh) "给你的建议" else "Guidance", content = reading.advice)

                    Spacer(Modifier.height(16.dp))
                    BadgeRow(
                        badges = listOf(
                            (if (isZh) "主导元素" else "Dominant Element") to reading.elementBalance.localizedDominantElement(isZh),
                            (if (isZh) "幸运色" else "Lucky Color") to reading.luckyColor,
                            (if (isZh) "幸运数字" else "Lucky Number") to reading.luckyNumber.toString()
                        )
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareText = ReadingPresentationBuilder.shareText(reading, isZh)
                                val shareUri = ShareCardExporter.export(
                                    context = context,
                                    payload = astrologySharePayload(reading = reading, isZh = isZh),
                                    fileName = "natal-${reading.birthDateText.hashCode()}-${reading.birthTimeText.hashCode()}"
                                )
                                context.startActivity(
                                    ShareCardExporter.shareIntent(
                                        uri = shareUri,
                                        chooserTitle = shareChooserText,
                                        shareText = shareText
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text(shareButtonText, color = AppColors.textPrimary)
                        }

                        Button(
                            onClick = { viewModel.saveReading() },
                            enabled = !uiState.hasSavedCurrentReading,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold)
                        ) {
                            Text(
                                text = if (uiState.hasSavedCurrentReading) savedText else saveText,
                                color = AppColors.backgroundDeep
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.error
                )
            }
        }
    }
}

private fun astrologySharePayload(
    reading: com.fortunepocket.core.model.HoroscopeReading,
    isZh: Boolean
): OracleShareCardPayload {
    return OracleShareCardPayload(
        eyebrow = if (isZh) "FORTUNE POCKET · 本命盘" else "FORTUNE POCKET · NATAL CHART",
        title = reading.chartSignature,
        subtitle = listOf(reading.birthDateText, reading.birthTimeText, reading.birthCityName).joinToString(" · "),
        headline = reading.chartSummary,
        summary = reading.overall,
        guidance = reading.advice,
        footer = if (isZh)
            "幸运提示：${reading.luckyColor} · ${reading.luckyNumber}"
        else
            "Lucky hint: ${reading.luckyColor} · ${reading.luckyNumber}"
    )
}

@Composable
private fun FortuneCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.backgroundElevated
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

@Composable
private fun AstrologySection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.backgroundBase, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary)
        Text(content, style = FortunePocketTypography.bodyMedium, color = AppColors.textSecondary)
    }
}

@Composable
private fun AstrologyInfoPanel(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.backgroundBase
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = FortunePocketTypography.bodyMedium, color = AppColors.textPrimary)
            Text(message, style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary)
        }
    }
}

@Composable
private fun BadgeRow(badges: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        badges.forEach { (title, value) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AppColors.backgroundBase
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(title, style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary)
                    Text(value, style = FortunePocketTypography.bodyMedium, color = AppColors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun PlanetPlacementCard(
    placement: AstrologyPlanetPlacement,
    isZh: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = AppColors.backgroundBase
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = placement.planetId.localizedName(isZh),
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
                if (placement.isRetrograde) {
                    Text("Rx", style = FortunePocketTypography.bodySmall, color = AppColors.accentGold)
                }
            }
            Text(
                text = placement.localizedSignName(isZh),
                style = FortunePocketTypography.bodyMedium,
                color = AppColors.textSecondary
            )
            Text(
                text = String.format(
                    Locale.US,
                    if (isZh) "%.1f° · 第%s宫" else "%.1f° · House %s",
                    placement.degreeInSign,
                    placement.house?.toString() ?: "-"
                ),
                style = FortunePocketTypography.bodySmall,
                color = AppColors.textMuted
            )
        }
    }
}

private fun aspectTitle(aspect: AstrologyAspect, isZh: Boolean): String {
    return "${aspect.firstPlanetId.localizedName(isZh)} · ${aspect.type.localizedName(isZh)} · ${aspect.secondPlanetId.localizedName(isZh)}"
}

private fun aspectMessage(aspect: AstrologyAspect, isZh: Boolean): String {
    val orbText = String.format(Locale.US, "%.1f°", aspect.orbDegrees)
    return if (isZh) {
        "容许度 $orbText。这个角度会放大这两颗行星之间的互动感。"
    } else {
        "Orb $orbText. This angle amplifies the dialogue between the two planets."
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
private fun BirthTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(initialHour, initialMinute) {
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(hour, minute) },
            initialHour,
            initialMinute,
            true
        )
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AstrologyCityPickerSheet(
    cities: List<BirthCity>,
    selectedCity: BirthCity?,
    searchQuery: String,
    isZh: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelect: (BirthCity) -> Unit,
    onDismiss: () -> Unit
) {
    val normalizedQuery = searchQuery.trim()
    val popularIds = listOf(
        "beijing", "shanghai", "guangzhou", "shenzhen", "chengdu",
        "hongkong", "taipei", "singapore", "tokyo",
        "london", "new_york", "los_angeles", "seattle", "sydney"
    )
    val popularCities = popularIds.mapNotNull { id -> cities.firstOrNull { it.id == id } }
    val searchResults = cities
        .filter { city ->
            normalizedQuery.isNotEmpty() && (
                city.id.contains(normalizedQuery, ignoreCase = true) ||
                    city.nameZh.contains(normalizedQuery, ignoreCase = true) ||
                    city.nameEn.contains(normalizedQuery, ignoreCase = true) ||
                    city.country.contains(normalizedQuery, ignoreCase = true) ||
                    city.timeZoneId.contains(normalizedQuery, ignoreCase = true) ||
                    city.searchAliases.any { it.contains(normalizedQuery, ignoreCase = true) }
                )
        }
        .sortedWith(compareBy<BirthCity> { !popularIds.contains(it.id) }.thenBy { if (isZh) it.nameZh else it.nameEn })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.backgroundBase
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isZh) "选择出生城市" else "Choose Birth City",
                style = FortunePocketTypography.headlineMedium,
                color = AppColors.textPrimary
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = {
                    Text(
                        if (isZh) "搜索城市 / 国家 / 时区，如邵阳 / Seattle"
                        else "Search city / country / time zone, e.g. Shaoyang / Seattle"
                    )
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (normalizedQuery.isEmpty()) {
                    if (popularCities.isNotEmpty()) {
                        Text(
                            text = if (isZh) "常用城市" else "Popular Cities",
                            style = FortunePocketTypography.titleMedium,
                            color = AppColors.accentGold
                        )
                        popularCities.forEach { city ->
                            CityChoiceCard(
                                title = if (isZh) city.nameZh else city.nameEn,
                                subtitle = "${city.timeZoneId} · ${city.country}",
                                isSelected = selectedCity?.id == city.id,
                                onClick = { onSelect(city) }
                            )
                        }
                    }
                    AstrologyInfoPanel(
                        title = if (isZh) "搜索更快" else "Search Is Faster",
                        message = if (isZh)
                            "目前已收录 ${cities.size} 个城市与区域中心，直接搜索城市名、英文名、国家代码或时区会更快。"
                        else
                            "${cities.size} cities and regional hubs are currently included. Searching by city name, English name, country code, or time zone is the fastest path."
                    )
                } else if (searchResults.isEmpty()) {
                    AstrologyInfoPanel(
                        title = if (isZh) "没有匹配的城市" else "No matching city",
                        message = if (isZh)
                            "请尝试中文名、英文名、国家代码或时区名，例如“邵阳”或“Seattle”。"
                        else
                            "Try a Chinese name, English name, country code, or time-zone name, for example “Shaoyang” or “Seattle”."
                    )
                } else {
                    Text(
                        text = (if (isZh) "搜索结果" else "Search Results") + " (${searchResults.size})",
                        style = FortunePocketTypography.titleMedium,
                        color = AppColors.accentGold
                    )
                    searchResults.forEach { city ->
                        CityChoiceCard(
                            title = if (isZh) city.nameZh else city.nameEn,
                            subtitle = "${city.timeZoneId} · ${city.country}",
                            isSelected = selectedCity?.id == city.id,
                            onClick = { onSelect(city) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CityChoiceCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.backgroundElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = FortunePocketTypography.bodyMedium, color = AppColors.textPrimary)
                Text(subtitle, style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary)
            }
            if (isSelected) {
                Text("✓", style = FortunePocketTypography.titleMedium, color = AppColors.accentGold)
            }
        }
    }
}

private fun utcOffsetLabel(offsetHours: Double): String {
    val sign = if (offsetHours >= 0) "+" else "-"
    val absolute = kotlin.math.abs(offsetHours)
    val wholeHours = absolute.toInt()
    val minutes = ((absolute - wholeHours.toDouble()) * 60.0).toInt()
    return String.format(Locale.US, "UTC%s%02d:%02d", sign, wholeHours, minutes)
}
