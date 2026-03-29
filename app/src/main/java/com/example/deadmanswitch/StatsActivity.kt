package com.example.deadmanswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadmanswitch.data.DailyCount
import com.example.deadmanswitch.data.EventRepository
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = com.example.deadmanswitch.data.SettingsManager(this)
        setContent {
            val darkMode by remember { mutableIntStateOf(settings.darkMode) }
            DeadManSwitchTheme(darkMode = darkMode) {
                StatsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { EventRepository(context) }

    var dailyStats by remember { mutableStateOf<List<DailyCount>>(emptyList()) }
    var totalUnlocks by remember { mutableIntStateOf(0) }
    var totalLocks by remember { mutableIntStateOf(0) }
    var totalAlerts by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        dailyStats = repo.getDailyStats(7)
        totalUnlocks = repo.getTotalCount("unlock")
        totalLocks = repo.getTotalCount("lock")
        totalAlerts = repo.getTotalCount("alert")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("使用统计") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 累计统计卡片
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📊 累计统计", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TotalStatCard("🔓", "解锁", totalUnlocks, Color(0xFF4CAF50))
                            TotalStatCard("🔒", "锁屏", totalLocks, Color(0xFF2196F3))
                            TotalStatCard("🔔", "警报", totalAlerts, Color(0xFFFF5722))
                        }
                    }
                }
            }

            // 最近 7 天柱状图
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📈 最近 7 天", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Color(0xFF4CAF50)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("解锁", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Color(0xFF2196F3)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("锁屏", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (dailyStats.isNotEmpty()) {
                            WeeklyBarChart(dailyStats)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 每日详情
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📋 每日详情", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (dailyStats.isEmpty()) {
                            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            dailyStats.reversed().forEach { day ->
                                DailyDetailRow(day)
                                if (day != dailyStats.first()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun TotalStatCard(emoji: String, label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun WeeklyBarChart(stats: List<DailyCount>) {
    val maxUnlock = (stats.maxOfOrNull { it.unlockCount } ?: 1).coerceAtLeast(1)
    val maxLock = (stats.maxOfOrNull { it.lockCount } ?: 1).coerceAtLeast(1)
    val maxValue = maxOf(maxUnlock, maxLock)

    val dayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val weekFormat = SimpleDateFormat("E", Locale.CHINESE)

    val unlockColor = Color(0xFF4CAF50)
    val lockColor = Color(0xFF2196F3)
    val gridColor = Color.LightGray.copy(alpha = 0.3f)
    val textMeasurer = rememberTextMeasurer()

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / (stats.size * 3f)
            val gap = barWidth * 0.5f
            val chartHeight = canvasHeight - 40.dp.toPx()

            // 画网格线
            for (i in 0..4) {
                val y = chartHeight - (chartHeight * i / 4)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
            }

            // 画柱状图
            stats.forEachIndexed { index, day ->
                val x = (index * 3 + 0.5f) * (barWidth + gap)

                // 解锁柱
                val unlockHeight = (day.unlockCount.toFloat() / maxValue) * chartHeight
                drawRoundRect(
                    color = unlockColor,
                    topLeft = Offset(x, chartHeight - unlockHeight),
                    size = Size(barWidth, unlockHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // 锁屏柱
                val lockHeight = (day.lockCount.toFloat() / maxValue) * chartHeight
                drawRoundRect(
                    color = lockColor,
                    topLeft = Offset(x + barWidth + gap, chartHeight - lockHeight),
                    size = Size(barWidth, lockHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // 日期标签
                val dateLabel = try {
                    val cal = Calendar.getInstance()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    cal.time = sdf.parse(day.date) ?: Date()
                    weekFormat.format(cal.time)
                } catch (e: Exception) {
                    day.date.takeLast(2)
                }

                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(dateLabel),
                    style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x + barWidth - textLayoutResult.size.width / 4,
                        chartHeight + 8.dp.toPx()
                    )
                )
            }
        }

        // 数值标注
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stats.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "${day.unlockCount}/${day.lockCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DailyDetailRow(day: DailyCount) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 日期
        val displayDate = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displaySdf = SimpleDateFormat("M月d日 E", Locale.CHINESE)
            val date = sdf.parse(day.date)
            if (date != null) displaySdf.format(date) else day.date
        } catch (e: Exception) {
            day.date
        }

        Text(displayDate, style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🔓 ${day.unlockCount}", style = MaterialTheme.typography.bodySmall)
            Text("🔒 ${day.lockCount}", style = MaterialTheme.typography.bodySmall)
            if (day.alertCount > 0) {
                Text("🔔 ${day.alertCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
