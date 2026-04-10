package com.fortunepocket.feature.bazi

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fortunepocket.core.content.bazi.BaziChart
import com.fortunepocket.core.content.bazi.BirthCity
import com.fortunepocket.core.content.bazi.ElementNamesEn
import com.fortunepocket.core.content.bazi.ElementNamesZh
import com.fortunepocket.core.content.bazi.Gender
import com.fortunepocket.core.content.bazi.HiddenStemEntry
import com.fortunepocket.core.content.bazi.HiddenStemWeight
import com.fortunepocket.core.content.bazi.MajorCycle
import com.fortunepocket.core.content.bazi.Pillar
import com.fortunepocket.core.content.bazi.StemElements
import com.fortunepocket.core.content.bazi.StemNames
import com.fortunepocket.core.content.bazi.TenGodEntry
import com.fortunepocket.core.ui.share.OracleShareCardPayload
import com.fortunepocket.core.ui.share.ShareCardExporter
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.core.ui.theme.FortunePocketTypography
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaziScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BaziViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isZh = Locale.getDefault().language == "zh"
    var showDatePicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }
    var showHourMenu   by remember { mutableStateOf(false) }
    var showMinuteMenu by remember { mutableStateOf(false) }

    if (showDatePicker) {
        BirthdayPickerDialog(
            initialMillis = state.selectedBirthDateMillis,
            onDateSelected = { viewModel.onBirthDateSelected(it); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showCityPicker) {
        BaziCityPickerSheet(
            cities = state.cities,
            selectedCity = state.selectedCity,
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
            .background(Brush.verticalGradient(listOf(AppColors.backgroundDeep, AppColors.backgroundBase)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Intro card
            BaziCard {
                Text("☯", style = FortunePocketTypography.titleLarge, color = AppColors.accentGold)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isZh) "四柱八字" else "Four Pillars (Bazi)",
                    style = FortunePocketTypography.headlineMedium, color = AppColors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isZh)
                        "依节气推算四柱，含大运、十神、藏干、五行力量。仅供参考。"
                    else
                        "Solar-term-based four-pillar chart with major cycles, ten gods, hidden stems, and five element balance. For reference only.",
                    style = FortunePocketTypography.bodyMedium, color = AppColors.textSecondary
                )
            }

            // Input card
            BaziCard {
                // Birth date
                Text(
                    text = if (isZh) "出生日期" else "Birth Date",
                    style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
                            .format(Date(state.selectedBirthDateMillis)),
                        style = FortunePocketTypography.bodyLarge, color = AppColors.textPrimary
                    )
                }

                Spacer(Modifier.height(12.dp))
                PrecisionSummaryPanel(state = state, isZh = isZh)

                Spacer(Modifier.height(18.dp))
                GoldDivider()
                Spacer(Modifier.height(18.dp))

                // Gender
                Text(
                    text = if (isZh) "性别" else "Gender",
                    style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Gender.MALE to (if (isZh) "男" else "Male"),
                           Gender.FEMALE to (if (isZh) "女" else "Female")).forEach { (gender, label) ->
                        Button(
                            onClick = { viewModel.onGenderChanged(gender) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.selectedGender == gender)
                                    AppColors.accentGold else AppColors.backgroundBase
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (state.selectedGender == gender)
                                    AppColors.backgroundDeep else AppColors.textPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                GoldDivider()
                Spacer(Modifier.height(18.dp))

                // Birth time toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isZh) "出生时间（建议填写）" else "Birth Time (recommended)",
                        style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary
                    )
                    Switch(
                        checked = state.includesBirthHour,
                        onCheckedChange = { viewModel.onIncludesBirthHourChanged(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppColors.backgroundDeep,
                            checkedTrackColor = AppColors.accentGold)
                    )
                }

                if (state.includesBirthHour) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Hour
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showHourMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    hourLabel(hour = state.selectedBirthHour, isZh = isZh),
                                    color = AppColors.textPrimary
                                )
                            }
                            DropdownMenu(expanded = showHourMenu, onDismissRequest = { showHourMenu = false }) {
                                (0..23).forEach { h ->
                                    DropdownMenuItem(
                                        text = { Text(hourLabel(hour = h, isZh = isZh)) },
                                        onClick = { viewModel.onBirthHourSelected(h); showHourMenu = false }
                                    )
                                }
                            }
                        }

                        // Minute
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showMinuteMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    minuteLabel(minute = state.selectedBirthMinute, isZh = isZh),
                                    color = AppColors.textPrimary
                                )
                            }
                            DropdownMenu(expanded = showMinuteMenu, onDismissRequest = { showMinuteMenu = false }) {
                                (0..59).forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(minuteLabel(minute = m, isZh = isZh)) },
                                        onClick = { viewModel.onBirthMinuteSelected(m); showMinuteMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    GoldDivider()
                    Spacer(Modifier.height(14.dp))

                    // City picker
                    Text(
                        text = if (isZh) "出生城市与时区" else "Birth City & Time Zone",
                        style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                citySearchQuery = ""
                                showCityPicker = true
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = AppColors.backgroundBase
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
                                Text(
                                    text = state.selectedCity?.let { if (isZh) it.nameZh else it.nameEn }
                                        ?: if (isZh) "未选择城市（按北京时间估算）" else "No city (Beijing legal time)",
                                    style = FortunePocketTypography.bodyMedium,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = if (isZh)
                                        "搜索城市、英文名、国家或时区"
                                    else
                                        "Search by city, English name, country, or time zone",
                                    style = FortunePocketTypography.bodySmall,
                                    color = AppColors.textSecondary
                                )
                            }
                            Text(
                                text = "⌕",
                                style = FortunePocketTypography.titleMedium,
                                color = AppColors.accentGold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    state.selectedCity?.let { city ->
                        CityMetadataPanel(city = city, isZh = isZh)
                    } ?: BaziInfoPanel(
                        title = if (isZh) "当前兜底时区：Asia/Shanghai" else "Current fallback zone: Asia/Shanghai",
                        message = if (isZh)
                            "如果未选城市，系统会按北京时间 `UTC+8` 民用时估算。这适合常见中国大陆场景，但不适合海外出生或需要处理 DST 的案例。"
                        else
                            "Without a city, the engine falls back to Beijing legal time `UTC+8`. This is acceptable for common mainland-China cases, but not for overseas births or DST-sensitive charts."
                    )

                    Spacer(Modifier.height(14.dp))
                    GoldDivider()
                    Spacer(Modifier.height(14.dp))

                    // True solar time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isZh) "真太阳时（经度 + 均时差）" else "True Solar Time (Longitude + EoT)",
                                style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary
                            )
                            Text(
                                text = if (isZh)
                                    "基于城市经度与均时差修正地方太阳时，需要先选择城市"
                                else
                                    "Adjust local apparent solar time with longitude and equation of time; requires a city",
                                style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary
                            )
                        }
                        Switch(
                            checked = state.useTrueSolarTime,
                            onCheckedChange = { viewModel.onTrueSolarTimeChanged(it) },
                            enabled = state.selectedCity != null,
                            colors = SwitchDefaults.colors(checkedThumbColor = AppColors.backgroundDeep,
                                checkedTrackColor = AppColors.accentGold)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Late Zi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isZh) "晚子时归次日" else "Late Zi Hour → Next Day",
                                style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary
                            )
                            Text(
                                text = if (isZh)
                                    if (state.selectedBirthHour == 23)
                                        "当前出生时刻落在 23:00–23:59，开启后会将其并入次日"
                                    else
                                        "仅在 23:00–23:59 生效，用于区分早子时/晚子时"
                                else
                                    if (state.selectedBirthHour == 23)
                                        "The selected birth time is within 23:00–23:59; enabling this moves it to the next day"
                                    else
                                        "Only applies to 23:00–23:59 when distinguishing early vs late Zi hour",
                                style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary
                            )
                        }
                        Switch(
                            checked = state.distinguishLateZi,
                            onCheckedChange = { viewModel.onDistinguishLateZiChanged(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AppColors.backgroundDeep,
                                checkedTrackColor = AppColors.accentGold)
                        )
                    }
                }
                else {
                    Spacer(Modifier.height(12.dp))
                    BaziInfoPanel(
                        title = if (isZh) "为什么时柱是空的？" else "Why is the hour pillar empty?",
                        message = if (isZh)
                            "你当前没有填写出生时间，所以系统只按年、月、日三柱排盘，时柱会保持未知。若知道大概时分，建议补上，盘面会完整很多。"
                        else
                            "You have not entered a birth time yet, so the engine is using only the year, month, and day pillars. Add an approximate time if you know it for a much fuller chart."
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.onIncludesBirthHourChanged(true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isZh) "补充出生时间" else "Add birth time",
                            color = AppColors.accentGold
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { viewModel.generate() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.backgroundDeep, strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = if (isZh) "排盘" else "Calculate Chart",
                        style = FortunePocketTypography.titleMedium, color = AppColors.backgroundDeep
                    )
                }
            }

            // Chart result
            state.chart?.let { chart ->
                BaziChartCard(chart = chart, isZh = isZh,
                    hasSaved = state.hasSavedCurrentChart, onSave = { viewModel.saveReading() })
            }

            // Error
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = FortunePocketTypography.bodyMedium, color = AppColors.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// MARK: - Chart display

