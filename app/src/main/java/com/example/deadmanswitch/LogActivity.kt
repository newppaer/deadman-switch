package com.example.deadmanswitch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme
import java.text.SimpleDateFormat
import java.util.*

class LogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeadManSwitchTheme {
                LogScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activityLog = remember { ActivityLogManager(context) }
    var entries by remember { mutableStateOf(activityLog.getAll()) }
    var showClearDialog by remember { mutableStateOf(false) }

    // 统计
    val unlockCount = entries.count { it.type == "unlock" }
    val lockCount = entries.count { it.type == "lock" }
    val alertCount = entries.count { it.type == "alert" }
    val totalCount = entries.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("活动历史") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { entries = activityLog.getAll() }) {
                        Text("🔄", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "清除", tint = MaterialTheme.colorScheme.error)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 统计卡片
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📊 统计", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatChip("🔓 解锁", unlockCount)
                            StatChip("🔒 锁屏", lockCount)
                            StatChip("⚠️ 警报", alertCount)
                        }
                        Text(
                            "共 $totalCount 条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 导出按钮
            item {
                OutlinedButton(
                    onClick = {
                        val text = entries.joinToString("\n") { entry ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val type = when (entry.type) {
                                "unlock" -> "解锁"
                                "lock" -> "锁屏"
                                "boot" -> "开机"
                                "manual_reset" -> "手动重置"
                                "alert" -> "触发警报"
                                "monitor_start" -> "开始监控"
                                "monitor_stop" -> "停止监控"
                                "service_restart" -> "服务重启"
                                else -> entry.type
                            }
                            "${sdf.format(Date(entry.timestamp))}  $type"
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("activity_log", text))
                        Toast.makeText(context, "已复制 ${entries.size} 条记录", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📋 复制全部记录")
                }
            }

            // 历史列表
            item {
                Text("全部记录", style = MaterialTheme.typography.titleMedium)
            }

            if (entries.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(entries) { entry ->
                val typeLabel = when (entry.type) {
                    "unlock" -> "🔓 解锁屏幕"
                    "lock" -> "🔒 锁定屏幕"
                    "boot" -> "🔄 开机启动"
                    "manual_reset" -> "👆 手动重置"
                    "alert" -> "⚠️ 触发警报"
                    "monitor_start" -> "▶️ 开始监控"
                    "monitor_stop" -> "⏹ 停止监控"
                    "service_restart" -> "🔄 服务自动重启"
                    else -> if (entry.type.startsWith("error")) "❌ ${entry.type}" else "📌 ${entry.type}"
                }
                val sdf = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
                val timeStr = sdf.format(Date(entry.timestamp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            entry.type == "alert" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            entry.type.startsWith("error") -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
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
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // 清除确认
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清除？") },
            text = { Text("将清除全部 ${entries.size} 条活动记录，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    activityLog.clear()
                    entries = emptyList()
                    showClearDialog = false
                    Toast.makeText(context, "已清除全部记录", Toast.LENGTH_SHORT).show()
                }) { Text("确认清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun StatChip(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$count",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
