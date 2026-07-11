# 会议门牌 Android 应用 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将会议门牌 Web 页面封装为 Android 原生 WebView 应用，支持开机自启、竖屏全屏、首次配置绑定会议室

**Architecture:** 创建一个独立 `board.html`（纯 HTML/JS/CSS，走 Nginx）替代依赖 Vue SPA 的门牌页；Android 原生 Activity 内嵌 WebView 加载该 URL；后端新增无需认证的会议室列表接口

**Tech Stack:** HTML5 + CSS3 + Vanilla JS (门牌页面), Kotlin 1.9 + Gradle 8.x + Android SDK 34 (原生壳), Spring Boot 2 + MyBatis (后端)

## Global Constraints

- 最低 Android 8.0 (API 26)，目标 SDK 34
- 竖屏 (portrait) 固定，1080×1920
- 所有门牌接口无需认证（`isToken: false` 或匿名访问）
- 服务器: 1.94.26.126:80 (Nginx)，后端端口 8090
- 复用现有 `/meeting/board/{roomId}` API，不改动其逻辑

---

### Task 1: 后端 — 会议室简单列表接口

**Files:**
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/meeting/MeetingRoomController.java`
- Check: `ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`

**Interfaces:**
- Produces: `GET /meeting/room/simpleList` → `AjaxResult` 内含 `data: [{roomId, roomName, location}]`

- [ ] **Step 1: 添加 simpleList 方法到 MeetingRoomController**

在 `MeetingRoomController.java` 中添加以下方法（紧跟 `getInfo` 方法之后）：

```java
/**
 * 获取会议室简单列表（供门牌平板配置页使用，无需权限认证）
 */
@GetMapping("/simpleList")
public AjaxResult simpleList()
{
    List<MeetingRoom> list = meetingRoomService.selectMeetingRoomList(new MeetingRoom());
    List<Map<String, Object>> simpleList = new java.util.ArrayList<>();
    for (MeetingRoom room : list)
    {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("roomId", room.getRoomId());
        item.put("roomName", room.getRoomName());
        item.put("location", room.getLocation());
        simpleList.add(item);
    }
    return success(simpleList);
}
```

- [ ] **Step 2: 配置 SecurityConfig 放行 simpleList 接口**

读取 `ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`，在匿名访问列表中添加：

```java
// 门牌平板配置：会议室简单列表
.antMatchers("/meeting/room/simpleList").anonymous()
```

注意：必须以 `.anonymous()` 方式添加，确保无需 token 即可访问。

- [ ] **Step 3: 编译后端验证**

```bash
cd <项目根目录> && mvn clean compile -q
```

预期：BUILD SUCCESS，无编译错误。

- [ ] **Step 4: Commit**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/controller/meeting/MeetingRoomController.java
git add ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java
git commit -m "feat: add /meeting/room/simpleList anonymous endpoint for board setup"
```

---

### Task 2: 门牌独立页面 board.html

**Files:**
- Create: `ruoyi-ui/public/board.html`

**Interfaces:**
- Consumes: `GET /prod-api/meeting/room/simpleList` → `{code:200, data:[{roomId,roomName,location}]}`
- Consumes: `GET /prod-api/meeting/board/{roomId}` → `{code:200, data:{room, todayBookings, currentStatus, currentBooking, nextBooking}}`
- Produces: 浏览器加载 `http://1.94.26.126/board.html?roomId=N`

