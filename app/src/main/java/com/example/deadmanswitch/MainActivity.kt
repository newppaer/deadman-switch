package com.example.deadmanswitch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadmanswitch.service.MonitorService
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme
import com.example.deadmanswitch.utils.ActivityDetector
import com.example.deadmanswitch.utils.Logging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logging.logActivityLifecycle("MainActivity", "onCreate")
        
        // 检查是否有崩溃报告
        checkCrashReport()
        
        setContent {
            DeadManSwitchTheme {
                MainScreen()
            }
        }
    }
    
    private fun checkCrashReport() {
        try {
            val crashReport = Logging.getLastCrashReport(this)
            if (crashReport != null) {
                Logging.w("MainActivity", "Previous crash detected: ${crashReport.take(100)}...")
                // 这里可以显示一个提示给用户
                // 暂时只记录到日志，未来可以添加UI提示
            }
        } catch (e: Exception) {
            Logging.e("MainActivity", "Failed to check crash report", e)
        }
    }
    
    override fun onStart() {
        super.onStart()
        Logging.logActivityLifecycle("MainActivity", "onStart")
    }
    
    override fun onResume() {
        super.onResume()
        Logging.logActivityLifecycle("MainActivity", "onResume")
    }
    
    override fun onPause() {
        super.onPause()
        Logging.logActivityLifecycle("MainActivity", "onPause")
    }
    
    override fun onStop() {
        super.onStop()
        Logging.logActivityLifecycle("MainActivity", "onStop")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Logging.logActivityLifecycle("MainActivity", "onDestroy")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("deadman_prefs", 0) }
    val activityDetector = remember { ActivityDetector(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var thresholdHours by remember { 
        mutableFloatStateOf(prefs.getFloat("threshold_hours", 12f)) 
    }
    var isMonitoring by remember { mutableStateOf(false) }
    var inactivityDuration by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // 自动刷新无活动时长
    LaunchedEffect(Unit) {
        while (true) {
            inactivityDuration = activityDetector.formatInactivityDuration()
            delay(10000) // 每10秒更新一次
        }
    }
    
    // 检查服务状态
    LaunchedEffect(Unit) {
        isMonitoring = MonitorService.isRunning
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = { 
                            // 打开设置
                            selectedTab = 2
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "主页") },
                    label = { Text("状态") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Contacts, contentDescription = "联系人") },
                    label = { Text(stringResource(R.string.emergency_contacts)) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> StatusTab(
                    isMonitoring = isMonitoring,
                    inactivityDuration = inactivityDuration,
                    thresholdHours = thresholdHours,
                    onThresholdChange = { 
                        thresholdHours = it
                        prefs.edit().putFloat("threshold_hours", it).apply()
                    },
                    onToggleMonitoring = {
                        try {
                            if (isMonitoring) {
                                Logging.logMonitoringEvent("Stopping monitoring service")
                                context.stopService(Intent(context, MonitorService::class.java))
                                Logging.d("MainActivity", "Service stop requested")
                            } else {
                                Logging.logMonitoringEvent("Starting monitoring service")
                                context.startForegroundService(Intent(context, MonitorService::class.java))
                                Logging.d("MainActivity", "Foreground service start requested")
                            }
                            isMonitoring = !isMonitoring
                        } catch (e: Exception) {
                            Logging.e("MainActivity", "Failed to toggle monitoring service", e)
                        }
                    },
                    onResetTimer = {
                        activityDetector.updateLastActivity()
                        inactivityDuration = activityDetector.formatInactivityDuration()
                    }
                )
                1 -> ContactsTab()
                2 -> SettingsTab(
                    autoStart = prefs.getBoolean("auto_start", true),
                    onAutoStartChange = {
                        prefs.edit().putBoolean("auto_start", it).apply()
                    },
                    darkMode = prefs.getBoolean("dark_mode", false),
                    onDarkModeChange = {
                        prefs.edit().putBoolean("dark_mode", it).apply()
                        // 注意：实际深色模式切换需要重启 Activity
                    }
                )
            }
        }
    }
}

@Composable
fun StatusTab(
    isMonitoring: Boolean,
    inactivityDuration: String,
    thresholdHours: Float,
    onThresholdChange: (Float) -> Unit,
    onToggleMonitoring: () -> Unit,
    onResetTimer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // 状态卡片
            StatusCard(
                isMonitoring = isMonitoring,
                inactivityDuration = inactivityDuration,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            // 阈值设置卡片
            ThresholdCard(
                thresholdHours = thresholdHours,
                onThresholdChange = onThresholdChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            // 控制按钮
            ControlButtons(
                isMonitoring = isMonitoring,
                onToggleMonitoring = onToggleMonitoring,
                onResetTimer = onResetTimer,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            // 统计信息（占位符）
            StatisticsCard(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StatusCard(
    isMonitoring: Boolean,
    inactivityDuration: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isMonitoring) 
                    stringResource(R.string.monitoring_status_running)
                else 
                    stringResource(R.string.monitoring_status_stopped),
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "时间",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${stringResource(R.string.last_activity_label)}: $inactivityDuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
            }
            
            if (!isMonitoring) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "服务已停止，点击下方按钮开始监控",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThresholdCard(
    thresholdHours: Float,
    onThresholdChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "阈值",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.threshold_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = stringResource(R.string.threshold_description).format(thresholdHours.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Slider(
                value = thresholdHours,
                onValueChange = onThresholdChange,
                valueRange = 1f..48f,
                steps = 47,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1h", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("24h", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("48h", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ControlButtons(
    isMonitoring: Boolean,
    onToggleMonitoring: () -> Unit,
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onToggleMonitoring,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMonitoring) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isMonitoring) 
                    stringResource(R.string.stop_monitoring)
                else 
                    stringResource(R.string.start_monitoring),
                fontSize = 16.sp
            )
        }
        
        OutlinedButton(
            onClick = onResetTimer,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.manual_reset))
        }
        
        // 测试警报按钮
        OutlinedButton(
            onClick = {
                // TODO: 测试警报功能
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.alarm_test))
        }
    }
}

@Composable
fun StatisticsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "统计",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.activity_stats),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 占位统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    title = stringResource(R.string.today),
                    value = "8.5h",
                    icon = Icons.Default.Today
                )
                StatItem(
                    title = stringResource(R.string.week),
                    value = "62h",
                    icon = Icons.Default.DateRange
                )
                StatItem(
                    title = stringResource(R.string.month),
                    value = "240h",
                    icon = Icons.Default.CalendarMonth
                )
            }
            
            Text(
                text = "统计功能开发中...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ContactsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Contacts,
            contentDescription = "联系人",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "紧急联系人功能",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "开发中...\n未来版本将支持添加紧急联系人，\n在警报触发时自动通知他们。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* TODO */ },
            enabled = false
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_contact))
        }
    }
}

@Composable
fun SettingsTab(
    autoStart: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 开机自启动
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.auto_start),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "设备重启后自动启动监控",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoStart,
                            onCheckedChange = onAutoStartChange
                        )
                    }
                    
                    Divider()
                    
                    // 深色模式
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.enable_dark_mode),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "跟随系统或手动选择主题",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = onDarkModeChange
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "关于",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "安全监控 v1.0\n基于 GitHub Actions 自动构建",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedButton(
                        onClick = { /* TODO: 打开关于页面 */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("检查更新")
                    }
                }
            }
        }
        
        item {
            Text(
                text = stringResource(R.string.hint_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}