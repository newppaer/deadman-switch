package com.example.deadmanswitch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.deadmanswitch.service.MonitorService
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme

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
    val prefs = remember { context.getSharedPreferences("deadman_prefs", 0) }
    
    var thresholdHours by remember { 
        mutableFloatStateOf(prefs.getFloat("threshold_hours", 12f)) 
    }
    var isMonitoring by remember { mutableStateOf(false) }
    var lastActivity by remember { mutableStateOf("未记录") }

    LaunchedEffect(Unit) {
        // 检查服务是否运行
        isMonitoring = MonitorService.isRunning
        lastActivity = prefs.getLong("last_activity", 0L).let {
            if (it > 0) formatTime(it) else "未记录"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("安全监控") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMonitoring) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isMonitoring) "🟢 监控中" else "🔴 已停止",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("上次活动: $lastActivity")
                }
            }

            // 阈值设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "无活动阈值",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "超过 ${thresholdHours.toInt()} 小时无活动将触发警报",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = thresholdHours,
                        onValueChange = { 
                            thresholdHours = it
                            prefs.edit().putFloat("threshold_hours", it).apply()
                        },
                        valueRange = 1f..48f,
                        steps = 47
                    )
                }
            }

            // 控制按钮
            Button(
                onClick = {
                    if (isMonitoring) {
                        context.stopService(Intent(context, MonitorService::class.java))
                    } else {
                        context.startForegroundService(Intent(context, MonitorService::class.java))
                    }
                    isMonitoring = !isMonitoring
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isMonitoring) "停止监控" else "开始监控")
            }

            // 手动重置
            OutlinedButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    prefs.edit().putLong("last_activity", now).apply()
                    lastActivity = formatTime(now)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("手动重置计时器")
            }

            // 说明
            Text(
                "提示: 解锁屏幕或打开任意 App 会自动重置计时器",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}