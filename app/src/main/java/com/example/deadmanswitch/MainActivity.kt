package com.example.deadmanswitch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager
import com.example.deadmanswitch.service.MonitorService
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings = remember { SettingsManager(this) }
            val darkMode by remember { mutableIntStateOf(settings.darkMode) }
            DeadManSwitchTheme(darkMode = darkMode) {
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
    var remainingMs by remember { mutableLongStateOf(settings.remainingMs) }
    var remainingPercent by remember { mutableFloatStateOf(settings.remainingPercent) }

    // 解锁/锁屏统计
    var unlockCount by remember { mutableIntStateOf(0) }
    var lockCount by remember { mutableIntStateOf(0) }

    fun refreshCounts() {
        val all = activityLog.getAll()
        unlockCount = all.count { it.type == "unlock" }
        lockCount = all.count { it.type == "lock" }
    }

    LaunchedEffect(Unit) { refreshCounts() }

    // 页面回到前台时刷新数据（从设置页/历史页返回）
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            refreshCounts()
        }
    }

    // 崩溃日志
    var crashLog by remember { mutableStateOf(CrashLogger.readLog(context)) }
    var showCrashLog by remember { mutableStateOf(CrashLogger.hasLog(context)) }

    // 服务错误
    var serviceError by remember { mutableStateOf<String?>(null) }

    // 权限弹窗
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingPermission by remember { mutableStateOf("") }

    // 通知权限
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
            isMonitoring = true
            activityLog.addEntry("monitor_start")
            refreshCounts()
            Toast.makeText(context, "监控已启动", Toast.LENGTH_SHORT).show()
            Handler(context.mainLooper).postDelayed({
                val err = MonitorService.consumeLastError()
                if (err != null) {
                    serviceError = err
                    isMonitoring = false
                    refreshCounts()
                }
            }, 1500)
        } else {
            showPermissionDialog = true
            pendingPermission = "通知"
        }
    }

    LaunchedEffect(isMonitoring) {
        while (isMonitoring) {
            remainingMs = settings.remainingMs
            remainingPercent = settings.remainingPercent
            delay(1000)
        }
    }

    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startForegroundService(Intent(context, MonitorService::class.java))
            isMonitoring = true
            activityLog.addEntry("monitor_start")
            refreshCounts()
            Handler(context.mainLooper).postDelayed({
                val err = MonitorService.consumeLastError()
                if (err != null) {
                    serviceError = err
                    isMonitoring = false
                    refreshCounts()
                }
            }, 1500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeadManSwitch") },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(context, LogActivity::class.java)) }) {
                        Icon(Icons.Default.List, contentDescription = "活动历史")
                    }
                    IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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

            // 状态卡片
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
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
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
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
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

            // 解锁/锁屏统计
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📱 手机使用统计", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatChip("🔓 解锁", unlockCount)
                            StatChip("🔒 锁屏", lockCount)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 温馨提示
                        val tip = getMilestoneTip(unlockCount, lockCount)
                        if (tip != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Text(
                                    tip,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // 阈值
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            onValueChange = { thresholdHours = it; settings.thresholdHours = it },
                            valueRange = 1f..48f,
                            steps = 47,
                            enabled = !isMonitoring
                        )
                    }
                }
            }

            // 按钮
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isMonitoring) {
                        Button(
                            onClick = { showStopDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("停止监控") }
                    } else {
                        Button(onClick = { startMonitoring() }, modifier = Modifier.weight(1f)) { Text("开始监控") }
                    }
                    OutlinedButton(
                        onClick = {
                            settings.resetActivity()
                            remainingMs = settings.remainingMs
                            remainingPercent = settings.remainingPercent
                            activityLog.addEntry("manual_reset")
                            refreshCounts()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("手动重置") }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // 停止确认
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("确认停止？") },
            text = { Text("停止监控后将不再检测您的活动状态。") },
            confirmButton = {
                TextButton(onClick = {
                    context.stopService(Intent(context, MonitorService::class.java))
                    isMonitoring = false
                    showStopDialog = false
                    activityLog.addEntry("monitor_stop")
                    refreshCounts()
                }) { Text("确认停止", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("取消") } }
        )
    }

    // 崩溃日志
    if (showCrashLog && crashLog.isNotEmpty()) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showCrashLog = false },
            title = { Text("⚠️ 上次崩溃报告") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(crashLog))
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }) { Text("📋 复制全部") }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item {
                            SelectionContainer {
                                Text(
                                    crashLog,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCrashLog = false; CrashLogger.clearLog(context); crashLog = ""
                }) { Text("关闭并清除") }
            },
            dismissButton = { TextButton(onClick = { showCrashLog = false }) { Text("仅关闭") } }
        )
    }

    // 服务启动错误弹窗
    if (serviceError != null) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { serviceError = null },
            title = { Text("❌ 监控服务启动失败") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(serviceError!!))
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }) { Text("📋 复制") }
                    }
                    SelectionContainer {
                        Text(
                            serviceError!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { serviceError = null }) { Text("关闭") } }
        )
    }

    // 权限被拒绝 → 引导去设置页
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要${pendingPermission}权限") },
            text = { Text("请在系统设置中手动开启${pendingPermission}权限，否则此功能无法正常使用。") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("取消") }
            }
        )
    }
}

/**
 * 温馨调皮提示语
 */
fun getMilestoneTip(unlockCount: Int, lockCount: Int): String? {
    return when {
        unlockCount == 0 -> null
        unlockCount < 10 -> "👀 今天还挺克制的嘛，才解锁了 ${unlockCount} 次"
        unlockCount < 30 -> "📱 解锁了 ${unlockCount} 次，正常操作"
        unlockCount < 50 -> "🤭 已经解锁 ${unlockCount} 次了，是不是在等什么消息？"
        unlockCount < 100 -> "😵 ${unlockCount} 次了！你确定不是在用手机做俯卧撑？"
        unlockCount < 200 -> "🤯 破百了！解锁 ${unlockCount} 次，手机屏幕都要被你磨秃了"
        unlockCount < 500 -> "💀 ${unlockCount} 次…你和手机的感情比我和代码还深"
        unlockCount < 1000 -> "🏆 恭喜解锁 ${unlockCount} 次！你应该是手机的 VIP 用户"
        else -> "👑 解锁 ${unlockCount} 次！建议手机给你发个年终奖 🎉"
    }
}
