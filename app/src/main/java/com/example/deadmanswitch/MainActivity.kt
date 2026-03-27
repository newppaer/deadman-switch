package com.example.deadmanswitch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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
    var showStopDialog by remember { mutableStateOf(false) }
    var logEntries by remember { mutableStateOf(activityLog.getAll().take(20)) }

    var remainingMs by remember { mutableLongStateOf(settings.remainingMs) }
    var remainingPercent by remember { mutableFloatStateOf(settings.remainingPercent) }

    // 通知权限请求（Android 13+）
    val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
            isMonitoring = true
            activityLog.addEntry("monitor_start")
            refreshLog(activityLog) { logEntries = it }
            Toast.makeText(context, "监控已启动", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "需要通知权限才能正常工作", Toast.LENGTH_LONG).show()
        }
    }

    // 检查崩溃日志
    var crashLog by remember { mutableStateOf(CrashLogger.readLog(context)) }
    var showCrashLog by remember { mutableStateOf(CrashLogger.hasLog(context)) }

    LaunchedEffect(isMonitoring) {
        while (isMonitoring) {
            remainingMs = settings.remainingMs
            remainingPercent = settings.remainingPercent
            delay(1000)
        }
    }

    fun startMonitoring() {
        if (needsNotificationPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startForegroundService(Intent(context, MonitorService::class.java))
            isMonitoring = true
            activityLog.addEntry("monitor_start")
            refreshLog(activityLog) { logEntries = it }
        }
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

            item {
                StatusCard(
                    isMonitoring = isMonitoring,
                    remainingMs = remainingMs,
                    remainingPercent = remainingPercent,
                    thresholdHours = thresholdHours
                )
            }

            item {
                ThresholdCard(
                    thresholdHours = thresholdHours,
                    isMonitoring = isMonitoring,
                    onThresholdChange = {
                        thresholdHours = it
                        settings.thresholdHours = it
                    }
                )
            }

            item {
                ControlButtons(
                    isMonitoring = isMonitoring,
                    onStartClick = { startMonitoring() },
                    onStopClick = { showStopDialog = true },
                    onResetClick = {
                        settings.resetActivity()
                        remainingMs = settings.remainingMs
                        remainingPercent = settings.remainingPercent
                        activityLog.addEntry("manual_reset")
                        refreshLog(activityLog) { logEntries = it }
                    }
                )
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
                    TextButton(onClick = { refreshLog(activityLog) { logEntries = it } }) {
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
                        activityLog.addEntry("monitor_stop")
                        refreshLog(activityLog) { logEntries = it }
                    }
                ) { Text("确认停止", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("取消") }
            }
        )
    // 崩溃日志弹窗
    if (showCrashLog && crashLog.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showCrashLog = false
                CrashLogger.clearLog(context)
                crashLog = ""
            },
            title = { Text("⚠️ 上次崩溃报告") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        Text(
                            crashLog,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCrashLog = false
                    CrashLogger.clearLog(context)
                    crashLog = ""
                }) { Text("关闭并清除") }
            },
            dismissButton = {
                TextButton(onClick = { showCrashLog = false }) { Text("仅关闭") }
            }
        )
    }
}

@Composable
fun StatusCard(
    isMonitoring: Boolean,
    remainingMs: Long,
    remainingPercent: Float,
    thresholdHours: Float
) {
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
                Text("距离警报触发", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { remainingPercent },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已用 ${(remainingPercent * 100).toInt()}% | 阈值 ${thresholdHours.toInt()}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("点击下方按钮开始监控", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ThresholdCard(
    thresholdHours: Float,
    isMonitoring: Boolean,
    onThresholdChange: (Float) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                onValueChange = onThresholdChange,
                valueRange = 1f..48f,
                steps = 47,
                enabled = !isMonitoring
            )
        }
    }
}

@Composable
fun ControlButtons(
    isMonitoring: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isMonitoring) {
            Button(
                onClick = onStopClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("停止监控") }
        } else {
            Button(onClick = onStartClick, modifier = Modifier.weight(1f)) { Text("开始监控") }
        }
        OutlinedButton(onClick = onResetClick, modifier = Modifier.weight(1f)) { Text("手动重置") }
    }
}

@Composable
fun ActivityLogItem(entry: ActivityEntry) {
    val typeLabel = when (entry.type) {
        "unlock" -> "🔓 解锁屏幕"
        "lock" -> "🔒 锁定屏幕"
        "boot" -> "🔄 开机启动"
        "manual_reset" -> "👆 手动重置"
        "alert" -> "⚠️ 触发警报"
        "monitor_start" -> "▶️ 开始监控"
        "monitor_stop" -> "⏹ 停止监控"
        "service_restart" -> "🔄 服务自动重启"
        else -> "📌 ${entry.type}"
    }

    val sdf = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    val timeStr = sdf.format(Date(entry.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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

private fun refreshLog(log: ActivityLogManager, onUpdate: (List<ActivityEntry>) -> Unit = {}) {
    onUpdate(log.getAll().take(20))
}