@Composable
private fun BaziChartCard(
    chart: BaziChart, isZh: Boolean, hasSaved: Boolean, onSave: () -> Unit
) {
    val context = LocalContext.current
    val insightSections = buildBaziInsightSections(chart = chart, isZh = isZh)

    BaziCard {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = AppColors.backgroundBase
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isZh) "命盘第一眼" else "First Look",
                    style = FortunePocketTypography.labelSmall,
                    color = AppColors.accentGold
                )
                Text(
                    text = insightSections.firstOrNull()?.title ?: if (isZh) "先看日主主轴" else "Start With the Day Master",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
                Text(
                    text = insightSections.firstOrNull()?.body.orEmpty(),
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        GoldDivider()
        Spacer(Modifier.height(16.dp))

        // Four pillars
        Text(
            text = if (isZh) "四柱命盘" else "Four Pillars Chart",
            style = FortunePocketTypography.headlineMedium, color = AppColors.textPrimary
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillarColumn(
                label = if (isZh) "年柱" else "Year",
                pillar = chart.yearPillar,
                hiddenStems = chart.hiddenStems[chart.yearPillar.normalizedBranch] ?: emptyList(),
                isDayMaster = false, modifier = Modifier.weight(1f)
            )
            PillarColumn(
                label = if (isZh) "月柱" else "Month",
                pillar = chart.monthPillar,
                hiddenStems = chart.hiddenStems[chart.monthPillar.normalizedBranch] ?: emptyList(),
                isDayMaster = false, modifier = Modifier.weight(1f)
            )
            PillarColumn(
                label = if (isZh) "日柱" else "Day",
                pillar = chart.dayPillar,
                hiddenStems = chart.hiddenStems[chart.dayPillar.normalizedBranch] ?: emptyList(),
                isDayMaster = true, modifier = Modifier.weight(1f)
            )
            chart.hourPillar?.let { hp ->
                PillarColumn(
                    label = if (isZh) "时柱" else "Hour",
                    pillar = hp,
                    hiddenStems = chart.hiddenStems[hp.normalizedBranch] ?: emptyList(),
                    isDayMaster = false, modifier = Modifier.weight(1f)
                )
            } ?: UnknownHourColumn(isZh = isZh, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        GoldDivider()
        Spacer(Modifier.height(16.dp))

        BaziInterpretationSection(chart = chart, isZh = isZh)

        Spacer(Modifier.height(18.dp))
        GoldDivider()
        Spacer(Modifier.height(16.dp))

        // Ten gods
        Text(
            text = if (isZh) "十神" else "Ten Gods",
            style = FortunePocketTypography.titleMedium, color = AppColors.accentGold
        )
        Spacer(Modifier.height(10.dp))
        listOf(
            Triple(if (isZh) "年" else "Yr", chart.tenGods.yearStemGod,  chart.tenGods.yearBranchGod),
            Triple(if (isZh) "月" else "Mo", chart.tenGods.monthStemGod, chart.tenGods.monthBranchGod),
            Triple(if (isZh) "时" else "Hr", chart.tenGods.hourStemGod,  chart.tenGods.hourBranchGod),
        ).forEach { (label, stemGod, branchGod) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary,
                    modifier = Modifier.width(24.dp))
                Text(
                    text = if (isZh) "天干：${stemGod?.nameZh ?: "—"}"
                           else "Stem: ${stemGod?.nameEn ?: "—"}",
                    style = FortunePocketTypography.bodySmall, color = AppColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isZh) "地支：${branchGod?.nameZh ?: "—"}"
                           else "Branch: ${branchGod?.nameEn ?: "—"}",
                    style = FortunePocketTypography.bodySmall, color = AppColors.textPrimary
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        GoldDivider()
        Spacer(Modifier.height(16.dp))

        // Five elements
        Text(
            text = if (isZh) "五行力量" else "Five Element Balance",
            style = FortunePocketTypography.titleMedium, color = AppColors.accentGold
        )
        Spacer(Modifier.height(10.dp))
        val total = maxOf(1, chart.fiveElements.total).toFloat()
        val elements = listOf(
            Triple(if (isZh) "木" else "Wood",  chart.fiveElements.wood,  Color(0xFF4CAF50)),
            Triple(if (isZh) "火" else "Fire",  chart.fiveElements.fire,  Color(0xFFF44336)),
            Triple(if (isZh) "土" else "Earth", chart.fiveElements.earth, Color(0xFFFF9800)),
            Triple(if (isZh) "金" else "Metal", chart.fiveElements.metal, Color(0xFF9E9E9E)),
            Triple(if (isZh) "水" else "Water", chart.fiveElements.water, Color(0xFF2196F3)),
        )
        elements.forEach { (name, value, color) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(name, style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary,
                    modifier = Modifier.width(28.dp))
                LinearProgressIndicator(
                    progress = { value.toFloat() / total },
                    modifier = Modifier.weight(1f).height(10.dp),
                    color = color, trackColor = AppColors.backgroundBase
                )
                Text("$value", style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary,
                    modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
            }
        }

        Spacer(Modifier.height(18.dp))
        GoldDivider()
        Spacer(Modifier.height(16.dp))

        // Major cycles
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isZh) "大运（${chart.cycleDirection}）" else "Major Cycles (${chart.cycleDirection})",
                style = FortunePocketTypography.titleMedium, color = AppColors.accentGold
            )
            Text(
                text = if (isZh) "起运：${chart.startingAge}岁" else "Starts age ${chart.startingAge}",
                style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chart.majorCycles.take(8).forEach { cycle ->
                Column(
                    modifier = Modifier
                        .background(AppColors.backgroundBase, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = cycle.pillar.stemZh + cycle.pillar.branchZh,
                        style = FortunePocketTypography.titleMedium, color = AppColors.textPrimary
                    )
                    Text(
                        text = "${cycle.startAge}–${cycle.startAge + 9}",
                        style = FortunePocketTypography.bodySmall, color = AppColors.textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        GoldDivider()
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val shareText = buildBaziShareText(chart = chart, insightSections = insightSections, isZh = isZh)
                    val shareUri = ShareCardExporter.export(
                        context = context,
                        payload = baziSharePayload(chart = chart, insightSections = insightSections, isZh = isZh),
                        fileName = "bazi-${chart.startingAge}-${chart.dayPillar.nameEn.lowercase(Locale.US)}"
                    )
                    context.startActivity(
                        ShareCardExporter.shareIntent(
                            uri = shareUri,
                            chooserTitle = if (isZh) "分享命盘结果" else "Share Chart",
                            shareText = shareText
                        )
                    )
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isZh) "分享结果" else "Share",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary
                )
            }

            Button(
                onClick = onSave,
                enabled = !hasSaved,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentGold)
            ) {
                Text(
                    text = if (hasSaved) (if (isZh) "已保存" else "Saved")
                           else (if (isZh) "保存记录" else "Save"),
                    style = FortunePocketTypography.titleMedium, color = AppColors.backgroundDeep
                )
            }
        }
    }
}

