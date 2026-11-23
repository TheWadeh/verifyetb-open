package com.verifyetb.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verifyetb.app.ui.components.EmptyHint
import com.verifyetb.app.ui.components.SectionLabel
import com.verifyetb.app.ui.theme.AccentBlue
import com.verifyetb.app.ui.theme.AccentGreen
import com.verifyetb.app.ui.theme.AccentRust
import com.verifyetb.app.ui.theme.AmountStyle
import com.verifyetb.app.ui.theme.BgElevated
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary
import com.verifyetb.app.util.DateFormatters
import com.verifyetb.app.util.MoneyFormat
import kotlin.math.abs

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    onOpenDay: (from: Long, to: Long) -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Insights", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Net flow, spending, and the last 12 weeks",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodChip("Week", ui.period == InsightsPeriod.WEEK) { viewModel.setPeriod(InsightsPeriod.WEEK) }
            PeriodChip("Month", ui.period == InsightsPeriod.MONTH) { viewModel.setPeriod(InsightsPeriod.MONTH) }
            PeriodChip("Year", ui.period == InsightsPeriod.YEAR) { viewModel.setPeriod(InsightsPeriod.YEAR) }
        }

        Spacer(modifier = Modifier.height(16.dp))
        NetHero(net = ui.net, count = ui.count)

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStat("In", MoneyFormat.birr(ui.incoming), AccentGreen, Modifier.weight(1f))
            MiniStat("Out", MoneyFormat.birr(ui.outgoing), AccentRust, Modifier.weight(1f))
            MiniStat("Fees", MoneyFormat.birr(ui.fees), AccentBlue, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionLabel(if (ui.period == InsightsPeriod.YEAR) "Monthly net" else "Daily net")
        FlowChart(bars = ui.bars)

        Spacer(modifier = Modifier.height(22.dp))
        SectionLabel("Spending")
        if (ui.categories.isEmpty()) {
            EmptyHint("No money out in this period.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.categories.forEach { slice ->
                    CategoryRow(slice)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionLabel("Activity")
        Text(
            "Tap a day to open those transactions.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Heatmap(weeks = ui.weeks, maxHeat = ui.maxHeat, onOpenDay = onOpenDay)
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun NetHero(net: Double, count: Int) {
    val tone = if (net >= 0) AccentGreen else AccentRust
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(BgSurface, BgElevated, AccentBlue.copy(alpha = 0.14f))),
            )
            .border(1.dp, AccentBlue.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Text("Net this period", color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Text(
            text = "ETB ${MoneyFormat.signed(abs(net), isDebit = net < 0)}",
            color = tone,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = if (count == 0) "Nothing recorded yet" else "$count transactions",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BgSurface)
            .border(1.dp, Divider, RoundedCornerShape(20.dp))
            .padding(12.dp),
    ) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            color = color,
            style = AmountStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun FlowChart(bars: List<FlowBar>) {
    if (bars.isEmpty() || bars.all { it.net == 0.0 }) {
        EmptyHint("No movement in this period.")
        return
    }
    val max = bars.maxOf { abs(it.net) }.coerceAtLeast(1.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .height(148.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            val frac = (abs(bar.net) / max).toFloat().coerceIn(if (bar.net == 0.0) 0f else 0.08f, 1f)
            val colWidth = if (bars.size > 16) 18.dp else 28.dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(colWidth).fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (bar.net != 0.0) {
                        Box(
                            modifier = Modifier
                                .width(if (bars.size > 16) 10.dp else 14.dp)
                                .fillMaxHeight(frac)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (bar.net >= 0) AccentGreen else AccentRust),
                        )
                    }
                }
                Text(
                    bar.label,
                    color = TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(slice.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(MoneyFormat.birr(slice.amount), color = TextMuted, style = AmountStyle)
        }
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(BgElevated),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(slice.fraction)
                    .background(AccentBlue),
            )
        }
    }
}

@Composable
private fun Heatmap(
    weeks: List<List<HeatCell>>,
    maxHeat: Int,
    onOpenDay: (Long, Long) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell ->
                    val alpha = when {
                        cell.inFuture || cell.count == 0 || maxHeat == 0 -> 0.18f
                        else -> 0.28f + 0.72f * (cell.count.toFloat() / maxHeat.toFloat())
                    }
                    val color = if (cell.count == 0 || cell.inFuture) Divider else AccentBlue.copy(alpha = alpha)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .clickable(
                                enabled = !cell.inFuture && cell.count > 0,
                                role = Role.Button,
                                onClick = {
                                    val (from, to) = DateFormatters.dayRange(cell.start)
                                    onOpenDay(from, to)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) AccentBlue else TextMuted,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) AccentBlue.copy(alpha = 0.16f) else BgSurface)
            .border(1.dp, if (selected) AccentBlue else Divider, RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
