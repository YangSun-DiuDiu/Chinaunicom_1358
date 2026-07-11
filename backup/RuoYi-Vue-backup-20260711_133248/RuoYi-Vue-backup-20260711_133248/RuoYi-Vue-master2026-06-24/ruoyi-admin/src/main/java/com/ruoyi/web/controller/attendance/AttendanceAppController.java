package com.ruoyi.web.controller.attendance;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 考勤应用条目管理 — 创建钉钉/企业微信应用，生成Token/AESKey，管理回调地址
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/attendance/app")
public class AttendanceAppController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AttendanceAppController.class);
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 应用条目列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public TableDataInfo list(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String status)
    {
        startPage();
        String sql = "SELECT * FROM attendance_app WHERE 1=1 ";
        List<Object> params = new ArrayList<>();
        if (platform != null && !platform.isEmpty()) { sql += "AND platform=? "; params.add(platform); }
        if (status != null && !status.isEmpty()) { sql += "AND status=? "; params.add(status); }
        sql += "ORDER BY create_time DESC";
        return getDataTable(jdbc.queryForList(sql, params.toArray()));
    }

    /**
     * 获取单个应用详情
     */
    @GetMapping("/{appId}")
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public AjaxResult getInfo(@PathVariable Long appId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM attendance_app WHERE app_id=?", appId);
        if (rows.isEmpty()) return error("应用不存在");
        return success(rows.get(0));
    }

    /**
     * 创建应用条目
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public AjaxResult create(@RequestBody Map<String, Object> body)
    {
        String appName = getStr(body, "appName");
        String platform = getStr(body, "platform");
        String agentId = getStr(body, "agentId");
        String description = getStr(body, "description");

        if (appName.isEmpty() || platform.isEmpty())
            return error("应用名称和平台不能为空");

        // 自动生成 AppKey (如 ding_xxxxxxxxxxxx 或 ww_xxxxxxxxxxxx)
        String prefix = platform.equalsIgnoreCase("WECHAT") ? "ww_" : "ding_";
        String appKey = prefix + generateRandom(12);
        // 自动生成 AppSecret (32位强随机字符串)
        String appSecret = generateRandom(32);
        // 自动生成 Token (8-16位随机字符串)
        String token = generateRandom(8 + RNG.nextInt(9));
        // 自动生成 AESKey (43位，Base64安全字符)
        String aesKey = generateRandom(43);

        // 生成回调地址
        String baseUrl = getCallbackBaseUrl();
        String callbackPath = platform.equalsIgnoreCase("WECHAT") ?
            "/attendance/callback/wecom" : "/attendance/callback/dingtalk";
        String callbackUrl = baseUrl + callbackPath;

        int rows = jdbc.update(
            "INSERT INTO attendance_app (app_name, platform, app_key, app_secret, " +
            "agent_id, token, aes_key, callback_url, description, status, " +
            "create_by, create_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW())",
            appName, platform.toUpperCase(), appKey, appSecret, agentId,
            token, aesKey, callbackUrl, description, "UNKNOWN",
            getLoginUser() != null ? getLoginUser().getUsername() : "admin");

        if (rows > 0)
        {
            // 同步更新 attendance_config 表
            syncToConfig(platform, "token", token);
            syncToConfig(platform, "aes_key", aesKey);
            syncToConfig(platform, "app_key", appKey);
            syncToConfig(platform, "secret", appSecret);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("appKey", appKey);
            result.put("appSecret", appSecret);
            result.put("token", token);
            result.put("aesKey", aesKey);
            result.put("callbackUrl", callbackUrl);
            return success(result);
        }
        return error("创建失败");
    }

    /**
     * 重新生成密钥 (Token + AESKey)
     */
    @PostMapping("/{appId}/regenerate")
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public AjaxResult regenerate(@PathVariable Long appId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT platform, app_key, app_secret FROM attendance_app WHERE app_id=?", appId);
        if (rows.isEmpty()) return error("应用不存在");

        Map<String, Object> app = rows.get(0);
        String platform = String.valueOf(app.get("platform"));

        String prefix = platform.equalsIgnoreCase("WECHAT") ? "ww_" : "ding_";
        String appKey = prefix + generateRandom(12);
        String appSecret = generateRandom(32);
        String token = generateRandom(8 + RNG.nextInt(9));
        String aesKey = generateRandom(43);

        jdbc.update(
            "UPDATE attendance_app SET app_key=?, app_secret=?, token=?, aes_key=?, update_time=NOW() WHERE app_id=?",
            appKey, appSecret, token, aesKey, appId);

        // 同步配置
        syncToConfig(platform, "token", token);
        syncToConfig(platform, "aes_key", aesKey);
        syncToConfig(platform, "app_key", appKey);
        syncToConfig(platform, "secret", appSecret);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appKey", appKey);
        result.put("appSecret", appSecret);
        result.put("token", token);
        result.put("aesKey", aesKey);
        return success(result);
    }

    /**
     * 删除应用条目
     */
    @DeleteMapping("/{appId}")
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public AjaxResult delete(@PathVariable Long appId)
    {
        jdbc.update("DELETE FROM attendance_app WHERE app_id=?", appId);
        return success();
    }

    /**
     * 连接测试 — 测试回调接口是否可达
     * 向自身回调地址发送测试请求，验证连通性
     */
    @PostMapping("/{appId}/test")
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public AjaxResult testConnection(@PathVariable Long appId, HttpServletRequest request)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM attendance_app WHERE app_id=?", appId);
        if (rows.isEmpty()) return error("应用不存在");

        Map<String, Object> app = rows.get(0);
        String platform = String.valueOf(app.get("platform"));
        String token = String.valueOf(app.get("token"));
        String callbackUrl = String.valueOf(app.get("callback_url"));

        long start = System.currentTimeMillis();
        Map<String, Object> testResult = new LinkedHashMap<>();

        try
        {
            // 构建测试URL（GET方式测试连通性）
            String baseUrl = getCallbackBaseUrl();
            String testUrl = baseUrl + callbackUrl;

            // 生成测试签名
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = generateRandom(8);

            if ("WECHAT".equals(platform))
            {
                // 企业微信签名: sha1(token, timestamp, nonce, echostr)
                String echostr = generateRandom(16);
                String[] arr = {token, timestamp, nonce, echostr};
                Arrays.sort(arr);
                String signStr = String.join("", arr);
                String signature = sha1Hex(signStr);

                // 发送GET测试请求到自身
                String fullUrl = baseUrl + "/attendance/callback/wecom" +
                    "?msg_signature=" + signature +
                    "&timestamp=" + timestamp +
                    "&nonce=" + nonce +
                    "&echostr=" + base64Encode(echostr);

                java.net.URL url = new java.net.URL(fullUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                int httpCode = conn.getResponseCode();
                conn.disconnect();

                testResult.put("testType", "GET 连通测试");
                testResult.put("callbackUrl", callbackUrl);
                testResult.put("httpStatus", httpCode);
                testResult.put("result", httpCode == 200 ? "可达" : "异常");
                testResult.put("latency", System.currentTimeMillis() - start);

                updateAppStatus(appId, httpCode == 200 ? "ONLINE" : "ERROR",
                    "GET " + httpCode + " (" + (System.currentTimeMillis() - start) + "ms)");
            }
            else
            {
                // 钉钉签名: sha256(timestamp + "\n" + token + "\n" + nonce)
                String signStr = timestamp + "\n" + token + "\n" + nonce;
                String signature = sha256Hex(signStr);

                String fullUrl = baseUrl + "/attendance/callback/dingtalk" +
                    "?signature=" + signature +
                    "&timestamp=" + timestamp +
                    "&nonce=" + nonce;

                java.net.URL url = new java.net.URL(fullUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                int httpCode = conn.getResponseCode();
                conn.disconnect();

                testResult.put("testType", "GET 连通测试");
                testResult.put("callbackUrl", callbackUrl);
                testResult.put("httpStatus", httpCode);
                testResult.put("result", httpCode == 200 ? "可达" : "异常");
                testResult.put("latency", System.currentTimeMillis() - start);

                updateAppStatus(appId, httpCode == 200 ? "ONLINE" : "ERROR",
                    "GET " + httpCode + " (" + (System.currentTimeMillis() - start) + "ms)");
            }
        }
        catch (java.net.ConnectException e)
        {
            testResult.put("testType", "GET 连通测试");
            testResult.put("callbackUrl", callbackUrl);
            testResult.put("result", "连接失败");
            testResult.put("error", e.getMessage());
            updateAppStatus(appId, "OFFLINE", "连接失败: " + e.getMessage());
        }
        catch (Exception e)
        {
            testResult.put("testType", "GET 连通测试");
            testResult.put("callbackUrl", callbackUrl);
            testResult.put("result", "测试异常");
            testResult.put("error", e.getMessage());
            updateAppStatus(appId, "ERROR", "异常: " + e.getMessage());
        }

        return success(testResult);
    }

    /**
     * 手动触发测试 — 向回调地址发送模拟考勤数据
     */
    @PostMapping("/{appId}/simulate")
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public AjaxResult simulateCallback(@PathVariable Long appId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM attendance_app WHERE app_id=?", appId);
        if (rows.isEmpty()) return error("应用不存在");

        Map<String, Object> app = rows.get(0);
        String platform = String.valueOf(app.get("platform"));
        String token = String.valueOf(app.get("token"));
        String baseUrl = getCallbackBaseUrl();

        try
        {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = generateRandom(8);

            if ("WECHAT".equals(platform))
            {
                // 企业微信模拟考勤打卡
                String xmlBody = "<xml>" +
                    "<MsgType><![CDATA[event]]></MsgType>" +
                    "<Event><![CDATA[attendance_check_notify]]></Event>" +
                    "<UserId><![CDATA[test_user_001]]></UserId>" +
                    "<CheckinTime><![CDATA[" + (System.currentTimeMillis() / 1000) + "]]></CheckinTime>" +
                    "<CheckinType><![CDATA[上班打卡]]></CheckinType>" +
                    "<LocationDetail><![CDATA[模拟测试位置]]></LocationDetail>" +
                    "<DeviceInfo><![CDATA[模拟设备]]></DeviceInfo>" +
                    "</xml>";
                String encrypted = base64Encode(xmlBody);

                // 计算签名
                String[] arr = {token, timestamp, nonce, encrypted};
                Arrays.sort(arr);
                String signature = sha1Hex(String.join("", arr));

                String encXml = "<xml>" +
                    "<ToUserName><![CDATA[testCorpId]]></ToUserName>" +
                    "<AgentID><![CDATA[1000001]]></AgentID>" +
                    "<Encrypt><![CDATA[" + encrypted + "]]></Encrypt>" +
                    "</xml>";

                String fullUrl = baseUrl + "/attendance/callback/wecom" +
                    "?msg_signature=" + signature +
                    "&timestamp=" + timestamp +
                    "&nonce=" + nonce;

                java.net.URL url = new java.net.URL(fullUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/xml");
                conn.getOutputStream().write(encXml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                int httpCode = conn.getResponseCode();
                conn.disconnect();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("platform", "企业微信");
                result.put("httpStatus", httpCode);
                result.put("result", httpCode == 200 ? "模拟成功" : "异常");
                return success(result);
            }
            else
            {
                // 钉钉模拟考勤打卡
                String signStr = timestamp + "\n" + token + "\n" + nonce;
                String signature = sha256Hex(signStr);

                String jsonBody = "{" +
                    "\"EventType\":\"attendance_check_record\"," +
                    "\"Data\":{" +
                    "\"userId\":\"test_user_001\"," +
                    "\"userName\":\"模拟测试用户\"," +
                    "\"checkType\":\"OnDuty\"," +
                    "\"timeResult\":\"Normal\"," +
                    "\"locationResult\":\"模拟测试位置\"," +
                    "\"userCheckTime\":" + System.currentTimeMillis() + "," +
                    "\"groupId\":\"test_group\"," +
                    "\"groupName\":\"模拟班制\"" +
                    "}}";

                String fullUrl = baseUrl + "/attendance/callback/dingtalk";
                java.net.URL url = new java.net.URL(fullUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("dingtalk-sign", signature);
                conn.setRequestProperty("dingtalk-sig-time", timestamp);
                conn.setRequestProperty("dingtalk-nonce", nonce);
                conn.getOutputStream().write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                int httpCode = conn.getResponseCode();
                conn.disconnect();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("platform", "钉钉");
                result.put("httpStatus", httpCode);
                result.put("result", httpCode == 200 ? "模拟成功" : "异常");
                return success(result);
            }
        }
        catch (Exception e)
        {
            return error("模拟失败: " + e.getMessage());
        }
    }

    // ============ 工具方法 ============

    /** 更新应用状态 */
    private void updateAppStatus(Long appId, String status, String testResult)
    {
        jdbc.update(
            "UPDATE attendance_app SET status=?, last_test_time=NOW(), last_test_result=?, update_time=NOW() " +
            "WHERE app_id=?", status, testResult, appId);
    }

    /** 同步配置到 attendance_config */
    private void syncToConfig(String platform, String keySuffix, String value)
    {
        String configKey = (platform.equalsIgnoreCase("WECHAT") ? "wecom." : "dingtalk.") + keySuffix;
        List<Map<String, Object>> existing = jdbc.queryForList(
            "SELECT config_id FROM attendance_config WHERE config_key=?", configKey);
        if (existing.isEmpty())
        {
            jdbc.update(
                "INSERT INTO attendance_config (config_key, config_value, config_type, description, status, create_by, create_time) " +
                "VALUES (?,?,?,?,?,?,NOW())",
                configKey, value, platform.toUpperCase(), "auto-generated", "0", "admin");
        }
        else
        {
            jdbc.update("UPDATE attendance_config SET config_value=?, update_time=NOW() WHERE config_key=?",
                value, configKey);
        }
    }

    /** 生成随机字符串 */
    private String generateRandom(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
        {
            sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String getStr(Map<String, Object> map, String key)
    {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : "";
    }

    /** 获取回调基础URL */
    private String getCallbackBaseUrl()
    {
        return "http://localhost:8080";
    }

    /** SHA1 */
    private static String sha1Hex(String input) throws Exception
    {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** SHA256 */
    private static String sha256Hex(String input) throws Exception
    {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** Base64编码 */
    private static String base64Encode(String s)
    {
        return Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