@Composable
private fun PillarColumn(
    label: String, pillar: Pillar, hiddenStems: List<HiddenStemEntry>,
    isDayMaster: Boolean, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label, fontSize = 11.sp,
            color = if (isDayMaster) AppColors.accentGold else AppColors.textSecondary
        )

        // Stem
        Surface(
            color = if (isDayMaster) AppColors.accentGold.copy(alpha = 0.15f) else AppColors.backgroundBase,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = pillar.stemZh,
                    style = FortunePocketTypography.titleMedium,
                    color = if (isDayMaster) AppColors.accentGold else AppColors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Branch
        Surface(
            color = AppColors.backgroundBase,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = pillar.branchZh,
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.textPrimary, textAlign = TextAlign.Center
                )
            }
        }

        // Hidden stems
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            hiddenStems.forEach { hs ->
                Text(
                    text = StemNames[hs.stemIndex], fontSize = 10.sp,
                    color = when (hs.weight) {
                        HiddenStemWeight.MAIN  -> AppColors.textPrimary
                        HiddenStemWeight.MID   -> AppColors.textSecondary
                        HiddenStemWeight.MINOR -> AppColors.textSecondary.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}

@Composable
private fun UnknownHourColumn(isZh: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isZh) "时柱" else "Hour", fontSize = 11.sp, color = AppColors.textSecondary
        )
        Surface(
            color = AppColors.backgroundBase, shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(92.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isZh) "未填" else "Missing",
                    style = FortunePocketTypography.bodyMedium, color = AppColors.textSecondary
                )
            }
        }
    }
}

