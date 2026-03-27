package com.example.deadmanswitch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.deadmanswitch.data.ActivityEntry
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager
import com.example.deadmanswitch.service.MonitorService
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeadManSwitchTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    val activityLog = remember { ActivityLogManager(context) }

    var isMonitoring by remember { mutableStateOf(MonitorService.isRunning(context)) }
    var thresholdHours by remember { mutableFloatStateOf(settings.thresholdHours) }
    var lastActivityTime by remember { mutableLongStateOf(settings.lastActivityTime) }
    var showStopDialog by remember { mutableStateOf(false) }
    var logEntries by remember { mutableStateOf(activityLog.getAll().take(20)) }

    // 实时更新倒计时
    var remainingMs by remember { mutableLongStateOf(settings.remainingMs) }
    var remainingPercent by remember { mutableFloatStateOf(settings.remainingPercent) }

    LaunchedEffect(isMonitoring) {
        while (isMonitoring) {
            remainingMs = settings.remainingMs
            remainingPercent = settings.remainingPercent
            delay(1000)
        }
    }

    // 刷新活动日志
    fun refreshLog() {
        logEntries = activityLog.getAll().take(20)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeadManSwitch") },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 状态卡片 - 大号倒计时 + 进度条
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            !isMonitoring -> MaterialTheme.colorScheme.errorContainer
                            remainingPercent > 0.8f -> MaterialTheme.colorScheme.errorContainer
                            remainingPercent > 0.5f -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isMonitoring) "🛡️ 监控中" else "🔴 已停止",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isMonitoring) {
                            val h = remainingMs / (1000 * 60 * 60)
                            val m = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
                            val s = (remainingMs % (1000 * 60)) / 1000

                            Text(
                                text = String.format("%02d:%02d:%02d", h, m, s),
                                style = MaterialTheme.typography.displayMedium,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "距离警报触发",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { remainingPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "已用 ${(remainingPercent * 100).toInt()}% | 阈值 ${thresholdHours.toInt()}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "点击下方按钮开始监控",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 阈值设置
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("无活动阈值", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${thresholdHours.toInt()} 小时",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "超过此时间无活动将触发警报",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = thresholdHours,
                            onValueChange = {
                                thresholdHours = it
                                settings.thresholdHours = it
                            },
                            valueRange = 1f..48f,
                            steps = 47,
                            enabled = !isMonitoring
                        )
                    }
                }
            }

            // 控制按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isMonitoring) {
                        Button(
                            onClick = { showStopDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("停止监控")
                        }
                    } else {
                        Button(
                            onClick = {
                                context.startForegroundService(
                                    Intent(context, MonitorService::class.java)
                                )
                                isMonitoring = true
                                remainingMs = settings.remainingMs
                                remainingPercent = settings.remainingPercent
                                refreshLog()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("开始监控")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            settings.resetActivity()
                            lastActivityTime = settings.lastActivityTime
                            remainingMs = settings.remainingMs
                            remainingPercent = settings.remainingPercent
                            activityLog.addEntry("manual_reset")
                            refreshLog()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("手动重置")
                    }
                }
            }

            // 活动历史
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("活动历史", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { refreshLog() }) {
                        Text("刷新")
                    }
                }
            }

            if (logEntries.isEmpty()) {
                item {
                    Text(
                        "暂无记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(logEntries) { entry ->
                ActivityLogItem(entry)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // 停止确认弹窗
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("确认停止？") },
            text = { Text("停止监控后将不再检测您的活动状态，警报也不会触发。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.stopService(Intent(context, MonitorService::class.java))
                        isMonitoring = false
                        showStopDialog = false
                        refreshLog()
                    }
                ) { Text("确认停止", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun ActivityLogItem(entry: ActivityEntry) {
    val typeLabel = when (entry.type) {
        "unlock" -> "🔓 解锁屏幕"
        "boot" -> "🔄 开机"
        "manual_reset" -> "👆 手动重置"
        "alert" -> "⚠️ 触发警报"
        "monitor_start" -> "▶️ 开始监控"
        "monitor_stop" -> "⏹ 停止监控"
        else -> entry.type
    }

    val sdf = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    val timeStr = sdf.format(Date(entry.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(typeLabel, style = MaterialTheme.typography.bodyMedium)
        Text(
            timeStr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
