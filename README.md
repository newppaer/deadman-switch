# DeadManSwitch - 被动安全监控

类似 "死了么" 的安全监控 App，但使用被动监听（屏幕解锁）代替主动签到。

## 功能

- **被动监控**: 监听屏幕解锁事件，自动重置计时器
- **可配置阈值**: 1-48 小时无活动警报
- **后台运行**: 前台服务保活，支持开机自启
- **本地警报**: 通知提醒 + 震动

## 项目结构

```
app/src/main/java/com/example/deadmanswitch/
├── MainActivity.kt          # 主界面 (Jetpack Compose)
├── DeadManApp.kt            # Application 初始化
├── service/
│   └── MonitorService.kt    # 后台监控服务
├── receiver/
│   ├── ScreenUnlockReceiver.kt  # 屏幕解锁监听
│   └── BootReceiver.kt      # 开机启动
└── ui/theme/
    └── Theme.kt             # 主题
```

## 使用方法

1. 打开 App，设置无活动阈值（默认 12 小时）
2. 点击"开始监控"
3. 正常使用手机即可，每次解锁会自动重置计时器
4. 超过阈值无活动会发出警报通知

## 扩展方向

- [ ] UsageStatsManager 监听 App 使用
- [ ] 紧急联系人短信通知
- [ ] OpenClaw API 联动
- [ ] 位置上报
- [ ] 智能阈值（工作日/周末区分）

## 编译

```bash
./gradlew assembleDebug
```

安装: `app/build/outputs/apk/debug/app-debug.apk`