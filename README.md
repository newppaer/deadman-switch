# DeadManSwitch - 被动安全监控

类似 "死了么" 的安全监控 App，使用被动监听（屏幕解锁）代替主动签到。超过设定时间无活动自动触发多渠道推送警报。

## ✨ 功能

### 核心监控
- **被动监控**: 监听屏幕解锁/锁屏事件，自动重置计时器
- **可配置阈值**: 1-48 小时无活动触发警报
- **前台服务保活**: WakeLock + ForegroundService，支持开机自启
- **深色模式**: 跟随系统 / 强制亮色 / 强制暗色

### 多渠道推送（超过阈值触发）
- **📱 短信推送**: 可编辑内容，支持多个紧急联系人，默认关闭
- **💼 企业微信**: 群机器人 Webhook，可编辑自定义消息
- **🤖 OpenClaw API**: 通用 HTTP POST 接口，可转发到微信/QQ/Telegram
- **本地通知**: 震动 + 高优先级通知

### 其他功能
- **解锁/锁屏统计**: 温馨调皮提示语（按次数分级）
- **活动历史**: 独立页面，支持导出复制
- **崩溃日志**: 闪退自动弹出，可复制
- **测试按钮**: 每个推送渠道独立测试
- **权限管理**: 被拒绝后引导跳转系统设置页

## 📁 项目结构

```
app/src/main/java/com/example/deadmanswitch/
├── MainActivity.kt              # 主界面（倒计时 + 统计）
├── SettingsActivity.kt          # 设置页（推送配置 + 常规设置）
├── LogActivity.kt               # 活动历史独立页面
├── CrashLogger.kt               # 崩溃日志记录器
├── DeadManApp.kt                # Application 初始化
├── service/
│   └── MonitorService.kt        # 后台监控服务
├── receiver/
│   ├── ScreenUnlockReceiver.kt  # 屏幕解锁监听（Manifest 静态）
│   └── BootReceiver.kt          # 开机启动
├── data/
│   ├── SettingsManager.kt       # 设置持久化
│   ├── ActivityLogManager.kt    # 活动日志
│   └── ContactManager.kt        # 紧急联系人管理
├── push/
│   └── PushManager.kt           # HTTP 推送（企微/OpenClaw）
└── ui/theme/
    └── Theme.kt                 # Material You 主题
```

## 🚀 使用方法

### 基础使用
1. 打开 App，设置无活动阈值（默认 12 小时）
2. 点击"开始监控"
3. 正常使用手机，每次解锁自动重置计时器
4. 超过阈值无活动 → 触发警报

### 配置推送
1. 点击 ⚙️ 进入设置页
2. 根据需要开启各推送渠道
3. 填入配置信息（Webhook URL / 联系人 / API 地址）
4. 使用 🧪 测试按钮确认推送正常

### 企业微信配置
1. 在企微群中添加"自定义机器人"
2. 复制 Webhook URL 到 App
3. 开启开关，点击测试

### OpenClaw 配置
1. 确保 OpenClaw 有 HTTP API 端口开放
2. 填入 API 地址和可选 Token
3. POST 格式：`{"title":"...","content":"...","timestamp":...,"source":"DeadManSwitch"}`

## 🔧 编译

### 本地编译
```bash
./gradlew assembleDebug
```
安装: `app/build/outputs/apk/debug/app-debug.apk`

### CI/CD
GitHub Actions 自动编译，push 到 main 分支触发。

## 📋 待办

- [ ] UsageStatsManager 监听 App 使用
- [ ] 智能阈值（工作日/周末区分）
- [ ] 位置上报
- [ ] OpenClaw 深度联动

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **最低版本**: Android 8.0 (API 26)
- **目标版本**: Android 14 (API 34)
- **构建**: Gradle 8.6 + AGP 8.2.2
