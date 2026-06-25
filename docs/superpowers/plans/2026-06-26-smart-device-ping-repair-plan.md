# 智能设备一键检测 + 报修 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task.

**Goal:** 在人员通行/车辆通行/视频监控三个设备页面添加一键检测(批量ping)+离线设备报修按钮

**Architecture:** 后端在 SmartAccessController 新增 2 个 ping-all 端点 + 1 个通用报修端点；前端三个页面各加检测按钮和报修弹窗

**Tech Stack:** Java 17, SpringBoot 4.0.3, Vue 2.6, Element UI 2.15, JdbcTemplate

## Global Constraints

- 视频监控 `/video-device/ping-all` 已存在，复用而非重复开发
- 报修短信模板与现有一致：`设备离线告警，设备：{name}，已离线，请及时处理。设备登录码：{token}`
- 智能设备表不新增字段
- 不修改 DeviceHeartbeatTask

---

### Task 1: 后端 — 新增 person-device 和 vehicle-device 的 ping-all 端点

**Files:**
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/smart/SmartAccessController.java`

**Interfaces:**
- Consumes: `JdbcTemplate` (已注入), `InetAddress`
- Produces: `POST /smart/access/person-device/ping-all` → `{total, online, offline}`, `POST /smart/access/vehicle-device/ping-all` → `{total, online, offline}`

- [ ] **Step 1: 添加 person-device ping-all 方法**

在 `SmartAccessController` 中添加：

```java
@PostMapping("/person-device/ping-all")
public AjaxResult pingAllPersonDevices() {
    return pingAllFromTable("iot_person_access_device");
}

@PostMapping("/vehicle-device/ping-all")
public AjaxResult pingAllVehicleDevices() {
    return pingAllFromTable("iot_vehicle_access_device");
}

/** 批量Ping指定表中的所有设备 */
private AjaxResult pingAllFromTable(String table) {
    List<Map<String, Object>> devices = jdbc.queryForList(
        "SELECT device_id, device_name, ip_address FROM " + table + " WHERE ip_address IS NOT NULL AND ip_address != ''");
    int total = 0, online = 0, offline = 0;
    for (Map<String, Object> row : devices) {
        Long id = ((Number) row.get("device_id")).longValue();
        String ip = String.valueOf(row.get("ip_address"));
        total++;
        try {
            boolean reachable = java.net.InetAddress.getByName(ip).isReachable(3000);
            String newStatus = reachable ? "ONLINE" : "OFFLINE";
            if (reachable) online++; else offline++;
            jdbc.update("UPDATE " + table + " SET status=? WHERE device_id=?", newStatus, id);
        } catch (Exception e) {
            offline++;
            jdbc.update("UPDATE " + table + " SET status='OFFLINE' WHERE device_id=?", id);
        }
    }
    AjaxResult result = AjaxResult.success();
    result.put("total", total);
    result.put("online", online);
    result.put("offline", offline);
    return result;
}
```

需要添加的 import：
```java
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl ruoyi-admin -am -DskipTests
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: add ping-all endpoints for person-device and vehicle-device"
```

---

### Task 2: 后端 — 新增智能设备通用报修端点

**Files:**
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/smart/SmartAccessController.java`

**Interfaces:**
- Consumes: `JdbcTemplate`, `ISmsService` (新增注入), `DeviceRepair`
- Produces: `POST /smart/access/device-repair/{table}/{deviceId}` body: `{phone}`

- [ ] **Step 1: 注入 ISmsService 并添加报修方法**

在 `SmartAccessController` 类中添加字段：
```java
@Autowired
private com.ruoyi.system.service.ISmsService smsService;
```

添加方法：
```java
@PostMapping("/device-repair/{table}/{deviceId}")
public AjaxResult repairSmartDevice(@PathVariable String table, @PathVariable Long deviceId,
                                     @RequestBody Map<String, String> body) {
    String phone = body.get("phone");
    if (phone == null || phone.isEmpty()) return error("手机号不能为空");

    // 映射 table 参数到实际表名
    String actualTable;
    switch (table) {
        case "person": actualTable = "iot_person_access_device"; break;
        case "vehicle": actualTable = "iot_vehicle_access_device"; break;
        case "video": actualTable = "iot_video_device"; break;
        default: return error("无效的设备类型: " + table);
    }

    Map<String, Object> dev = null;
    try { dev = jdbc.queryForMap("SELECT device_name, ip_address FROM " + actualTable + " WHERE device_id=?", deviceId); }
    catch (Exception e) { return error("设备不存在"); }

    String deviceName = String.valueOf(dev.getOrDefault("device_name", "未知设备"));
    String ipAddress = String.valueOf(dev.getOrDefault("ip_address", ""));

    // 创建维修工单
    com.ruoyi.system.domain.DeviceRepair repair = new com.ruoyi.system.domain.DeviceRepair();
    repair.setDeviceId(deviceId);
    repair.setDeviceName(deviceName);
    repair.setDeviceIp(ipAddress);
    repair.setFaultDescription("智能设备离线故障，请及时维修");
    repair.setStatus("PENDING");
    repair.setCreateBy(getUsername());
    repair.setCreateTime(new java.util.Date());
    repair.setOriginalResponsible(getUsername());
    repair.setOriginalPhone(phone);
    repair.setCurrentResponsible(getUsername());
    repair.setCurrentPhone(phone);
    repair.setCompleteToken(java.util.UUID.randomUUID().toString().replace("-", ""));
    repair.setRepairNo(generateSmartRepairNo());
    jdbc.update("INSERT INTO iot_device_repair (device_id,device_name,device_ip,fault_description," +
        "status,create_by,create_time,original_responsible,original_phone,current_responsible,current_phone," +
        "complete_token,repair_no) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
        repair.getDeviceId(), repair.getDeviceName(), repair.getDeviceIp(), repair.getFaultDescription(),
        repair.getStatus(), repair.getCreateBy(), repair.getCreateTime(), repair.getOriginalResponsible(),
        repair.getOriginalPhone(), repair.getCurrentResponsible(), repair.getCurrentPhone(),
        repair.getCompleteToken(), repair.getRepairNo());

    // 发送短信
    smsService.sendSms(repair.getCurrentResponsible(), phone,
        "设备离线告警，设备：" + deviceName + "，已离线，请及时处理。设备登录码：" + repair.getCompleteToken(),
        "REPAIR", deviceId);

    return success(repair.getCompleteToken());
}

private String generateSmartRepairNo() {
    String today = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM iot_device_repair WHERE DATE(create_time)=CURDATE()", Integer.class);
    int seq = (count != null ? count : 0) + 1;
    return today + String.format("%03d", seq);
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl ruoyi-admin -am -DskipTests
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: add smart device repair endpoint with SMS"
```

