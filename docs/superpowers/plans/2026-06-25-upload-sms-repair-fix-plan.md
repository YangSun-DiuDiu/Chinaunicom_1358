# 上传路径修复 + 短信改造 + 维修页面重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复上传路径为 Linux 路径、改造短信内容为新模板、重构维修页面为 token 输入模式

**Architecture:** 后端修改配置路径 + 短信模板内容；前端 complete.vue 新增 token 输入模式（URL 无 token 参数时显示输入框，有 token 参数时保持现有详情页逻辑）

**Tech Stack:** Java 17, SpringBoot 4.0.3, Vue 2.6, Element UI 2.15

## Global Constraints

- 不修改后端 API 签名
- 不修改 token 生成逻辑（`UUID.randomUUID()`）
- 不新增数据库表
- 已有 `?token=xxx` 直接访问链接仍可正常工作
- 上传目录 `/home/ruoyi/uploadPath` 需在服务器上存在

---

### Task 1: 修复上传路径 + 创建服务器目录

**Files:**
- Modify: `ruoyi-admin/src/main/resources/application.yml:10`

**Interfaces:**
- Consumes: nothing
- Produces: `profile: /home/ruoyi/uploadPath` in application.yml

- [ ] **Step 1: 修改 application.yml**

将 line 10 的 `profile: D:/ruoyi/uploadPath` 改为 `profile: /home/ruoyi/uploadPath`

```yaml
  profile: /home/ruoyi/uploadPath
```

- [ ] **Step 2: 服务器创建目录**

```bash
ssh root@1.94.26.126 'mkdir -p /home/ruoyi/uploadPath/upload/repair && ls -la /home/ruoyi/uploadPath/'
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix: change upload profile path from Windows to Linux"
```

---

### Task 2: 改造短信内容模板

**Files:**
- Modify: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SmsServiceImpl.java:112-119` (sendRepairAlert)
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/device/DeviceRepairController.java:135-139` (createRepair SMS)

**Interfaces:**
- Consumes: Device.getDeviceName(), completeToken (already exists)
- Produces: new SMS content format

- [ ] **Step 1: 修改 SmsServiceImpl.sendRepairAlert()**

将 lines 138-145 的方法体改为：

```java
    @Override
    public void sendRepairAlert(Device device)
    {
        // 此方法不再直接发送维修短信 — 改由 DeviceRepairController.createRepair 统一发送
        // 保留方法以防其他调用方
        String content = "设备离线告警，设备：" + safeStr(device.getDeviceName())
                + "，已离线，请及时处理。";
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                BIZ_TYPE_REPAIR, device.getDeviceId());
    }
```

- [ ] **Step 2: 修改 DeviceRepairController.createRepair() 中的短信内容**

将 lines 136-139 改为：

```java
        String repairToken = repair.getCompleteToken();
        smsService.sendSms(device.getResponsible(), device.getResponsiblePhone(),
            "设备离线告警，设备：" + device.getDeviceName()
            + "，已离线，请及时处理。设备登录码：" + repairToken,
            "REPAIR", repair.getRepairId());
```

同时删除不再需要的 `getRepairCallbackUrl()` 调用（line 135 的 callbackUrl）。

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl ruoyi-system,ruoyi-admin -am -DskipTests
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: change SMS content to new template with device_name and token"
```

---

### Task 3: 重构维修反馈页面 (complete.vue)

**Files:**
- Modify: `ruoyi-ui/src/views/device/repair/complete.vue`

**Interfaces:**
- Consumes: `$route.query.token` (Vue Router)
- Produces: token entry form + existing repair detail form

- [ ] **Step 1: 新增 token 输入模式 template**

在现有 `<template>` 的 `<div class="repair-complete-page">` 内，最顶部新增 token 输入卡片（在现有加载中/错误/已完成/工单详情逻辑之前）：

```vue
<template>
  <div class="repair-complete-page">
    <!-- Token 输入模式 -->
    <div class="card" v-if="!token">
      <div class="header-icon">🔑</div>
      <h2>设备维修确认</h2>
      <p style="color:#666;text-align:center;margin-bottom:16px">请输入短信中的设备登录码</p>
      <el-input v-model="tokenInput" placeholder="请输入设备登录码" clearable size="medium" style="margin-bottom:12px" />
      <el-button type="primary" size="medium" :disabled="!tokenInput" @click="submitToken" style="width:100%">提 交</el-button>
    </div>

    <!-- 原有逻辑: 有 token 参数时加载工单 -->
    <div v-else>
      <!-- 加载中 -->
      ...
    </div>
  </div>
</template>
```

- [ ] **Step 2: 新增 data 和 computed**

在 `<script>` 中：

```javascript
data() {
  return {
    token: this.$route.query.token || '',
    tokenInput: '',
    // ... 保留现有 data
  }
},
watch: {
  '$route.query.token'(val) { if (val) { this.token = val; this.loadRepair(); } }
},
methods: {
  submitToken() {
    if (this.tokenInput) {
      this.$router.replace({ path: '/repair-complete', query: { token: this.tokenInput } })
    }
  },
  // ... 保留现有 methods (loadRepair, uploadPhoto, submitComplete 等)
}
```

- [ ] **Step 3: 确保原有逻辑仅在 token 存在时执行**

`mounted()` 改为：
```javascript
mounted() {
  if (this.token) { this.loadRepair() }
}
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: refactor repair-complete page with token input mode"
```

---

### Task 4: 重新构建部署并验证

**Files:** (none new)

- [ ] **Step 1: 构建**

```bash
cd <项目根目录>
mvn clean package -DskipTests -q
cd ruoyi-ui && npm run build:prod
```

- [ ] **Step 2: 上传部署**

```bash
scp ruoyi-admin/target/ruoyi-admin.jar root@1.94.26.126:/root/zhihuiyuanqu20260625/
scp -r ruoyi-ui/dist/* root@1.94.26.126:/usr/share/nginx/zhihuiyuanqu/dist/
ssh root@1.94.26.126 'cd /root/zhihuiyuanqu20260625 && bash stop-server.sh && bash start-server.sh && nginx -s reload'
```

- [ ] **Step 3: 验证**

```bash
# 1. 后端启动
curl -s -o /dev/null -w "HTTP %{http_code}" http://1.94.26.126:8090/
# Expected: HTTP 200

# 2. 前端页面 (无 token — 输入模式)
curl -s -o /dev/null -w "HTTP %{http_code}" http://1.94.26.126:80/repair-complete
# Expected: HTTP 200

# 3. 前端页面 (有 token — 详情模式)
TOKEN=$(mysql -h 1.94.26.126 ... -N -e "SELECT complete_token FROM iot_device_repair LIMIT 1")
curl -s -o /dev/null -w "HTTP %{http_code}" "http://1.94.26.126:80/repair-complete?token=$TOKEN"
# Expected: HTTP 200

# 4. 创建维修工单 → 确认 SMS 内容为新格式
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "chore: deploy upload-sms-repair fixes — all verified"
```
