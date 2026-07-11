# 会议门牌 Android 应用 — 设计文档

- **日期**: 2026-07-10
- **版本**: 1.0
- **状态**: 已确认，待执行

## 1. 概述

将智慧园区管理系统的会议门牌页面（`meeting/board/index.vue`）封装为 Android 原生应用，运行在会议室门口竖屏平板上，支持开机自启动、全屏常亮、首次扫码/输入绑定会议室。

### 约束

| 项目 | 值 |
|:---|:---|
| 设备 | 会议室门口 Android 平板，竖屏安装 |
| 分辨率 | 1080×1920 (portrait) |
| Android 版本 | 8.0+ (API 26+) |
| 服务器 | 1.94.26.126:80 (Nginx) |
| 后端 API | `/meeting/board/{roomId}` (无需认证) |

## 2. 方案架构

**方案 A — WebView 壳**：Android 原生 Activity 内嵌 WebView，加载远端门牌 HTML 页面。

```
┌──────────────────────────────┐
│     Android 原生壳 (Kotlin)   │
│  ┌────────────────────────┐  │
│  │  BootReceiver          │  │  ← 开机自启
│  │  SetupActivity         │  │  ← 首次配置会议室
│  │  KioskActivity         │  │  ← 全屏 WebView
│  │  ┌──────────────────┐  │  │
│  │  │  WebView         │  │  │
│  │  │  board.html      │  │  │  ← 远端门牌页面
│  │  │  ?roomId=xxx     │  │  │
│  │  └──────────────────┘  │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

### 选型理由

- 已有完整的深色主题门牌页面，无需重写
- WebView 门牌方案是行业通行做法
- 页面更新无需重新打包 APK
- 开发量最低，交付速度最快

## 3. 组件详情

### 3.1 门牌独立页面 (`board.html`)

**位置**: `ruoyi-ui/public/board.html`

从 `meeting/board/index.vue` 抽离为纯静态 HTML，使用原生 JS + CSS 实现（不依赖 Vue/axios）。

| 功能 | 实现 |
|:---|:---|
| 状态展示 | 空闲 (绿) / 使用中 (红) / 即将开始 (橙) |
| 今日会议列表 | 从 API 加载，按时间排序 |
| 下一场倒计时 | 每秒 tick 倒计时 |
| 实时时钟 | 每秒刷新 |
| 自动刷新 | 30 秒 `setInterval` 完整 reload |
| 错误处理 | 网络断开显示半透明错误覆盖层 |
| 深色主题 | CSS 变量，暗色底座 |

**API 调用**：使用 `fetch()` 调用 `/prod-api/meeting/board/{roomId}`，无需认证 token。

**URL**: `http://1.94.26.126/board.html?roomId={roomId}`

### 3.2 后端补充

| 改动项 | 说明 |
|:---|:---|
| 会议室列表接口 | `GET /meeting/room/simpleList`，返回 `[{roomId, roomName, location}]`，无需认证 |
| 门牌 API | 现有 `/meeting/board/{roomId}` 已支持 `isToken: false`，复用 |

### 3.3 Android 应用

**项目结构**:

```
meeting-board-app/
├── app/src/main/
│   ├── java/com/ruoyi/board/
│   │   ├── BoardApplication.kt     ← 初始化
│   │   ├── SetupActivity.kt        ← 会议室选择页
│   │   ├── KioskActivity.kt        ← 全屏 WebView
│   │   └── BootReceiver.kt         ← 开机广播
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_setup.xml
│   │   │   └── activity_kiosk.xml
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   └── mipmap-*/
│   │       └── ic_launcher.png
│   └── AndroidManifest.xml
├── build.gradle.kts
├── gradle.properties
└── gradle/
```

**Activity 流转**:

```
App 启动
  │
  ├── roomId 为空? ──→ SetupActivity
  │                      │
  │                      ├── 用户选择会议室
  │                      ├── 存入 SharedPreferences
  │                      └── → KioskActivity
  │
  └── roomId 已配置? ──→ KioskActivity
                           │
                           └── WebView 加载 board.html?roomId=xxx
```

**关键配置**:

| 特性 | 实现 |
|:---|:---|
| 全屏 | `SYSTEM_UI_FLAG_FULLSCREEN` + `SYSTEM_UI_FLAG_HIDE_NAVIGATION` + `SYSTEM_UI_FLAG_IMMERSIVE_STICKY` |
| 常亮 | `FLAG_KEEP_SCREEN_ON` |
| 开机自启 | `BootReceiver` 监听 `BOOT_COMPLETED` + `RECEIVE_BOOT_COMPLETED` 权限 |
| 退出管理 | 长按屏幕四角 5 秒 → 密码验证 → 返回 SetupActivity |
| WebView 安全 | 禁用缩放、长按、文本选择、文件访问 |
| 网络异常 | WebViewClient `onReceivedError` → JavaScript 显示错误覆盖层 |

**AndroidManifest 关键声明**:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<activity android:name=".KioskActivity"
    android:screenOrientation="portrait"
    android:configChanges="orientation|screenSize" />

<activity android:name=".SetupActivity"
    android:screenOrientation="portrait" />

<receiver android:name=".BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

## 4. 开发环境

| 工具 | 版本/说明 |
|:---|:---|
| IDE | Android Studio Hedgehog (2023.1+) |
| 语言 | Kotlin 1.9+ |
| 构建系统 | Gradle 8.x + Kotlin DSL |
| minSdk | 26 (Android 8.0) |
| targetSdk | 34 (Android 14) |
| 本地测试 | Android Emulator (1080×1920 portrait AVD) / ADB USB 实体机 |
| 门牌页面调试 | Chrome DevTools remote debugging (`chrome://inspect`) |

## 5. 打包与分发

```bash
# Debug APK
./gradlew assembleDebug       → app/build/outputs/apk/debug/app-debug.apk

# Release APK (需配置签名)
./gradlew assembleRelease     → app/build/outputs/apk/release/app-release.apk
```

**安装后流程**:
1. `adb install app-release.apk`（或 U 盘/OTA）
2. 首次启动 → SetupActivity → 选择会议室 → 确认
3. 平板重启 → BootReceiver → 自动进入 KioskActivity
4. 门牌正常工作

## 6. 文件清单（待产出）

| 文件 | 说明 |
|:---|:---|
| `ruoyi-ui/public/board.html` | 独立门牌页面 |
| `meeting-board-app/` | Android 应用完整工程 |
| 后端: MeetingRoomController 新增 `simpleList` | 会议室选择接口 |
