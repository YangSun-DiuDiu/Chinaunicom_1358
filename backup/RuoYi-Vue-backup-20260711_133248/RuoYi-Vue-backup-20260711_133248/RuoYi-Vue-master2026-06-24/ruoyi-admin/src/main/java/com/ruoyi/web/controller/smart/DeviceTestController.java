package com.ruoyi.web.controller.smart;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 海康/大华设备SDK加载测试
 */
@RestController
@RequestMapping("/smart/device-test")
public class DeviceTestController extends BaseController
{
    @Autowired private JdbcTemplate jdbc;

    /**
     * 测试设备连通性 (TCP连接 + 设备品牌识别)
     */
    @PostMapping("/connect")
    @PreAuthorize("@ss.hasPermi('smart:personAccess:list')")
    public AjaxResult testConnect(@RequestBody Map<String, Object> params) {
        String ip = (String) params.get("ipAddress");
        String brand = (String) params.get("deviceBrand");
        Integer port = params.get("port") != null ? Integer.parseInt(params.get("port").toString()) : 80;

        if (ip == null || ip.isEmpty()) return error("IP地址不能为空");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ip", ip);
        result.put("port", port);
        result.put("brand", brand);

        long start = System.currentTimeMillis();

        // TCP连通性测试
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 5000);
            long latency = System.currentTimeMillis() - start;
            result.put("tcpConnect", true);
            result.put("latency", latency + "ms");
        } catch (Exception e) {
            result.put("tcpConnect", false);
            result.put("error", e.getMessage());
            return success(result);
        }

        // 品牌识别与SDK适配建议
        if ("HIKVISION".equals(brand)) {
            result.put("sdkType", "海康威视 HCNetSDK / ISAPI");
            result.put("defaultPorts", "HTTP:80, RTSP:554, SDK:8000");
            result.put("sdkStatus", "需要安装海康SDK原生库(HCNetSDK.dll/so)并配置JNA调用");
            result.put("httpApi", "可通过ISAPI REST接口实现部分功能: http://" + ip + "/ISAPI/");
            result.put("suggestion", "推荐使用HTTP/ISAPI接口进行二次开发，无需安装原生SDK");
        } else if ("DAHUA".equals(brand)) {
            result.put("sdkType", "大华 General SDK / HTTP API");
            result.put("defaultPorts", "HTTP:80, RTSP:554, SDK:37777");
            result.put("sdkStatus", "需要安装大华SDK原生库(dhnetsdk.dll/so)并配置JNA调用");
            result.put("httpApi", "可通过HTTP API实现部分功能: http://" + ip + "/cgi-bin/");
            result.put("suggestion", "推荐使用HTTP CGI接口进行二次开发，无需安装原生SDK");
        } else {
            result.put("sdkType", "通用ONVIF/RTSP协议");
            result.put("suggestion", "建议使用ONVIF协议或HTTP API进行集成");
        }

        result.put("testTime", new Date());
        return success(result);
    }

    /**
     * 批量测试所有已配置设备
     */
    @GetMapping("/batch-test")
    @PreAuthorize("@ss.hasPermi('smart:personAccess:list')")
    public AjaxResult batchTest(@RequestParam(defaultValue = "person") String type) {
        String table = "person".equals(type) ? "iot_person_access_device" : "iot_vehicle_access_device";
        List<Map<String, Object>> devices = jdbc.queryForList("select * from " + table);
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> dev : devices) {
            String ip = (String) dev.get("ip_address");
            Integer port = dev.get("port") != null ? Integer.parseInt(dev.get("port").toString()) : 80;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("device_id", dev.get("device_id"));
            r.put("device_name", dev.get("device_name"));
            r.put("brand", dev.get("device_brand"));
            r.put("ip", ip);

            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(ip, port), 3000);
                r.put("status", "ONLINE");
                r.put("latency", "reachable");
            } catch (Exception e) {
                r.put("status", "OFFLINE");
                r.put("error", e.getMessage());
            }
            results.add(r);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        long online = results.stream().filter(r -> "ONLINE".equals(r.get("status"))).count();
        summary.put("total", results.size());
        summary.put("online", online);
        summary.put("offline", results.size() - online);
        summary.put("devices", results);
        return success(summary);
    }
}
