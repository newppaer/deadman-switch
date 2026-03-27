package com.example.deadmanswitch

import android.Manifest
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.deadmanswitch.data.ActivityLogManager
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
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    val contactManager = remember { ContactManager(context) }

    // 常规设置
    var autoStart by remember { mutableStateOf(settings.autoStart) }
    var darkMode by remember { mutableIntStateOf(settings.darkMode) }

    // 推送设置
    var smsEnabled by remember { mutableStateOf(contactManager.isSmsEnabled()) }
    var wecomUrl by remember { mutableStateOf(settings.wecomWebhookUrl) }
    var openclawUrl by remember { mutableStateOf(settings.openclawApiUrl) }
    var openclawToken by remember { mutableStateOf(settings.openclawToken) }

    // 联系人
    var contacts by remember { mutableStateOf(contactManager.getAll()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // SMS 权限申请
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            smsEnabled = true
            contactManager.setSmsEnabled(true)
            Toast.makeText(context, "短信推送已开启", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "需要短信权限才能发送通知", Toast.LENGTH_LONG).show()
        }
    }

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
            // ===== 常规设置 =====
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

            // ===== 推送设置 =====
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("推送通知", style = MaterialTheme.typography.titleMedium)
                Text("超过阈值时自动推送警报", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 短信开关
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("📱 短信推送", style = MaterialTheme.typography.titleSmall)
                                Text("通过手机短信通知紧急联系人", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = smsEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        // 检查权限
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                                            != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                        } else {
                                            smsEnabled = true
                                            contactManager.setSmsEnabled(true)
                                        }
                                    } else {
                                        smsEnabled = false
                                        contactManager.setSmsEnabled(false)
                                    }
                                }
                            )
                        }

                        // 紧急联系人列表
                        if (smsEnabled) {
                            Divider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("紧急联系人", style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "添加联系人")
                                }
                            }

                            if (contacts.isEmpty()) {
                                Text("暂无联系人，点击 + 添加", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }

                            contacts.forEachIndexed { index, contact ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(contact.phone, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            contactManager.remove(index)
                                            contacts = contactManager.getAll()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 企业微信 Webhook
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💼 企业微信推送", style = MaterialTheme.typography.titleSmall)
                        Text("群机器人 Webhook URL，在企微群设置→群机器人中添加", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = wecomUrl,
                            onValueChange = {
                                wecomUrl = it
                                settings.wecomWebhookUrl = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // OpenClaw API
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🤖 OpenClaw API 推送", style = MaterialTheme.typography.titleSmall)
                        Text("通过 OpenClaw 转发到微信/QQ/Telegram 等渠道", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = openclawUrl,
                            onValueChange = {
                                openclawUrl = it
                                settings.openclawApiUrl = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("http://your-server:port/api/push") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = openclawToken,
                            onValueChange = {
                                openclawToken = it
                                settings.openclawToken = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Token（可选）", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ===== 清除历史 =====
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { ActivityLogManager(context).clear() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清除活动历史")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("手机号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        contactManager.add(EmergencyContact(name.trim(), phone.trim()))
                        contacts = contactManager.getAll()
                        showAddDialog = false
                    }
                }) { Text("添加") }
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
