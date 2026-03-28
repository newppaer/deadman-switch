package com.example.deadmanswitch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.example.deadmanswitch.push.PushManager
import com.example.deadmanswitch.ui.theme.DeadManSwitchTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings = remember { SettingsManager(this) }
            var darkMode by remember { mutableIntStateOf(settings.darkMode) }
            // darkMode 变化时实时重建主题
            key(darkMode) {
                DeadManSwitchTheme(darkMode = darkMode) {
                    SettingsScreen(
                        onBack = { finish() },
                        darkMode = darkMode,
                        onDarkModeChange = { darkMode = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    darkMode: Int = 0,
    onDarkModeChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    val contactManager = remember { ContactManager(context) }

    // 常规
    var autoStart by remember { mutableStateOf(settings.autoStart) }
    // darkMode 从参数传入，变更后回调

    // 短信
    var smsEnabled by remember { mutableStateOf(contactManager.isSmsEnabled()) }
    var smsMessage by remember { mutableStateOf(contactManager.getSmsMessage()) }
    var contacts by remember { mutableStateOf(contactManager.getAll()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 企业微信
    var wecomEnabled by remember { mutableStateOf(settings.wecomEnabled) }
    var wecomUrl by remember { mutableStateOf(settings.wecomWebhookUrl) }
    var wecomMessage by remember { mutableStateOf(settings.wecomMessage) }

    // OpenClaw
    var openclawEnabled by remember { mutableStateOf(settings.openclawEnabled) }
    var openclawUrl by remember { mutableStateOf(settings.openclawApiUrl) }
    var openclawToken by remember { mutableStateOf(settings.openclawToken) }
    var openclawMessage by remember { mutableStateOf(settings.openclawMessage) }

    // 权限弹窗
    var showPermDialog by remember { mutableStateOf(false) }
    var permName by remember { mutableStateOf("") }

    // 测试状态
    var testStatus by remember { mutableStateOf("") }

    // SMS 权限申请
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            smsEnabled = true
            contactManager.setSmsEnabled(true)
            Toast.makeText(context, "短信推送已开启", Toast.LENGTH_SHORT).show()
        } else {
            showPermDialog = true
            permName = "短信"
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
            item { Text("常规", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("开机自动启动")
                    Switch(checked = autoStart, onCheckedChange = {
                        autoStart = it; settings.autoStart = it
                    })
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
                    SegmentedButton(selectedIndex = darkMode, options = options,
                        onSelect = { onDarkModeChange(it); settings.darkMode = it })
                }
            }

            // ===== 推送通知 =====
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("推送通知", style = MaterialTheme.typography.titleMedium)
                Text("超过阈值时自动推送警报",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // --- 短信推送 ---
            item {
                PushChannelCard(
                    icon = "📱",
                    title = "短信推送",
                    subtitle = "通过手机短信通知紧急联系人",
                    enabled = smsEnabled,
                    onToggle = { enable ->
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    smsEnabled = true; contactManager.setSmsEnabled(true)
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                }
                            } else {
                                smsEnabled = true; contactManager.setSmsEnabled(true)
                            }
                        } else {
                            smsEnabled = false; contactManager.setSmsEnabled(false)
                        }
                    },
                    hasConfig = smsEnabled && contacts.isNotEmpty(),
                    onTest = {
                        if (contacts.isEmpty()) {
                            testStatus = "⚠️ 请先添加联系人"
                        } else {
                            testStatus = "正在发送测试短信..."
                            try {
                                contactManager.sendTestSms()
                                testStatus = "✅ 测试短信已发送"
                            } catch (e: Exception) {
                                testStatus = "❌ 发送失败: ${e.message}"
                            }
                        }
                    }
                ) {
                    if (smsEnabled) {
                        Divider()
                        // 短信内容
                        OutlinedTextField(
                            value = smsMessage,
                            onValueChange = {
                                smsMessage = it; contactManager.setSmsMessage(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("短信内容") },
                            minLines = 2,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 联系人
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("紧急联系人", style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "添加")
                            }
                        }
                        if (contacts.isEmpty()) {
                            Text("暂无联系人，点击 + 添加",
                                style = MaterialTheme.typography.bodySmall,
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
                                IconButton(onClick = {
                                    contactManager.remove(index); contacts = contactManager.getAll()
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // --- 企业微信 ---
            item {
                PushChannelCard(
                    icon = "💼",
                    title = "企业微信推送",
                    subtitle = "群机器人 Webhook（设置→群机器人→添加）",
                    enabled = wecomEnabled,
                    onToggle = {
                        wecomEnabled = it; settings.wecomEnabled = it
                    },
                    hasConfig = wecomEnabled && wecomUrl.isNotBlank(),
                    onTest = {
                        if (wecomUrl.isBlank()) {
                            testStatus = "⚠️ 请先填入 Webhook URL"
                        } else {
                            testStatus = "正在发送测试消息..."
                            val msg = wecomMessage.ifBlank { "🧪 DeadManSwitch 测试消息 - 推送正常 ✅" }
                            PushManager.sendWeChatWork(wecomUrl, msg)
                            testStatus = "✅ 测试消息已发送"
                        }
                    }
                ) {
                    if (wecomEnabled) {
                        Divider()
                        OutlinedTextField(
                            value = wecomUrl,
                            onValueChange = { wecomUrl = it; settings.wecomWebhookUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wecomMessage,
                            onValueChange = { wecomMessage = it; settings.wecomMessage = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("自定义消息（留空用默认）") },
                            minLines = 2,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // --- OpenClaw ---
            item {
                PushChannelCard(
                    icon = "🤖",
                    title = "OpenClaw API 推送",
                    subtitle = "通过 OpenClaw 转发到微信/QQ/Telegram 等",
                    enabled = openclawEnabled,
                    onToggle = {
                        openclawEnabled = it; settings.openclawEnabled = it
                    },
                    hasConfig = openclawEnabled && openclawUrl.isNotBlank(),
                    onTest = {
                        if (openclawUrl.isBlank()) {
                            testStatus = "⚠️ 请先填入 API 地址"
                        } else {
                            testStatus = "正在发送测试消息..."
                            val msg = openclawMessage.ifBlank { "🧪 DeadManSwitch 测试消息 - 推送正常 ✅" }
                            PushManager.sendHttpPost(openclawUrl, openclawToken, "测试", msg)
                            testStatus = "✅ 测试消息已发送"
                        }
                    }
                ) {
                    if (openclawEnabled) {
                        Divider()
                        OutlinedTextField(
                            value = openclawUrl,
                            onValueChange = { openclawUrl = it; settings.openclawApiUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("http://your-server:port/api/push") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = openclawToken,
                            onValueChange = { openclawToken = it; settings.openclawToken = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Token（可选）") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = openclawMessage,
                            onValueChange = { openclawMessage = it; settings.openclawMessage = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("自定义消息（留空用默认）") },
                            minLines = 2,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 测试状态
            if (testStatus.isNotBlank()) {
                item {
                    Text(testStatus, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            // ===== 清除历史 =====
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { ActivityLogManager(context).clear() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("清除活动历史") }
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
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it },
                        label = { Text("手机号") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        contactManager.add(EmergencyContact(name.trim(), phone.trim()))
                        contacts = contactManager.getAll(); showAddDialog = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }

    // 权限引导弹窗
    if (showPermDialog) {
        AlertDialog(
            onDismissRequest = { showPermDialog = false },
            title = { Text("需要${permName}权限") },
            text = { Text("请在系统设置中手动开启${permName}权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showPermDialog = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) { Text("去设置") }
            },
            dismissButton = { TextButton(onClick = { showPermDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun PushChannelCard(
    icon: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    hasConfig: Boolean,
    onTest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$icon $title", style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            // 测试按钮
            if (enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (hasConfig) "✅ 已配置" else "⚠️ 请完善配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasConfig) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(
                        onClick = onTest,
                        enabled = hasConfig,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("🧪 测试", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            content()
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
            if (index < options.size - 1) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}
