# 上传路径修复 + 短信内容改造 + 维修页面重构 — 设计方案

> **日期**: 2026-06-25
> **状态**: 已审核

---

## 1. 改动一：修复文件上传路径

**问题**: `application.yml` 中 `ruoyi.profile: D:/ruoyi/uploadPath` 是 Windows 路径，服务器上不存在

**修复**:
- `application.yml` → `profile: /home/ruoyi/uploadPath`
- 服务器上创建目录: `mkdir -p /home/ruoyi/uploadPath/upload/repair`
- 重新编译部署后生效

---

## 2. 改动二：短信内容改造

### 2.1 设备离线/维修短信

**改造前**:
```
【设备维修】设备「xxx」（IP:xxx）离线故障，维修完成后请点击: http://1.94.26.126/repair-complete?token=UUID
```

**改造后**:
```
设备离线告警，设备：${device_name}，已离线，请及时处理。设备登录码：${token}
```

参数:
- `device_name`: 设备名称
- `token`: 维修工单的 `completeToken`（32位UUID）

影响方法:
- `SmsServiceImpl.sendRepairAlert()` — 离线设备短信
- `DeviceRepairController.createRepair()` — 创建工单时短信

### 2.2 转派短信 (保持不变)

转派短信内容不变，但 token 仍包含在 URL 中供维修人员使用。

---

## 3. 改动三：维修反馈页面重构

### 3.1 交互流程

```
维修人员收到短信
  → 手动打开 http://1.94.26.126/repair-complete
  → 页面显示：
      ┌─────────────────────────────┐
      │  🔧 设备维修确认             │
      │                             │
      │  ┌───────────────────────┐  │
      │  │ 请输入设备登录码       │  │
      │  └───────────────────────┘  │
      │  [ 提 交 ]                  │
      └─────────────────────────────┘
  → 输入token → 提交
  → 跳转 http://1.94.26.126/repair-complete?token={输入的token}
  → 加载工单详情 + 维修表单（保持现有逻辑）
```

### 3.2 页面两种模式

根据 URL 参数区分：

**模式A — 无 token 参数** (`/repair-complete`)：
- 显示 token 输入框 + 提交按钮
- 用户输入 token → `router.replace(/repair-complete?token=${input})`

**模式B — 有 token 参数** (`/repair-complete?token=xxx`)：
- 保持现有逻辑不变：加载工单详情、显示维修表单、上传照片、提交完成

### 3.3 具体改动

| 文件 | 改动 |
|------|------|
| `ruoyi-ui/src/views/device/repair/complete.vue` | template 新增 token 输入模式；script 新增 `tokenInput` 数据 + `submitToken()` 方法 + 根据 `$route.query.token` 切换模式 |
| `ruoyi-system/.../SmsServiceImpl.java` | `sendRepairAlert()` 短信内容改为新模板；`sendDeviceOfflineAlert()` 同样使用新模板 |
| `ruoyi-admin/.../DeviceRepairController.java` | `createRepair()` 短信内容改为新模板 |
| `ruoyi-admin/.../application.yml` | `profile: /home/ruoyi/uploadPath` |

---

## 4. 不做的事

- 不修改转派短信的 URL 逻辑（转派仍带 URL）
- 不修改后端 API 签名
- 不修改 token 生成逻辑
- 不新增数据库表

---

## 5. 验证标准

- [ ] 上传路径改为 Linux 路径，服务器目录已创建
- [ ] 维修工单创建时 SMS 内容为新格式（含 device_name + token）
- [ ] `/repair-complete` 无 token 时显示输入框
- [ ] 输入 token 提交后正确跳转到详情页
- [ ] 已有 `?token=xxx` 链接仍可正常工作
- [ ] 上传照片功能恢复正常