- [ ] **Step 1: 创建 board.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no">
<meta name="apple-mobile-web-app-capable" content="yes">
<title>会议门牌</title>
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
body{
  min-height:100vh;
  background:#1a1a2e;
  color:#e0e0e0;
  font-family:-apple-system,"Microsoft YaHei","PingFang SC",sans-serif;
  display:flex;flex-direction:column;
  padding:24px 32px;
}
/* Header */
.board-header{text-align:center;padding:16px 0 24px;border-bottom:1px solid rgba(255,255,255,.08)}
.room-name{font-size:42px;font-weight:700;color:#fff;letter-spacing:2px;margin-bottom:8px}
.room-info{font-size:18px;color:#a0a0b8}
.info-divider{margin:0 12px;color:#3a3a5c}
.room-equipment{font-size:15px;color:#707088;margin-top:6px}
/* Status */
.status-area{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:280px;margin:20px 0;border-radius:16px;transition:background .5s}
.status-free{background:linear-gradient(135deg,#16213e,#1a3a1a);border:2px solid rgba(76,175,80,.3)}
.status-inuse{background:linear-gradient(135deg,#2d1f1f,#4a2020);border:2px solid rgba(244,67,54,.4)}
.status-coming{background:linear-gradient(135deg,#1e2a38,#3d3520);border:2px solid rgba(255,152,0,.4)}
.status-icon{font-size:64px;margin-bottom:8px}
.status-label{font-size:48px;font-weight:700;letter-spacing:6px}
.status-free .status-label{color:#4caf50}
.status-inuse .status-label{color:#f44336}
.status-coming .status-label{color:#ff9800}
.status-time{font-size:20px;color:#a0a0b8;margin-top:8px}
.current-booking{text-align:center;margin-top:16px;padding:12px 40px;background:rgba(255,255,255,.06);border-radius:8px}
.current-booking .booking-title{font-size:22px;color:#fff;font-weight:600}
.current-booking .booking-time{font-size:17px;color:#9090a8;margin-top:4px}
.current-booking .booking-host{font-size:15px;color:#707088;margin-top:2px}
/* Schedule */
.schedule-section{margin-bottom:20px}
.section-title{font-size:20px;color:#9090a8;margin-bottom:12px;padding-left:4px;letter-spacing:1px}
.schedule-item{display:flex;align-items:center;padding:14px 16px;margin-bottom:6px;border-radius:8px;background:rgba(255,255,255,.04);border-left:3px solid transparent;transition:background .3s}
.schedule-item.item-active{background:rgba(244,67,54,.12);border-left-color:#f44336}
.schedule-item.item-upcoming{background:rgba(255,152,0,.08);border-left-color:#ff9800}
.schedule-item.item-past{opacity:.45}
.schedule-time{font-size:22px;font-weight:700;color:#c0c0d0;min-width:60px;text-align:center;margin-right:16px}
.schedule-body{flex:1;min-width:0}
.schedule-title{font-size:17px;color:#e0e0e0;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.schedule-meta{font-size:13px;color:#707088;margin-top:3px}
.schedule-meta span{margin-right:12px}
.schedule-tag{font-size:13px;padding:3px 10px;border-radius:12px;white-space:nowrap;margin-left:12px}
.tag-active{background:rgba(244,67,54,.2);color:#f44336}
.tag-upcoming{background:rgba(255,152,0,.2);color:#ff9800}
.tag-past{background:rgba(255,255,255,.05);color:#555}
.schedule-empty{text-align:center;font-size:16px;color:#555;padding:32px 0}
/* Countdown */
.countdown-area{text-align:center;padding:20px;background:rgba(255,152,0,.08);border-radius:12px;border:1px solid rgba(255,152,0,.2);margin-bottom:20px}
.countdown-label{font-size:15px;color:#9090a8;margin-bottom:4px}
.countdown-time{font-size:36px;font-weight:700;color:#ff9800;letter-spacing:2px}
.countdown-booking{margin-top:8px}
.countdown-booking .booking-title{font-size:17px;color:#e0e0e0}
.countdown-booking .booking-time{font-size:14px;color:#808098;margin-top:2px}
/* Footer */
.board-footer{display:flex;align-items:center;justify-content:center;font-size:22px;color:#555;padding:12px 0 8px;gap:10px}
.refresh-dot{width:8px;height:8px;border-radius:50%;background:#555;transition:background .3s}
.refresh-dot.active{background:#4caf50}
/* Error overlay */
.error-overlay{display:none;position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,.6);z-index:1000;align-items:center;justify-content:center;flex-direction:column}
.error-overlay.visible{display:flex}
.error-msg{color:#fff;font-size:20px;margin-bottom:12px}
/* Setup page (config mode) */
.setup-container{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;background:#1a1a2e;color:#e0e0e0;padding:40px}
.setup-title{font-size:28px;font-weight:700;color:#fff;margin-bottom:32px}
.setup-select{width:80%;max-width:500px;padding:14px 18px;font-size:18px;border-radius:8px;border:1px solid #3a3a5c;background:#16213e;color:#e0e0e0;margin-bottom:24px;outline:none;appearance:none;text-align:center}
.setup-select:focus{border-color:#409eff}
.setup-btn{padding:14px 48px;font-size:20px;font-weight:600;color:#fff;background:#409eff;border:none;border-radius:8px;cursor:pointer}
.setup-btn:disabled{background:#555;cursor:not-allowed}
/* Responsive */
@media(max-width:1024px){
  body{padding:16px 20px}
  .room-name{font-size:32px}
  .status-icon{font-size:48px}
  .status-label{font-size:36px}
  .countdown-time{font-size:28px}
  .board-footer{font-size:18px}
}
</style>
</head>
<body>
  <!-- Error overlay -->
  <div id="errorOverlay" class="error-overlay">
    <div class="error-msg">⚠️ 网络连接异常</div>
    <div class="error-msg" style="font-size:16px;color:#aaa">正在尝试重新连接...</div>
  </div>

  <!-- Main content -->
  <div id="mainContent"></div>

  <div id="boardFooter" class="board-footer">
    <span id="clockDisplay"></span>
    <span id="refreshDot" class="refresh-dot"></span>
  </div>

  <script>
  (function(){
    var API = '/prod-api';
    var roomId = getParam('roomId');
    var room = {};
    var todayBookings = [];
    var currentStatus = 'FREE';
    var currentBooking = null;
    var nextBooking = null;
    var countdownMs = 0;
    var errorCount = 0;
    var MAX_ERRORS = 3;

    // --- URL param ---
    function getParam(name) {
      var m = location.search.match(new RegExp('[?&]' + name + '=([^&]*)'));
      return m ? decodeURIComponent(m[1]) : null;
    }

    // --- Time helpers ---
    function parseTime(s) {
      if (!s) return null;
      var t = new Date(s.replace(/-/g, '/'));
      return isNaN(t.getTime()) ? null : t;
    }

    function formatShortTime(s) {
      if (!s) return '';
      var parts = s.split(' ');
      if (parts.length > 1) {
        var segs = parts[1].split(':');
        return segs[0] + ':' + segs[1];
      }
      return s;
    }

    // --- Template render ---
    function render() {
      var cls = 'status-free';
      var icon = '🟢';  // green circle
      var text = '空闲';  // 空闲
      if (currentStatus === 'IN_USE') { cls = 'status-inuse'; icon = '🔴'; text = '使用中'; }
      if (currentStatus === 'COMING_SOON') { cls = 'status-coming'; icon = '🟡'; text = '即将开始'; }

      var statusTimeHtml = '';
      if (currentStatus === 'IN_USE' && currentBooking) {
        statusTimeHtml = formatShortTime(currentBooking.startTime) + ' - ' + formatShortTime(currentBooking.endTime);
      }

      var curBookingHtml = '';
      if (currentStatus === 'IN_USE' && currentBooking) {
        curBookingHtml =
          '<div class="current-booking">' +
            '<div class="booking-title">' + esc(currentBooking.title) + '</div>' +
            '<div class="booking-time">' + formatShortTime(currentBooking.startTime) + ' - ' + formatShortTime(currentBooking.endTime) + '</div>' +
            (currentBooking.hostName ? '<div class="booking-host">' + esc(currentBooking.hostName) + '</div>' : '') +
          '</div>';
      }

      var scheduleHtml = '';
      if (todayBookings.length > 0) {
        for (var i = 0; i < todayBookings.length; i++) {
          var b = todayBookings[i];
          var itemCls = getItemClass(b);
          var tagCls = getTagClass(b);
          var tagText = getTagText(b);
          scheduleHtml +=
            '<div class="schedule-item ' + itemCls + '">' +
              '<div class="schedule-time">' + formatShortTime(b.startTime) + '</div>' +
              '<div class="schedule-body">' +
                '<div class="schedule-title">' + esc(b.title) + '</div>' +
                '<div class="schedule-meta">' +
                  '<span>' + formatShortTime(b.startTime) + ' - ' + formatShortTime(b.endTime) + '</span>' +
                  (b.hostName ? '<span>' + esc(b.hostName) + '</span>' : '') +
                '</div>' +
              '</div>' +
              '<div class="schedule-tag ' + tagCls + '">' + tagText + '</div>' +
            '</div>';
        }
      } else {
        scheduleHtml = '<div class="schedule-empty">今日暂无会议安排</div>';
      }

      var countdownHtml = '';
      if (nextBooking && getCountdownText()) {
        countdownHtml =
          '<div class="countdown-area">' +
            '<div class="countdown-label">下一场会议</div>' +
            '<div class="countdown-time">' + getCountdownText() + '</div>' +
            '<div class="countdown-booking">' +
              '<div class="booking-title">' + esc(nextBooking.title) + '</div>' +
              '<div class="booking-time">' + (nextBooking.startTime||'') + ' - ' + (nextBooking.endTime||'') + '</div>' +
            '</div>' +
          '</div>';
      }

      document.getElementById('mainContent').innerHTML =
        '<div class="board-header">' +
          '<div class="room-name">' + esc(room.roomName || '未指定') + '</div>' +
          '<div class="room-info">' +
            '<span class="info-item">容纳 ' + (room.capacity||0) + ' 人</span>' +
            '<span class="info-divider">|</span>' +
            '<span class="info-item">' + esc(room.location||'') + '</span>' +
          '</div>' +
          (room.equipment ? '<div class="room-equipment">设备: ' + esc(room.equipment) + '</div>' : '') +
        '</div>' +
        '<div class="status-area ' + cls + '">' +
          '<div class="status-icon">' + icon + '</div>' +
          '<div class="status-label">' + text + '</div>' +
          '<div class="status-time">' + statusTimeHtml + '</div>' +
          curBookingHtml +
        '</div>' +
        '<div class="schedule-section">' +
          '<div class="section-title">今日会议安排</div>' +
          scheduleHtml +
        '</div>' +
        countdownHtml;
    }

    function getItemClass(b) {
      if (currentStatus === 'IN_USE' && currentBooking && currentBooking.bookingId === b.bookingId) return 'item-active';
      var s = parseTime(b.startTime), e = parseTime(b.endTime), n = Date.now();
      if (s && e && s.getTime() <= n && e.getTime() > n) return 'item-active';
      if (s && s.getTime() > n) return 'item-upcoming';
      return 'item-past';
    }

    function getTagClass(b) {
      if (currentStatus === 'IN_USE' && currentBooking && currentBooking.bookingId === b.bookingId) return 'tag-active';
      var s = parseTime(b.startTime), e = parseTime(b.endTime), n = Date.now();
      if (s && e && s.getTime() <= n && e.getTime() > n) return 'tag-active';
      if (s && s.getTime() > n) return 'tag-upcoming';
      return 'tag-past';
    }

    function getTagText(b) {
      if (currentStatus === 'IN_USE' && currentBooking && currentBooking.bookingId === b.bookingId) return '进行中';
      var s = parseTime(b.startTime), e = parseTime(b.endTime), n = Date.now();
      if (s && e && s.getTime() <= n && e.getTime() > n) return '进行中';
      if (s && s.getTime() > n) return '待开始';
      return '已结束';
    }

    function esc(s) {
      if (!s) return '';
      return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    // --- Countdown ---
    function getCountdownText() {
      if (countdownMs <= 0 || !nextBooking) return '';
      var totalSec = Math.floor(countdownMs / 1000);
      var h = Math.floor(totalSec / 3600);
      var m = Math.floor((totalSec % 3600) / 60);
      var s = totalSec % 60;
      if (h > 0) return h + '小时 ' + pad(m) + '分 ' + pad(s) + '秒';
      return pad(m) + '分 ' + pad(s) + '秒';
    }

    function pad(n) { return n < 10 ? '0' + n : '' + n; }

    function updateCountdown() {
      if (!nextBooking) { countdownMs = 0; return; }
      var target = parseTime(nextBooking.startTime);
      if (!target) { countdownMs = 0; return; }
      countdownMs = target.getTime() - Date.now();
      if (countdownMs < 0) countdownMs = 0;
    }

    function tickCountdown() {
      if (!nextBooking) return;
      countdownMs -= 1000;
      if (countdownMs < 0) countdownMs = 0;
      // Re-render countdown area
      render();
    }

    // --- Clock ---
    function updateClock() {
      var now = new Date();
      var s = now.getFullYear() + '-' + pad(now.getMonth()+1) + '-' + pad(now.getDate()) +
        ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
      document.getElementById('clockDisplay').textContent = s;
    }

    // --- API ---
    function loadBoard() {
      if (!roomId) return;
      var dot = document.getElementById('refreshDot');
      if (dot) dot.className = 'refresh-dot active';

      fetch(API + '/meeting/board/' + roomId)
        .then(function(r) { return r.json(); })
        .then(function(res) {
          if (res.code === 200) {
            var d = res.data;
            room = d.room || {};
            todayBookings = d.todayBookings || [];
            currentStatus = d.currentStatus || 'FREE';
            currentBooking = d.currentBooking || null;
            nextBooking = d.nextBooking || null;
            updateCountdown();
            render();
            errorCount = 0;
            hideError();
          }
          if (dot) dot.className = 'refresh-dot';
        }).catch(function() {
          errorCount++;
          if (errorCount >= MAX_ERRORS) showError();
          if (dot) dot.className = 'refresh-dot';
        });
    }

    // --- Error overlay ---
    function showError() {
      document.getElementById('errorOverlay').className = 'error-overlay visible';
    }

    function hideError() {
      document.getElementById('errorOverlay').className = 'error-overlay';
    }

    // --- Setup mode (no roomId) ---
    function showSetup() {
      document.getElementById('boardFooter').style.display = 'none';
      fetch(API + '/meeting/room/simpleList')
        .then(function(r) { return r.json(); })
        .then(function(res) {
          var rooms = (res.code === 200 && res.data) ? res.data : [];
          var opts = '<option value="">请选择会议室</option>';
          for (var i = 0; i < rooms.length; i++) {
            var r = rooms[i];
            opts += '<option value="' + r.roomId + '">' + esc(r.roomName) + ' (' + esc(r.location||'') + ')</option>';
          }
          document.getElementById('mainContent').innerHTML =
            '<div class="setup-container">' +
              '<div class="setup-title">会议室配置</div>' +
              '<div class="setup-title" style="font-size:16px;font-weight:400;color:#9090a8;margin-top:-20px;margin-bottom:24px">请选择本平板对应的会议室</div>' +
              '<select id="roomSelect" class="setup-select">' + opts + '</select>' +
              '<button id="confirmBtn" class="setup-btn" disabled onclick="confirmSetup()">确认</button>' +
            '</div>';
          document.getElementById('roomSelect').addEventListener('change', function() {
            document.getElementById('confirmBtn').disabled = !this.value;
          });
        }).catch(function() {
          document.getElementById('mainContent').innerHTML =
            '<div class="setup-container"><div class="setup-title" style="color:#f44336">无法连接服务器</div></div>';
        });
    }

    window.confirmSetup = function() {
      var val = document.getElementById('roomSelect').value;
      if (!val) return;
      localStorage.setItem('board_roomId', val);
      location.search = '?roomId=' + val;
    };

    // --- Init ---
    if (!roomId) {
      showSetup();
    } else {
      localStorage.setItem('board_roomId', roomId);
      loadBoard();
      setInterval(loadBoard, 30000);
      setInterval(tickCountdown, 1000);
      setInterval(updateClock, 1000);
      updateClock();
    }
  })();
  </script>
</body>
</html>
```

- [ ] **Step 2: 验证 board.html 在浏览器中可用**

打开 `ruoyi-ui/dist/board.html`（如 dist 目录不存在，先复制 board.html 到 dist 中测试），确认文件存在且无 JS 语法错误。

- [ ] **Step 3: Commit**

```bash
git add ruoyi-ui/public/board.html
git commit -m "feat: add standalone board.html for meeting room tablet display"
```

---

### Task 3: Android 工程骨架

**Files:**
- Create: `meeting-board-app/settings.gradle.kts`
- Create: `meeting-board-app/build.gradle.kts`
- Create: `meeting-board-app/gradle.properties`
- Create: `meeting-board-app/gradle/wrapper/gradle-wrapper.properties` (optional, use wrapper)
- Create: `meeting-board-app/app/build.gradle.kts`
- Create: `meeting-board-app/app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: 可编译的空白 Android 项目（`./gradlew build` 成功）

- [ ] **Step 1: 创建项目根 build.gradle.kts**

文件: `meeting-board-app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
}
```

- [ ] **Step 2: 创建 settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MeetingBoard"
include(":app")
```

- [ ] **Step 3: 创建 app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ruoyi.board"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ruoyi.board"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
```

- [ ] **Step 4: 创建 gradle.properties**

```properties
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m
```

- [ ] **Step 5: 创建 AndroidManifest.xml**

文件: `meeting-board-app/app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".BoardApplication"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.MeetingBoard"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".SetupActivity"
            android:exported="false"
            android:screenOrientation="portrait" />

        <activity
            android:name=".KioskActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:configChanges="orientation|screenSize"
            android:theme="@style/Theme.MeetingBoard.Kiosk">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 6: 验证工程结构**

检查目录结构：

```
meeting-board-app/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       └── AndroidManifest.xml
```

- [ ] **Step 7: Commit**

```bash
git add meeting-board-app/
git commit -m "feat: scaffold Android project skeleton for meeting board app"
```

---

### Task 4: Android 资源文件和主题

**Files:**
- Create: `meeting-board-app/app/src/main/res/values/strings.xml`
- Create: `meeting-board-app/app/src/main/res/values/themes.xml`
- Create: `meeting-board-app/app/src/main/res/layout/activity_kiosk.xml`
- Create: `meeting-board-app/app/src/main/res/layout/activity_setup.xml`

- [ ] **Step 1: 创建 strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">会议门牌</string>
    <string name="select_room">请选择会议室</string>
    <string name="confirm">确认</string>
    <string name="exit_password_title">管理员验证</string>
    <string name="exit_password_hint">请输入管理员密码</string>
</resources>
```

- [ ] **Step 2: 创建 themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Base theme -->
    <style name="Theme.MeetingBoard" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="android:statusBarColor">@android:color/black</item>
        <item name="android:navigationBarColor">@android:color/black</item>
    </style>

    <!-- Kiosk (fullscreen) theme -->
    <style name="Theme.MeetingBoard.Kiosk" parent="Theme.MeetingBoard">
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowNoTitle">true</item>
    </style>
</resources>
```

- [ ] **Step 3: 创建 activity_kiosk.xml (WebView 容器)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:keepScreenOn="true">

    <WebView
        android:id="@+id/kioskWebView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
```

- [ ] **Step 4: 创建 activity_setup.xml (配置页)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="40dp"
    android:background="#1a1a2e">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="会议室配置"
        android:textColor="#ffffff"
        android:textSize="28sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="请选择本平板对应的会议室"
        android:textColor="#9090a8"
        android:textSize="15sp"
        android:layout_marginBottom="24dp" />

    <Spinner
        android:id="@+id/roomSpinner"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="#16213e"
        android:padding="14dp" />

    <Button
        android:id="@+id/btnConfirm"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:text="确认"
        android:textSize="18sp"
        android:textColor="#ffffff"
        android:backgroundTint="#409eff"
        android:layout_marginTop="20dp"
        android:enabled="false" />

</LinearLayout>
```

- [ ] **Step 5: Commit**

```bash
git add meeting-board-app/app/src/main/res/
git commit -m "feat: add Android resources (themes, layouts, strings) for board app"
```

---

### Task 5: Android Kotlin 核心代码

**Files:**
- Create: `meeting-board-app/app/src/main/java/com/ruoyi/board/BoardApplication.kt`
- Create: `meeting-board-app/app/src/main/java/com/ruoyi/board/SetupActivity.kt`
- Create: `meeting-board-app/app/src/main/java/com/ruoyi/board/KioskActivity.kt`
- Create: `meeting-board-app/app/src/main/java/com/ruoyi/board/BootReceiver.kt`
- Create: `meeting-board-app/app/proguard-rules.pro`

**Interfaces:**
- `SetupActivity` → writes `roomId` to `SharedPreferences` → launches `KioskActivity`
- `KioskActivity` → reads `roomId`, loads WebView with `board.html?roomId=N`
- `BootReceiver` → listens `BOOT_COMPLETED` → launches `KioskActivity`

- [ ] **Step 1: 创建 BoardApplication.kt**

```kotlin
package com.ruoyi.board

import android.app.Application

class BoardApplication : Application() {
    companion object {
        const val PREFS_NAME = "meeting_board_prefs"
        const val KEY_ROOM_ID = "room_id"
        const val BOARD_URL = "http://1.94.26.126/board.html"
        const val SERVER_URL = "http://1.94.26.126"
    }
}
```

- [ ] **Step 2: 创建 BootReceiver.kt**

```kotlin
package com.ruoyi.board

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
            val roomId = prefs.getString(BoardApplication.KEY_ROOM_ID, null)

            val launchIntent = if (roomId.isNullOrEmpty()) {
                Intent(context, SetupActivity::class.java)
            } else {
                Intent(context, KioskActivity::class.java)
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
}
```

- [ ] **Step 3: 创建 SetupActivity.kt**

```kotlin
package com.ruoyi.board

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class RoomItem(val roomId: Long, val roomName: String, val location: String) {
    override fun toString(): String = "$roomName ($location)"
}

class SetupActivity : AppCompatActivity() {

    private var rooms = listOf<RoomItem>()
    private var selectedRoomId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val spinner = findViewById<Spinner>(R.id.roomSpinner)
        val btn = findViewById<Button>(R.id.btnConfirm)

        loadRooms(spinner)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRoomId = if (position > 0) rooms.getOrNull(position - 1)?.roomId else null
                btn.isEnabled = selectedRoomId != null
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btn.setOnClickListener {
            selectedRoomId?.let { rid ->
                getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(BoardApplication.KEY_ROOM_ID, rid.toString()).apply()
                startActivity(Intent(this, KioskActivity::class.java))
                finish()
            }
        }
    }

    private fun loadRooms(spinner: Spinner) {
        thread {
            try {
                val url = URL("${BoardApplication.SERVER_URL}/prod-api/meeting/room/simpleList")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.connect()

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(text)
                    val data = json.optJSONArray("data") ?: JSONArray()
                    val list = mutableListOf<RoomItem>()
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        list.add(RoomItem(
                            obj.getLong("roomId"),
                            obj.getString("roomName"),
                            obj.optString("location", "")
                        ))
                    }
                    rooms = list
                    runOnUiThread {
                        val adapter = ArrayAdapter(this@SetupActivity,
                            android.R.layout.simple_spinner_item, rooms)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
```

- [ ] **Step 4: 创建 KioskActivity.kt**

```kotlin
package com.ruoyi.board

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class KioskActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var touchDownTime = 0L
    private val touchDownPoint = FloatArray(2)
    private val handler = Handler(Looper.getMainLooper())
    private var exitRunnable: Runnable? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen + keep screen on
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        setContentView(R.layout.activity_kiosk)

        val roomId = getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(BoardApplication.KEY_ROOM_ID, null)

        if (roomId.isNullOrEmpty()) {
            Toast.makeText(this, "未配置会议室，请重新配置", Toast.LENGTH_LONG).show()
            startActivity(android.content.Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        webView = findViewById(R.id.kioskWebView)
        setupWebView()
        webView.loadUrl("${BoardApplication.BOARD_URL}?roomId=$roomId")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setBackgroundColor(Color.parseColor("#1a1a2e"))

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Disable long-press and text selection
        webView.setOnTouchListener { _, event ->
            // Corner-area long-press detection for exit
            if (event.action == MotionEvent.ACTION_DOWN) {
                touchDownTime = System.currentTimeMillis()
                touchDownPoint[0] = event.x
                touchDownPoint[1] = event.y
                val w = webView.width.toFloat()
                val h = webView.height.toFloat()
                val cornerSize = 80f
                val inCorner = (event.x < cornerSize || event.x > w - cornerSize) &&
                               (event.y < cornerSize || event.y > h - cornerSize)
                if (inCorner) {
                    exitRunnable?.let { handler.removeCallbacks(it) }
                    exitRunnable = Runnable { showExitDialog() }
                    handler.postDelayed(exitRunnable!!, 5000)
                }
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                exitRunnable?.let { handler.removeCallbacks(it) }
            }
            false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?,
                                         error: WebResourceError?) {
                val js = "document.getElementById('errorOverlay').className='error-overlay visible';"
                view?.evaluateJavascript(js, null)
            }
        }
    }

    private fun showExitDialog() {
        val editText = EditText(this).apply {
            hint = "请输入管理员密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("管理员验证")
            .setView(editText)
            .setPositiveButton("确认") { _, _ ->
                if (editText.text.toString() == "admin888") {
                    getSharedPreferences(BoardApplication.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().remove(BoardApplication.KEY_ROOM_ID).apply()
                    startActivity(android.content.Intent(this, SetupActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
```

- [ ] **Step 5: 创建 proguard-rules.pro**

```
# Keep the data class for JSON parsing
-keep class com.ruoyi.board.RoomItem { *; }
```

- [ ] **Step 6: Commit**

```bash
git add meeting-board-app/app/src/main/java/ meeting-board-app/app/proguard-rules.pro
git commit -m "feat: implement KioskActivity, SetupActivity, BootReceiver in Kotlin"
```

---

### Task 6: 编译 APK + 部署门牌页面

**Files:**
- Verify: `ruoyi-ui/public/board.html` → 编译后 → `ruoyi-ui/dist/board.html`

- [ ] **Step 1: 编译前端（含 board.html）**

```bash
cd <项目根目录>/ruoyi-ui && npm run build:prod
```

验证：`ruoyi-ui/dist/board.html` 文件存在。

- [ ] **Step 2: 编译后端（含 simpleList 接口）**

```bash
cd <项目根目录> && mvn clean package -DskipTests -q
```

验证：BUILD SUCCESS

- [ ] **Step 3: 上传部署**

```bash
# 上传前端（含 board.html）
scp -o StrictHostKeyChecking=no -r ruoyi-ui/dist/* root@1.94.26.126:/root/zhihuiyuanqu20260625/dist/
ssh root@1.94.26.126 "cp -r /root/zhihuiyuanqu20260625/dist/* /usr/share/nginx/zhihuiyuanqu/dist/ && nginx -s reload"

# 上传后端 jar + 重启
scp -o StrictHostKeyChecking=no ruoyi-admin/target/ruoyi-admin.jar root@1.94.26.126:/root/zhihuiyuanqu20260625/
ssh root@1.94.26.126 "cd /root/zhihuiyuanqu20260625 && PID=\$(ps aux | grep 'ruoyi-admin.jar' | grep -v grep | awk '{print \$2}') && [ -n \"\$PID\" ] && kill \$PID && sleep 2; nohup java -jar ruoyi-admin.jar --server.port=8090 --spring.devtools.restart.enabled=false > app.log 2>&1 &"
```

- [ ] **Step 4: 验证部署**

```bash
# 验证 simpleList 接口
curl -s http://1.94.26.126:80/prod-api/meeting/room/simpleList | python -m json.tool 2>/dev/null || curl -s http://1.94.26.126:80/prod-api/meeting/room/simpleList

# 验证 board.html 可访问
curl -s -o /dev/null -w "board.html: HTTP %{http_code}\n" http://1.94.26.126:80/board.html

# 验证门牌 API
curl -s http://1.94.26.126:80/prod-api/meeting/board/1 | head -c 200
```

预期：`simpleList` 返回 JSON，`board.html` HTTP 200，门牌 API 正常返回。

- [ ] **Step 5: 构建 Android APK**

在 Android Studio 中打开 `meeting-board-app/` 项目，或命令行：

```bash
cd meeting-board-app && ./gradlew assembleDebug
```

验证：`app/build/outputs/apk/debug/app-debug.apk` 生成，大小约 5MB。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "chore: build, deploy board page + backend, finalize Android APK"
```

---

## 验证清单

- [ ] `http://1.94.26.126:80/board.html?roomId=1` 浏览器中正常显示门牌页面
- [ ] `http://1.94.26.126:80/prod-api/meeting/room/simpleList` 返回会议室列表 JSON
- [ ] Android APK 安装到平板后：首次显示配置页 → 选择会议室 → 确认 → 进入门牌全屏
- [ ] 平板重启后自动进入门牌界面
- [ ] 长按屏幕四角 5 秒 → 密码验证 → 返回配置页
- [ ] 断网后显示「网络连接异常」覆盖层