// MARK: - Shared helpers

@Composable
private fun BaziCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.backgroundElevated
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun GoldDivider() {
    HorizontalDivider(color = AppColors.accentGold.copy(alpha = 0.2f), thickness = 1.dp)
}

@Composable
private fun PrecisionSummaryPanel(state: BaziUiState, isZh: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isZh) "排盘精度与时制" else "Precision & Time Basis",
            style = FortunePocketTypography.titleMedium,
            color = AppColors.textPrimary
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            precisionTags(state = state, isZh = isZh).forEach { tag ->
                PrecisionChip(text = tag)
            }
        }
        BaziInfoPanel(
            title = precisionTitle(state = state, isZh = isZh),
            message = precisionDescription(state = state, isZh = isZh)
        )
    }
}

@Composable
private fun PrecisionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AppColors.backgroundBase
    ) {
        Text(
            text = text,
            style = FortunePocketTypography.bodySmall,
            color = AppColors.accentGold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BaziInfoPanel(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.backgroundBase
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = FortunePocketTypography.bodyMedium,
                color = AppColors.textPrimary
            )
            Text(
                text = message,
                style = FortunePocketTypography.bodySmall,
                color = AppColors.textSecondary
            )
        }
    }
}

private data class BaziInsightSection(
    val id: String,
    val title: String,
    val body: String
)

@Composable
private fun BaziInterpretationSection(chart: BaziChart, isZh: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isZh) "命盘讲解" else "Chart Guide",
            style = FortunePocketTypography.titleMedium,
            color = AppColors.accentGold
        )
        buildBaziInsightSections(chart = chart, isZh = isZh).forEach { section ->
            BaziInfoPanel(title = section.title, message = section.body)
        }
    }
}

