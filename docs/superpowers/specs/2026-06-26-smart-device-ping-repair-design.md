# 智能设备一键检测 + 报修 — 设计方案

> **日期**: 2026-06-26
> **状态**: 已审核

---

## 1. 目标

在人员通行设备、车辆通行设备、视频监控设备三个页面，新增"一键检测"按钮和离线"报修"按钮。

---

## 2. 后端改动

### 2.1 批量Ping端点

在 `SmartAccessController` 新增：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/smart/access/person-device/ping-all` | POST | Ping所有人员通行设备 |
| `/smart/access/vehicle-device/ping-all` | POST | Ping所有车辆通行设备 |

**逻辑**：遍历表中所有有 `ip_address` 的设备 → `InetAddress.isReachable(3000)` → 更新 `status` 列 → 返回 `{total, online, offline}`。

注：`/smart/access/video-device/ping-all` 已存在，无需新增。

### 2.2 智能设备报修端点

在 `SmartAccessController` 新增：

```
POST /smart/access/device-repair/{table}/{deviceId}
Body: { "phone": "138xxxx" }
```

**逻辑**：
1. `table` 参数：`person` / `vehicle` / `video`（映射到对应表名）
2. 查对应表获取 `device_name`、`ip_address`
3. 创建 `DeviceRepair` 工单（`completeToken` + `repairNo`，createBy=当前用户）
4. 发送短信：`设备离线告警，设备：{name}，已离线，请及时处理。设备登录码：{token}`
5. 返回成功

---

## 3. 前端改动

### 3.1 一键检测按钮

三个页面各加一个按钮在顶部工具栏：

```html
<el-button type="warning" icon="el-icon-video-play" @click="handlePingAll">一键检测</el-button>
```

**方法**：
```javascript
handlePingAll() {
  this.$modal.loading('正在检测设备...')
  pingAllApi().then(res => {
    this.$modal.closeLoading()
    this.$modal.msgSuccess(`检测完成：${res.data.online}台在线，${res.data.offline}台离线`)
    this.getList()
  })
}
```

| 页面 | pingAllApi |
|------|-----------|
| 人员通行设备 | `POST /smart/access/person-device/ping-all` |
| 车辆通行设备 | `POST /smart/access/vehicle-device/ping-all` |
| 视频监控设备 | `POST /smart/access/video-device/ping-all`（已存在） |

### 3.2 报修按钮

操作列中，当设备 `status === 'OFFLINE'` 时显示：

```html
<el-button v-if="scope.row.status === 'OFFLINE'" type="warning" size="mini" icon="el-icon-warning" @click="handleRepair(scope.row)">报修</el-button>
```

**弹窗**：
```html
<el-dialog title="设备报修" :visible.sync="repairOpen" width="400px">
  <el-form>
    <el-form-item label="设备名称">{{ repairDevice.deviceName }}</el-form-item>
    <el-form-item label="维修人手机号" required>
      <el-input v-model="repairPhone" placeholder="请输入手机号" />
    </el-form-item>
  </el-form>
  <div slot="footer">
    <el-button @click="repairOpen=false">取消</el-button>
    <el-button type="primary" :disabled="!repairPhone" @click="submitRepair">确认报修</el-button>
  </div>
</el-dialog>
```

**方法**：
```javascript
handleRepair(row) {
  this.repairDevice = row
  this.repairPhone = ''
  this.repairOpen = true
},
submitRepair() {
  repairApi(this.repairDevice.deviceId, this.repairPhone).then(() => {
    this.$modal.msgSuccess('报修工单已创建，短信已发送')
    this.repairOpen = false
  })
}
```

---

## 4. 不做的事

- 不修改已有 `/video-device/ping-all` 的逻辑
- 不修改 `DeviceHeartbeatTask` 定时任务
- 不在智能设备表中新增字段
- 不修改短信模板格式

---

## 5. 验证标准

- [ ] 三个页面"一键检测"按钮正常触发Ping
- [ ] 检测结果正确返回在线/离线数量
- [ ] 离线设备显示"报修"按钮
- [ ] 报修弹窗可输入手机号
- [ ] 提交后创建维修工单 + 发送短信
- [ ] 视频监控设备页面复用已有 `/ping-all` 不重复开发