---

### Task 3: 前端 — 三个页面添加一键检测 + 报修按钮

**Files:**
- Modify: `ruoyi-ui/src/views/smart/person-access/device/index.vue`
- Modify: `ruoyi-ui/src/views/smart/vehicle-access/device/index.vue`
- Modify: `ruoyi-ui/src/views/smart/video/device/index.vue`

**Interfaces:**
- Consumes: 后端 Task 1+2 的新端点
- Produces: 一键检测按钮 + 离线报修按钮 + 报修弹窗

- [ ] **Step 1: 人员通行设备页面**

在 toolbar 区添加一键检测按钮：
```html
<el-button type="warning" icon="el-icon-video-play" @click="handlePingAll">一键检测</el-button>
```

在操作列中（`<el-table-column label="操作">` 内），状态标签后添加：
```html
<el-button v-if="scope.row.status === 'OFFLINE'" type="warning" size="mini" icon="el-icon-warning" @click="handleRepair(scope.row)">报修</el-button>
```

在 `</template>` 之前添加报修弹窗：
```html
<el-dialog title="设备报修" :visible.sync="repairOpen" width="400px">
  <el-form label-width="100px">
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

在 `data()` 中添加：
```javascript
repairOpen: false, repairDevice: {}, repairPhone: ''
```

在 `methods` 中添加：
```javascript
handlePingAll() {
  this.$modal.loading('正在检测设备...')
  request({ url: '/smart/access/person-device/ping-all', method: 'post' }).then(res => {
    this.$modal.closeLoading()
    this.$modal.msgSuccess(`检测完成：${res.data.online}台在线，${res.data.offline}台离线`)
    this.getList()
  }).catch(() => { this.$modal.closeLoading() })
},
handleRepair(row) {
  this.repairDevice = row; this.repairPhone = ''; this.repairOpen = true
},
submitRepair() {
  request({ url: '/smart/access/device-repair/person/' + this.repairDevice.deviceId, method: 'post', data: { phone: this.repairPhone } }).then(() => {
    this.$modal.msgSuccess('报修工单已创建，短信已发送')
    this.repairOpen = false; this.getList()
  })
}
```

- [ ] **Step 2: 车辆通行设备页面 — 同上改动**

将 `person-device` 替换为 `vehicle-device`，`person` 替换为 `vehicle`。

- [ ] **Step 3: 视频监控设备页面 — 同上改动**

将 ping-all URL 改为已有的 `/smart/access/video-device/ping-all`，报修 URL 使用 `video`。

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: add one-click ping and repair button to smart device pages"
```

---

### Task 4: 构建部署验证

**Files:** (none new)

- [ ] **Step 1: 构建**
```bash
cd <项目根> && mvn clean package -DskipTests -q && cd ruoyi-ui && npm run build:prod
```

- [ ] **Step 2: 上传部署**
```bash
scp ruoyi-admin/target/ruoyi-admin.jar root@1.94.26.126:/root/zhihuiyuanqu20260625/
scp -r ruoyi-ui/dist/* root@1.94.26.126:/usr/share/nginx/zhihuiyuanqu/dist/
ssh root@1.94.26.126 'cd /root/zhihuiyuanqu20260625 && bash stop-server.sh && bash start-server.sh && nginx -s reload'
sleep 15 && curl -s -o /dev/null -w "HTTP %{http_code}\n" http://1.94.26.126:8090/
```

- [ ] **Step 3: 验证**
```bash
# 测试 ping-all
TOKEN=$(curl -s http://1.94.26.126:8090/login ... | sed 's/.*"token":"//;s/".*//')
curl -s -X POST "http://1.94.26.126:8090/smart/access/person-device/ping-all" -H "Authorization: Bearer $TOKEN"

# 测试报修
curl -s -X POST "http://1.94.26.126:8090/smart/access/device-repair/person/1" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"phone":"13800000001"}'
```

- [ ] **Step 4: Commit**
```bash
git add -A && git commit -m "chore: deploy smart device ping-repair feature" && git push origin master
```
