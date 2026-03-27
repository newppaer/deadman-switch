package com.example.deadmanswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deadmanswitch.data.ContactManager
import com.example.deadmanswitch.data.EmergencyContact
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
    val contactManager = remember { ContactManager(context) }

    var autoStart by remember { mutableStateOf(settings.autoStart) }
    var smsEnabled by remember { mutableStateOf(contactManager.isSmsEnabled()) }
    var smsMessage by remember { mutableStateOf(contactManager.getSmsMessage()) }
    var contacts by remember { mutableStateOf(contactManager.getAll()) }
    var showAddDialog by remember { mutableStateOf(false) }
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

            // 紧急联系人
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("紧急联系人", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            }

            if (contacts.isEmpty()) {
                item {
                    Text(
                        "暂无联系人，点击 + 添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(contacts) { index, contact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.bodyLarge)
                            Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            contactManager.remove(index)
                            contacts = contactManager.getAll()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }

            // 短信设置
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("短信通知", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("触发警报时发送短信")
                    Switch(
                        checked = smsEnabled,
                        onCheckedChange = {
                            smsEnabled = it
                            contactManager.setSmsEnabled(it)
                        }
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = smsMessage,
                    onValueChange = {
                        smsMessage = it
                        contactManager.setSmsMessage(it)
                    },
                    label = { Text("短信内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
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

    // 添加联系人弹窗
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加紧急联系人") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("姓名") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("手机号") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            contactManager.add(EmergencyContact(name.trim(), phone.trim()))
                            contacts = contactManager.getAll()
                            showAddDialog = false
                        }
                    }
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
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