@Composable
private fun CityMetadataPanel(city: BirthCity, isZh: Boolean) {
    BaziInfoPanel(
        title = if (isZh) "${city.nameZh} · ${city.country}" else "${city.nameEn} · ${city.country}",
        message = if (isZh)
            "IANA 时区：${city.timeZoneId}\n法定时区偏移：${utcOffsetLabel(city.utcOffsetHours)}。如该时区历史上存在 DST，系统会按 IANA 时区库自动处理。"
        else
            "IANA zone: ${city.timeZoneId}\nStandard legal offset: ${utcOffsetLabel(city.utcOffsetHours)}. Historical DST, when applicable, is resolved from the IANA time-zone database."
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaziCityPickerSheet(
    cities: List<BirthCity>,
    selectedCity: BirthCity?,
    searchQuery: String,
    isZh: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelect: (BirthCity?) -> Unit,
    onDismiss: () -> Unit
) {
    val normalizedQuery = searchQuery.trim()
    val popularCities = PopularBaziCityIds.mapNotNull { id ->
        cities.firstOrNull { it.id == id }
    }
    val searchResults = cities
        .filter { city ->
            if (normalizedQuery.isEmpty()) {
                false
            } else {
                city.id.contains(normalizedQuery, ignoreCase = true) ||
                    city.nameZh.contains(normalizedQuery, ignoreCase = true) ||
                    city.nameEn.contains(normalizedQuery, ignoreCase = true) ||
                    city.country.contains(normalizedQuery, ignoreCase = true) ||
                    city.timeZoneId.contains(normalizedQuery, ignoreCase = true) ||
                    city.searchAliases.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
        }
        .sortedWith(compareBy<BirthCity> { !PopularBaziCityIds.contains(it.id) }.thenBy {
            if (isZh) it.nameZh else it.nameEn
        })

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item("no-city") {
                    CityChoiceCard(
                        title = if (isZh) "不选城市（按北京时间估算）" else "No city (Beijing legal time)",
                        subtitle = if (isZh)
                            "使用 Asia/Shanghai / UTC+8 作为兜底时区"
                        else
                            "Use Asia/Shanghai / UTC+8 as the fallback time zone",
                        isSelected = selectedCity == null,
                        onClick = { onSelect(null) }
                    )
                }

                if (normalizedQuery.isEmpty()) {
                    if (popularCities.isNotEmpty()) {
                        item("popular-header") {
                            CitySectionHeader(title = if (isZh) "常用城市" else "Popular Cities")
                        }
                        items(popularCities, key = { it.id }) { city ->
                            CityChoiceCard(
                                title = if (isZh) city.nameZh else city.nameEn,
                                subtitle = "${city.timeZoneId} · ${city.country}",
                                isSelected = selectedCity?.id == city.id,
                                onClick = { onSelect(city) }
                            )
                        }
                    }
                    item("search-hint") {
                        BaziInfoPanel(
                            title = if (isZh) "搜索更快" else "Search Is Faster",
                            message = if (isZh)
                                "目前已收录 ${cities.size} 个城市与区域中心，直接搜索城市名、英文名、国家代码或时区会更快。"
                            else
                                "${cities.size} cities and regional hubs are currently included. Searching by city name, English name, country code, or time zone is the fastest path."
                        )
                    }
                } else {
                    item("results-header") {
                        CitySectionHeader(
                            title = (if (isZh) "搜索结果" else "Search Results") + " (${searchResults.size})"
                        )
                    }
                    if (searchResults.isEmpty()) {
                        item("empty-results") {
                            BaziInfoPanel(
                                title = if (isZh) "没有匹配的城市" else "No matching city",
                                message = if (isZh)
                                    "请尝试中文名、英文名、国家代码或时区名，例如“邵阳”或“Seattle”。"
                                else
                                    "Try a Chinese name, English name, country code, or time-zone name, for example “Shaoyang” or “Seattle”."
                            )
                        }
                    } else {
                        items(searchResults, key = { it.id }) { city ->
                            CityChoiceCard(
                                title = if (isZh) city.nameZh else city.nameEn,
                                subtitle = "${city.timeZoneId} · ${city.country}",
                                isSelected = selectedCity?.id == city.id,
                                onClick = { onSelect(city) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CitySectionHeader(title: String) {
    Text(
        text = title,
        style = FortunePocketTypography.titleMedium,
        color = AppColors.accentGold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
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
                Text(
                    text = title,
                    style = FortunePocketTypography.bodyMedium,
                    color = AppColors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = FortunePocketTypography.bodySmall,
                    color = AppColors.textSecondary
                )
            }
            if (isSelected) {
                Text(
                    text = "✓",
                    style = FortunePocketTypography.titleMedium,
                    color = AppColors.accentGold
                )
            }
        }
    }
}

@Composable
private fun BirthdayPickerDialog(
    initialMillis: Long, onDateSelected: (Long) -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(initialMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                val result = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onDateSelected(result)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}

private fun precisionTags(state: BaziUiState, isZh: Boolean): List<String> = buildList {
    if (state.includesBirthHour) {
        add(if (isZh) "分钟级时刻" else "Minute Precision")
        add(
            if (state.selectedCity == null) {
                if (isZh) "北京时间估算" else "Asia/Shanghai Fallback"
            } else {
                "IANA / DST"
            }
        )
        add(
            if (state.useTrueSolarTime && state.selectedCity != null) {
                if (isZh) "真太阳时" else "True Solar Time"
            } else {
                if (isZh) "民用时" else "Civil Time"
            }
        )
        if (state.distinguishLateZi) {
            add(if (isZh) "晚子时规则" else "Late Zi Rule")
        }
    } else {
        add(if (isZh) "日期级估算" else "Date-only Estimate")
        add(if (isZh) "时柱留空" else "Hour Pillar Omitted")
    }
}

private fun precisionTitle(state: BaziUiState, isZh: Boolean): String = when {
    !state.includesBirthHour ->
        if (isZh) "当前按日期级别排盘" else "Currently using a date-only chart"
    state.selectedCity == null ->
        if (isZh) "当前按法定民用时估算" else "Currently estimating with standard legal time"
    state.useTrueSolarTime ->
        if (isZh) "当前按真太阳时排盘" else "Currently using true solar time"
    else ->
        if (isZh) "当前按城市民用时排盘" else "Currently using city civil time"
}

private fun precisionDescription(state: BaziUiState, isZh: Boolean): String = when {
    !state.includesBirthHour ->
        if (isZh)
            "未填写出生时刻时，系统只计算年、月、日三柱，时柱保持未知，更适合做日级别参考。"
        else
            "Without a birth time, the engine calculates only year, month, and day pillars. The hour pillar remains unknown, so treat the result as day-level guidance."
    state.selectedCity == null ->
        if (isZh)
            "未选城市时，系统会以北京时间 `Asia/Shanghai / UTC+8` 作为法定时兜底，不应用 DST，也不能开启真太阳时。国际出生或节气边界案例建议补充城市。"
        else
            "When no city is selected, the engine falls back to Beijing legal time `Asia/Shanghai / UTC+8`. DST is not applied and true solar time stays unavailable. Add a city for international births or boundary cases."
    state.useTrueSolarTime ->
        if (isZh)
            "当前会先按 IANA 时区处理法定时与 DST，再叠加经度修正与均时差，得到更接近地方真太阳时的有效时刻。"
        else
            "The engine first resolves legal time and DST from the IANA time zone, then adds longitude correction and equation of time to derive local apparent solar time."
    else ->
        if (isZh)
            "当前使用所选城市的 IANA 时区与 DST 规则，还原出生地民用时。若你希望按地方太阳时排盘，可开启真太阳时。"
        else
            "The engine is using the selected city's IANA time zone and DST rules to recover local civil time. Enable true solar time if you want a solar-time-based chart."
}

private fun hourLabel(hour: Int, isZh: Boolean): String =
    if (isZh) String.format(Locale.US, "%02d时", hour) else String.format(Locale.US, "%02d h", hour)

private fun minuteLabel(minute: Int, isZh: Boolean): String =
    if (isZh) String.format(Locale.US, "%02d分", minute) else String.format(Locale.US, "%02d min", minute)

private fun utcOffsetLabel(offsetHours: Double): String {
    val sign = if (offsetHours >= 0) "+" else "-"
    val absolute = kotlin.math.abs(offsetHours)
    val whole = absolute.toInt()
    val minutes = (((absolute - whole) * 60.0).toInt())
    return String.format(Locale.US, "UTC%s%02d:%02d", sign, whole, minutes)
}

private val PopularBaziCityIds = listOf(
    "beijing", "shanghai", "guangzhou", "shenzhen", "chengdu",
    "hongkong", "taipei", "singapore", "tokyo",
    "london", "new_york", "los_angeles", "seattle", "sydney"
)

private fun buildBaziInsightSections(chart: BaziChart, isZh: Boolean): List<BaziInsightSection> {
    return listOf(
        buildOverviewSection(chart, isZh),
        buildHourSection(chart, isZh),
        buildElementSection(chart, isZh),
        buildTenGodSection(chart, isZh),
        buildCycleSection(chart, isZh)
    )
}

private fun buildOverviewSection(chart: BaziChart, isZh: Boolean): BaziInsightSection {
    val stemIndex = chart.dayPillar.normalizedStem
    val stemName = if (isZh) chart.dayPillar.stemZh else chart.dayPillar.stemEn
    val elementName = if (isZh) ElementNamesZh[StemElements[stemIndex]] else ElementNamesEn[StemElements[stemIndex]]
    val description = dayMasterDescription(stemIndex = stemIndex, isZh = isZh)
    val monthName = if (isZh) chart.monthPillar.nameZh else chart.monthPillar.nameEn
    val body = if (isZh) {
        "这张盘以 ${stemName}日主为核心，五行属$elementName。$description 月柱是 $monthName，它更像你所处环境与做事节奏的底色，要和日主一起看。"
    } else {
        "This chart is anchored by the $stemName day master, aligned with the $elementName element. $description The month pillar $monthName adds the surrounding rhythm and context that shape how the day master expresses itself."
    }
    return BaziInsightSection(
        id = "overview",
        title = if (isZh) "先看日主主轴" else "Start With the Day Master",
        body = body
    )
}

private fun buildHourSection(chart: BaziChart, isZh: Boolean): BaziInsightSection {
    val body = chart.hourPillar?.let { hourPillar ->
        val pillarName = if (isZh) hourPillar.nameZh else hourPillar.nameEn
        if (isZh) {
            "当前已经纳入时柱 $pillarName。时柱通常更贴近内在动机、私人节奏、晚年主题，以及一些更细的十神差异，所以现在这张盘是完整四柱。"
        } else {
            "The hour pillar $pillarName is included. It often refines inner motivation, private rhythm, later-life themes, and finer ten-god differences, so this chart is using the full four pillars."
        }
    } ?: if (isZh) {
        "你这次没有填写出生时间，所以当前只排到年、月、日三柱。时柱、部分十神和一些更细的判断会留空；如果知道大概时分，建议补上后再看。"
    } else {
        "No birth time was entered, so the engine is only using the year, month, and day pillars. The hour pillar and some finer ten-god detail are intentionally left blank; add an approximate time if you know it."
    }
    return BaziInsightSection(
        id = "hour",
        title = if (chart.hourPillar == null) {
            if (isZh) "当前还是三柱版盘面" else "This Is a Three-Pillar Chart"
        } else {
            if (isZh) "时柱已参与排盘" else "Hour Pillar Included"
        },
        body = body
    )
}

private fun buildElementSection(chart: BaziChart, isZh: Boolean): BaziInsightSection {
    val elements = listOf(
        Triple(0, if (isZh) "木" else "Wood", chart.fiveElements.wood),
        Triple(1, if (isZh) "火" else "Fire", chart.fiveElements.fire),
        Triple(2, if (isZh) "土" else "Earth", chart.fiveElements.earth),
        Triple(3, if (isZh) "金" else "Metal", chart.fiveElements.metal),
        Triple(4, if (isZh) "水" else "Water", chart.fiveElements.water)
    ).sortedWith(compareByDescending<Triple<Int, String, Int>> { it.third }.thenBy { it.first })
    val strongest = elements.first()
    val weakest = elements.last()
    val body = if (isZh) {
        "五行里目前最强的是${strongest.second}，最弱的是${weakest.second}。${elementHint(strongest.first, true, true)} ${elementHint(weakest.first, false, true)} 这里更适合把“弱”理解成需要有意识补足，而不是简单的好坏判断。"
    } else {
        "The strongest element here is ${strongest.second}, while the weakest is ${weakest.second}. ${elementHint(strongest.first, true, false)} ${elementHint(weakest.first, false, false)} Treat the weaker side as something to support consciously, not as a simple flaw."
    }
    return BaziInsightSection(
        id = "elements",
        title = if (isZh) "五行怎么读" else "Reading the Elements",
        body = body
    )
}

private fun buildTenGodSection(chart: BaziChart, isZh: Boolean): BaziInsightSection {
    val visibleGods = listOfNotNull(
        chart.tenGods.yearStemGod,
        chart.tenGods.monthStemGod,
        chart.tenGods.hourStemGod,
        chart.tenGods.yearBranchGod,
        chart.tenGods.monthBranchGod,
        chart.tenGods.dayBranchGod,
        chart.tenGods.hourBranchGod
    )
    val dominant = visibleGods
        .groupBy { if (isZh) it.nameZh else it.nameEn }
        .maxWithOrNull(compareBy<Map.Entry<String, List<TenGodEntry>>>({ it.value.size }, { it.key }))
        ?.value
        ?.firstOrNull()
    val label = if (isZh) dominant?.nameZh ?: "十神" else dominant?.nameEn ?: "Ten Gods"
    val description = dominant?.let { tenGodDescription(it, isZh) } ?: if (isZh) {
        "十神是拿其他干支和日主比较之后得到的关系名词，用来看你更常通过哪种方式处理关系、表达与资源。"
    } else {
        "Ten gods are relationship labels created by comparing other stems and branches to the day master. They help show how you tend to handle people, expression, and resources."
    }
    val body = if (isZh) {
        "当前盘里更突出的十神是 $label。$description 读十神时，不必把它理解成绝对命运，它更像是你常用的一种心理与行为模式。"
    } else {
        "The most visible ten-god pattern right now is $label. $description Rather than treating ten gods as fixed destiny, it is more useful to read them as recurring behavioral and psychological patterns."
    }
    return BaziInsightSection(
        id = "ten-gods",
        title = if (isZh) "十神怎么理解" else "How to Read the Ten Gods",
        body = body
    )
}

private fun buildCycleSection(chart: BaziChart, isZh: Boolean): BaziInsightSection {
    val body = if (isZh) {
        "这张盘是${chart.cycleDirection}，${chart.startingAge}岁起运。大运可以先理解成每十年环境主题的切换，不是单年的吉凶宣判；先看自己正落在哪一步，再回头对照四柱、十神与五行，会更容易读懂。"
    } else {
        "This chart moves in the ${chart.cycleDirection} direction and begins its major cycles at age ${chart.startingAge}. Major cycles are better read as ten-year shifts in environment and emphasis, not as one-line verdicts; first find the current cycle, then read it alongside the pillars, ten gods, and element balance."
    }
    return BaziInsightSection(
        id = "cycles",
        title = if (isZh) "大运怎么看" else "How to Read the Major Cycles",
        body = body
    )
}

private fun dayMasterDescription(stemIndex: Int, isZh: Boolean): String {
    return when (stemIndex) {
        0 -> if (isZh) "甲木常像向上生长的大树，重视方向感、原则和伸展空间。" else "Jia Wood often acts like a tall tree, valuing direction, principles, and room to grow."
        1 -> if (isZh) "乙木更像花草藤蔓，细腻、灵活，也很在意环境中的支持与质感。" else "Yi Wood is more like flowers or vines: flexible, subtle, and highly responsive to its environment."
        2 -> if (isZh) "丙火带着外放和照亮感，通常更容易把热情与存在感带到表面。" else "Bing Fire tends to radiate outward, bringing warmth, visibility, and expressive presence."
        3 -> if (isZh) "丁火像灯烛，敏感而专注，往往更在乎温度、关系和内在的光。" else "Ding Fire is like candlelight: focused, sensitive, and concerned with warmth and inner illumination."
        4 -> if (isZh) "戊土更像厚实的山地，稳定、扛事，也容易先从整体结构来判断问题。" else "Wu Earth resembles solid ground or a mountain: steady, reliable, and structurally minded."
        5 -> if (isZh) "己土像可塑的田土，擅长承接与调整，常常默默把细节照顾起来。" else "Ji Earth is like cultivated soil: adaptive, receptive, and good at quietly tending detail."
        6 -> if (isZh) "庚金更像未经打磨的金属，直接、果断，喜欢清晰边界和执行效率。" else "Geng Metal feels like raw forged metal: direct, decisive, and drawn to clear edges and action."
        7 -> if (isZh) "辛金像珠玉与精工，更重质地、审美和分寸感，也更讲究精确。" else "Xin Metal is closer to refined metal or jewelry, emphasizing precision, refinement, and taste."
        8 -> if (isZh) "壬水像江海大水，流动性强，格局大，也常通过连接与变化来推进事情。" else "Ren Water resembles rivers and seas: expansive, fluid, and inclined toward movement and connection."
        9 -> if (isZh) "癸水像雨露与雾气，感受细，反应快，常在看不见的层面先捕捉变化。" else "Gui Water is like mist or rain: subtle, perceptive, and quick to sense shifts beneath the surface."
        else -> ""
    }
}

private fun elementHint(element: Int, isStrongest: Boolean, isZh: Boolean): String {
    return when (Triple(element, isStrongest, isZh)) {
        Triple(0, true, true) -> "木强时，成长性、方向感和主动延展会更明显。"
        Triple(0, false, true) -> "木弱时，可以多留意边界里的成长空间与持续性。"
        Triple(1, true, true) -> "火强时，表达、热情与被看见的需求通常更突出。"
        Triple(1, false, true) -> "火弱时，节奏感、自我点燃和情绪温度可能更需要经营。"
        Triple(2, true, true) -> "土强时，稳定、现实感与承托能力会成为明显优势。"
        Triple(2, false, true) -> "土弱时，安全感、落地执行和界限感值得多补一点。"
        Triple(3, true, true) -> "金强时，判断、规则感和收束能力通常更突出。"
        Triple(3, false, true) -> "金弱时，决断、筛选与说清楚底线会更需要刻意练习。"
        Triple(4, true, true) -> "水强时，感知、流动性和适应变化的能力会比较亮眼。"
        Triple(4, false, true) -> "水弱时，弹性、休息和情绪流动感往往更需要照顾。"
        Triple(0, true, false) -> "Strong Wood emphasizes growth, direction, and outward development."
        Triple(0, false, false) -> "Weak Wood often asks for more room to grow steadily within your boundaries."
        Triple(1, true, false) -> "Strong Fire highlights expression, warmth, and the need to be seen."
        Triple(1, false, false) -> "Weak Fire often points to tending motivation, rhythm, and emotional warmth more intentionally."
        Triple(2, true, false) -> "Strong Earth supports steadiness, realism, and reliable containment."
        Triple(2, false, false) -> "Weak Earth suggests giving more care to grounding, security, and follow-through."
        Triple(3, true, false) -> "Strong Metal highlights judgment, structure, and the ability to cut clearly."
        Triple(3, false, false) -> "Weak Metal often means boundaries, clarity, and decisiveness need more conscious support."
        Triple(4, true, false) -> "Strong Water highlights sensitivity, adaptability, and movement."
        Triple(4, false, false) -> "Weak Water often asks for more rest, flexibility, and emotional flow."
        else -> ""
    }
}

private fun tenGodDescription(god: TenGodEntry, isZh: Boolean): String {
    return when (god.nameZh) {
        "比肩" -> if (isZh) "它常对应自我主张、并肩竞争和按自己的步调做事。" else "It often points to self-assertion, peer dynamics, and moving at your own pace."
        "劫财" -> if (isZh) "它更像强烈的主动争取，也提醒你留意资源分配和边界。" else "It carries a stronger impulse to contend or claim, and asks for care around resource boundaries."
        "食神" -> if (isZh) "它偏向自然表达、创作输出、享受过程和温和地释放能力。" else "It leans toward natural expression, output, creativity, and releasing talent with ease."
        "伤官" -> if (isZh) "它常带来锋利表达、反思权威和不想被框住的冲劲。" else "It often brings sharper expression, challenge to authority, and a dislike of constraint."
        "偏财" -> if (isZh) "它更像对机会、资源调度和外部回报的敏感度。" else "It reflects sensitivity to opportunity, flexible resources, and external gain."
        "正财" -> if (isZh) "它强调稳定经营、现实责任和一步一步把事情落地。" else "It emphasizes steady management, practical responsibility, and grounded results."
        "七杀" -> if (isZh) "它会让人对压力、挑战和迅速应战更有反应。" else "It can show strong responsiveness to pressure, challenge, and decisive action."
        "正官" -> if (isZh) "它通常和秩序、规范、责任感以及社会角色感有关。" else "It is often linked to order, rules, responsibility, and social role."
        "偏印" -> if (isZh) "它偏向直觉、防御、本能理解和跳脱常规的思路。" else "It leans toward intuition, protective instinct, and unconventional understanding."
        "正印" -> if (isZh) "它常和学习、支持、修复感与被滋养的经验有关。" else "It is often tied to learning, support, repair, and the feeling of being resourced."
        else -> if (isZh) "它是相对于日主的一种关系名称。" else "It is one relationship label relative to the day master."
    }
}

private fun baziSharePayload(
    chart: BaziChart,
    insightSections: List<BaziInsightSection>,
    isZh: Boolean
): OracleShareCardPayload {
    val title = if (isZh) {
        "${chart.dayPillar.stemZh}日主 · ${chart.dayPillar.nameZh}"
    } else {
        "${chart.dayPillar.stemEn} Day Master · ${chart.dayPillar.nameEn}"
    }
    val date = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(
        Calendar.getInstance().apply {
            set(chart.input.birthYear, chart.input.birthMonth - 1, chart.input.birthDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    )
    return OracleShareCardPayload(
        eyebrow = if (isZh) "FORTUNE POCKET · 八字" else "FORTUNE POCKET · BAZI",
        title = title,
        subtitle = if (chart.hourPillar == null) {
            if (isZh) "$date · 三柱估算" else "$date · Three-pillar estimate"
        } else {
            if (isZh) "$date · 四柱完整盘" else "$date · Full four-pillar chart"
        },
        headline = insightSections.firstOrNull()?.title ?: title,
        summary = insightSections.firstOrNull()?.body.orEmpty(),
        guidance = insightSections.getOrNull(2)?.body ?: insightSections.lastOrNull()?.body.orEmpty(),
        footer = if (isZh) "大运起于 ${chart.startingAge} 岁 · ${chart.cycleDirection}" else "Major cycles start at age ${chart.startingAge} · ${chart.cycleDirection}"
    )
}

private fun buildBaziShareText(
    chart: BaziChart,
    insightSections: List<BaziInsightSection>,
    isZh: Boolean
): String {
    val title = if (isZh) "Fortune Pocket · 八字排盘" else "Fortune Pocket · Four Pillars Chart"
    val date = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(
        Calendar.getInstance().apply {
            set(chart.input.birthYear, chart.input.birthMonth - 1, chart.input.birthDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    )
    return listOf(
        title,
        date,
        if (isZh) "四柱：${chart.yearPillar.nameZh} / ${chart.monthPillar.nameZh} / ${chart.dayPillar.nameZh} / ${chart.hourPillar?.nameZh ?: "—"}"
        else "Pillars: ${chart.yearPillar.nameEn} / ${chart.monthPillar.nameEn} / ${chart.dayPillar.nameEn} / ${chart.hourPillar?.nameEn ?: "—"}",
        "",
        insightSections.joinToString("\n\n") { "${it.title}\n${it.body}" }
    ).joinToString("\n")
}
