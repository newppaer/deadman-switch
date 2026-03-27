package com.example.deadmanswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deadmanswitch.data.SettingsManager
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeadManSwitchTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { SettingsManager(context) }

    var autoStart by remember { mutableStateOf(settings.autoStart) }
    var darkMode by remember { mutableIntStateOf(settings.darkMode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 常规设置
            item {
                Text("常规", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("开机自动启动")
                    Switch(
                        checked = autoStart,
                        onCheckedChange = {
                            autoStart = it
                            settings.autoStart = it
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("深色模式")
                    val options = listOf("跟随系统", "亮色", "暗色")
                    SegmentedButton(
                        selectedIndex = darkMode,
                        options = options,
                        onSelect = { darkMode = it; settings.darkMode = it }
                    )
                }
            }

            // 清除历史
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        com.example.deadmanswitch.data.ActivityLogManager(context).clear()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清除活动历史")
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    Row {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            OutlinedButton(
                onClick = { onSelect(index) },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
            if (index < options.size - 1) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}
