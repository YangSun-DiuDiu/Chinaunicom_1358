package com.ruoyi.web.controller.smart;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;

/**
 * 智能化管理 - 人员/车辆通行设备与权限管理通用控制器
 */
@RestController
@RequestMapping("/smart/access")
public class SmartAccessController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbc;

    // ==================== 通用 CRUD ====================
    private TableDataInfo listTable(String table, String where, Object... params) {
        startPage();
        String sql = "select * from " + table + " where 1=1 " + where + " order by create_time desc";
        List<java.util.Map<String,Object>> list = params.length > 0 ? jdbc.queryForList(sql, params) : jdbc.queryForList(sql);
        return getDataTable(list);
    }

    private AjaxResult getOne(String table, String idCol, Long id) {
        return success(jdbc.queryForMap("select * from " + table + " where " + idCol + " = ?", id));
    }

    private AjaxResult save(String table, java.util.Map<String, Object> data, String idCol) {
        if (data.containsKey(idCol) && data.get(idCol) != null && StringUtils.isNotEmpty(data.get(idCol).toString())) {
            // update
            StringBuilder sb = new StringBuilder("update " + table + " set ");
            java.util.List<Object> vals = new java.util.ArrayList<>();
            data.forEach((k, v) -> { if (!idCol.equals(k)) { sb.append(k).append("=?, "); vals.add(v); } });
            sb.setLength(sb.length() - 2);
            sb.append(" where ").append(idCol).append("=?");
            vals.add(data.get(idCol));
            jdbc.update(sb.toString(), vals.toArray());
        } else {
            // insert
            StringBuilder cols = new StringBuilder(), phs = new StringBuilder();
            java.util.List<Object> vals = new java.util.ArrayList<>();
            data.forEach((k, v) -> { if (!idCol.equals(k)) { cols.append(k).append(","); phs.append("?,"); vals.add(v); } });
            cols.setLength(cols.length() - 1); phs.setLength(phs.length() - 1);
            jdbc.update("insert into " + table + "(" + cols + ") values(" + phs + ")", vals.toArray());
        }
        return success();
    }

    private AjaxResult delete(String table, String idCol, Long id) {
        jdbc.update("delete from " + table + " where " + idCol + " = ?", id);
        return success();
    }

    // ==================== 人员通行设备 ====================
    @GetMapping("/person-device/list")
    @PreAuthorize("@ss.hasPermi('smart:personAccess:list')")
    public TableDataInfo pdList(@RequestParam(required = false) String deviceName,
                                 @RequestParam(required = false) String deviceBrand) {
        String where = "";
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (StringUtils.isNotEmpty(deviceName)) { where += "and device_name like ? "; params.add("%" + deviceName + "%"); }
        if (StringUtils.isNotEmpty(deviceBrand)) { where += "and device_brand = ? "; params.add(deviceBrand); }
        return listTable("iot_person_access_device", where, params.toArray());
    }

    @GetMapping("/person-device/{id}")
    @PreAuthorize("@ss.hasPermi('smart:personAccess:query')")
    public AjaxResult pdGet(@PathVariable Long id) { return getOne("iot_person_access_device", "device_id", id); }

    @PostMapping("/person-device")
    @PreAuthorize("@ss.hasPermi('smart:personAccess:add')")
    public AjaxResult pdAdd(@RequestBody java.util.Map<String, Object> data) {
        data.put("create_time", new java.util.Date());
        return save("iot_person_access_device", data, "device_id");
    }

    @PutMapping("/person-device")
    @PreAuthorize("@ss.hasPermi('smart:personAccess:edit')")
    public AjaxResult pdEdit(@RequestBody java.util.Map<String, Object> data) { return save("iot_person_access_device", data, "device_id"); }

    @DeleteMapping("/person-device/{id}")
    @PreAuthorize("@ss.hasPermi('smart:personAccess:remove')")
    public AjaxResult pdDel(@PathVariable Long id) { return delete("iot_person_access_device", "device_id", id); }

    // ==================== 车辆通行设备 (同样模式) ====================
    @GetMapping("/vehicle-device/list")
    @PreAuthorize("@ss.hasPermi('smart:vehicleAccess:list')")
    public TableDataInfo vdList(@RequestParam(required = false) String deviceName,
                                 @RequestParam(required = false) String deviceBrand) {
        String where = "";
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (StringUtils.isNotEmpty(deviceName)) { where += "and device_name like ? "; params.add("%" + deviceName + "%"); }
        if (StringUtils.isNotEmpty(deviceBrand)) { where += "and device_brand = ? "; params.add(deviceBrand); }
        return listTable("iot_vehicle_access_device", where, params.toArray());
    }

    @GetMapping("/vehicle-device/{id}")
    @PreAuthorize("@ss.hasPermi('smart:vehicleAccess:query')")
    public AjaxResult vdGet(@PathVariable Long id) { return getOne("iot_vehicle_access_device", "device_id", id); }

    @PostMapping("/vehicle-device")
    @PreAuthorize("@ss.hasPermi('smart:vehicleAccess:add')")
    public AjaxResult vdAdd(@RequestBody java.util.Map<String, Object> data) {
        data.put("create_time", new java.util.Date());
        return save("iot_vehicle_access_device", data, "device_id");
    }

    @PutMapping("/vehicle-device")
    @PreAuthorize("@ss.hasPermi('smart:vehicleAccess:edit')")
    public AjaxResult vdEdit(@RequestBody java.util.Map<String, Object> data) { return save("iot_vehicle_access_device", data, "device_id"); }

    @DeleteMapping("/vehicle-device/{id}")
    @PreAuthorize("@ss.hasPermi('smart:vehicleAccess:remove')")
    public AjaxResult vdDel(@PathVariable Long id) { return delete("iot_vehicle_access_device", "device_id", id); }

    // ==================== 视频设备 ====================
    @GetMapping("/video-device/list")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:list')")
    public TableDataInfo viList(@RequestParam(required = false) String deviceName) {
        String where = "";
        if (StringUtils.isNotEmpty(deviceName)) where += "and device_name like ? ";
        return listTable("iot_video_device", where, StringUtils.isNotEmpty(deviceName) ? new Object[]{"%" + deviceName + "%"} : new Object[]{});
    }

    @GetMapping("/video-device/{id}")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:query')")
    public AjaxResult viGet(@PathVariable Long id) { return getOne("iot_video_device", "device_id", id); }

    @PostMapping("/video-device")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:add')")
    public AjaxResult viAdd(@RequestBody java.util.Map<String, Object> data) {
        data.put("create_time", new java.util.Date());
        return save("iot_video_device", data, "device_id");
    }

    @PutMapping("/video-device")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:edit')")
    public AjaxResult viEdit(@RequestBody java.util.Map<String, Object> data) { return save("iot_video_device", data, "device_id"); }

    @DeleteMapping("/video-device/{id}")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:remove')")
    public AjaxResult viDel(@PathVariable Long id) { return delete("iot_video_device", "device_id", id); }

    // ==================== 地图 ====================
    @GetMapping("/map/list")
    @PreAuthorize("@ss.hasPermi('smart:videoMap:list')")
    public TableDataInfo mapList() { return listTable("iot_map_base", "", new Object[]{}); }

    @PostMapping("/map")
    @PreAuthorize("@ss.hasPermi('smart:videoMap:add')")
    public AjaxResult mapAdd(@RequestBody java.util.Map<String, Object> data) {
        data.put("create_time", new java.util.Date());
        return save("iot_map_base", data, "map_id");
    }

    @PutMapping("/map")
    @PreAuthorize("@ss.hasPermi('smart:videoMap:edit')")
    public AjaxResult mapEdit(@RequestBody java.util.Map<String, Object> data) { return save("iot_map_base", data, "map_id"); }

    @DeleteMapping("/map/{id}")
    @PreAuthorize("@ss.hasPermi('smart:videoMap:remove')")
    public AjaxResult mapDel(@PathVariable Long id) { return delete("iot_map_base", "map_id", id); }

    @PutMapping("/video-device/position")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:edit')")
    public AjaxResult updatePosition(@RequestBody java.util.Map<String, Object> data) {
        jdbc.update("update iot_video_device set map_x=?, map_y=? where device_id=?",
            data.get("mapX"), data.get("mapY"), data.get("deviceId"));
        return success();
    }

    /** 视频设备在线检测(Ping) */
    @PostMapping("/video-device/ping/{id}")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:list')")
    public AjaxResult pingDevice(@PathVariable Long id) {
        java.util.Map<String, Object> dev = jdbc.queryForMap("select * from iot_video_device where device_id=?", id);
        if (dev == null || dev.isEmpty()) return error("设备不存在");
        String ip = (String) dev.get("ip_address");
        if (ip == null || ip.isEmpty()) return error("设备未配置IP地址");

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("deviceId", id);
        result.put("deviceName", dev.get("device_name"));
        result.put("ip", ip);

        long start = System.currentTimeMillis();
        try {
            java.net.InetAddress inet = java.net.InetAddress.getByName(ip);
            boolean reachable = inet.isReachable(3000);
            long latency = System.currentTimeMillis() - start;
            String status = reachable ? "ONLINE" : "OFFLINE";
            result.put("reachable", reachable);
            result.put("status", status);
            result.put("latency", latency + "ms");
            // 更新状态
            jdbc.update("update iot_video_device set status=? where device_id=?", status, id);
        } catch (Exception e) {
            result.put("reachable", false);
            result.put("status", "OFFLINE");
            result.put("error", e.getMessage());
            jdbc.update("update iot_video_device set status='OFFLINE' where device_id=?", id);
        }
        return success(result);
    }

    /** 批量检测所有视频设备 */
    @PostMapping("/video-device/ping-all")
    @PreAuthorize("@ss.hasPermi('smart:videoDevice:list')")
    public AjaxResult pingAll() {
        java.util.List<java.util.Map<String, Object>> devices = jdbc.queryForList("select * from iot_video_device");
        int online = 0, offline = 0;
        for (java.util.Map<String, Object> dev : devices) {
            String ip = (String) dev.get("ip_address");
            if (ip != null && !ip.isEmpty()) {
                try {
                    boolean r = java.net.InetAddress.getByName(ip).isReachable(3000);
                    String s = r ? "ONLINE" : "OFFLINE";
                    jdbc.update("update iot_video_device set status=? where device_id=?", s, dev.get("device_id"));
                    if (r) online++; else offline++;
                } catch (Exception e) { offline++; }
            }
        }
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("total", devices.size()); r.put("online", online); r.put("offline", offline);
        return success(r);
    }

    // ==================== 批量Ping ====================
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
}
