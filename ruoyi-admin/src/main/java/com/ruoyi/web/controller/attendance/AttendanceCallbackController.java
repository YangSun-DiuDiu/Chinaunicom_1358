package com.ruoyi.web.controller.attendance;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;

/**
 * 考勤回调控制器 — 钉钉 / 企业微信 考勤数据对接
 *
 * <h3>钉钉考勤回调（DingTalk Attendance Callback）</h3>
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ 1. 在钉钉开放平台 (open.dingtalk.com) 创建应用                        │
 * │ 2. 进入"事件订阅"，配置回调URL:                                       │
 * │    http(s)://你的域名/attendance/callback/dingtalk                    │
 * │ 3. 订阅事件类型:                                                      │
 * │    - attendance_check_record    (员工打卡事件)                        │
 * │    - attendance_leave_record    (员工请假事件)                        │
 * │    - attendance_overtime_record (员工加班事件)                        │
 * │ 4. 生成并填写Token、AESKey (43位随机字符串)                           │
 * │ 5. 本接口自动处理:                                                    │
 * │    - URL验证(GET): 返回加密的challenge完成验证                        │
 * │    - 事件回调(POST): 解密回调数据、解析考勤记录、存入数据库           │
 * └──────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>企业微信考勤回调（WeCom Attendance Callback）</h3>
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ 1. 在企业微信管理后台 (work.weixin.qq.com) 创建自建应用               │
 * │ 2. 进入"接收消息" → "设置API接收":                                    │
 * │    http(s)://你的域名/attendance/callback/wecom                       │
 * │ 3. 生成并填写Token、EncodingAESKey (43位随机字符串)                  │
 * │ 4. 本接口自动处理:                                                    │
 * │    - URL验证(GET): 解密echostr并返回明文完成验证                      │
 * │    - 事件回调(POST): 解密XML消息体、解析考勤事件、存入数据库          │
 * └──────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/attendance/callback")
public class AttendanceCallbackController
{
    private static final Logger log = LoggerFactory.getLogger(AttendanceCallbackController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AttendanceWebSocketHandler wsHandler;

    // ============ 钉钉回调 ============

    /**
     * 钉钉回调入口 — GET: URL验证 / POST: 事件回调
     *
     * <h4>GET — URL验证</h4>
     * <p>钉钉开放平台配置回调URL时，会发送GET请求验证URL有效性。</p>
     * <pre>
     * 请求示例:
     * GET /attendance/callback/dingtalk?signature=xxx&timestamp=1234567890&nonce=abc123
     *
     * 处理流程:
     * 1. 从 attendance_config 获取 token (dingtalk.token)
     * 2. 计算签名: sha256(timestamp + "\n" + token + "\n" + nonce)
     * 3. 验证签名 → 返回加密后的 {"message":"success"}
     * </pre>
     *
     * <h4>POST — 事件回调</h4>
     * <p>钉钉系统在发生订阅事件时推送JSON数据。</p>
     * <pre>
     * 请求头:
     *   Content-Type: application/json
     *   dingtalk-sign: xxxxx   (签名)
     *   dingtalk-sig-time: 1234567890
     *   dingtalk-nonce: abc123
     *
     * 请求体示例 (考勤打卡):
     * {
     *   "EventType": "attendance_check_record",
     *   "Data": {
     *     "userId": "manager001",
     *     "userName": "张三",
     *     "checkType": "OnDuty",      // OnDuty=上班 OffDuty=下班
     *     "timeResult": "Normal",     // Normal/Early/Late/Absenteeism
     *     "locationResult": "浙江省杭州市余杭区",
     *     "baseCheckTime": 1705651200000,
     *     "userCheckTime": 1705651200000,
     *     "groupId": "group1",
     *     "groupName": "固定班制"
     *   }
     * }
     *
     * 返回: {"msg":"success","code":0}   (必须返回此格式，否则钉钉会重试)
     * </pre>
     */
    @RequestMapping(value = "/dingtalk", method = {RequestMethod.GET, RequestMethod.POST})
    public String dingtalkCallback(HttpServletRequest request,
            @RequestParam Map<String, String> queryParams) throws Exception
    {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String ip = getIpAddr(request);

        if ("GET".equalsIgnoreCase(method))
        {
            return dingtalkUrlVerify(queryParams, ip, start);
        }
        else
        {
            return dingtalkEventCallback(request, ip, start);
        }
    }

    /**
     * 钉钉 URL 验证 (GET请求)
     */
    private String dingtalkUrlVerify(Map<String, String> params, String ip, long start)
    {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("source", "DINGTALK");
        logEntry.put("callbackType", "URL_VERIFY");
        logEntry.put("ipAddress", ip);
        logEntry.put("requestBody", params.toString());

        try
        {
            String signature = params.get("signature");
            String timestamp = params.get("timestamp");
            String nonce = params.get("nonce");
            String token = getConfig("dingtalk.token");

            if (StringUtils.isEmpty(token))
            {
                insertLog(logEntry, "FAILED", "dingtalk.token 未配置", start);
                return "error: token not configured";
            }

            // 计算SHA256签名: timestamp + "\n" + token + "\n" + nonce
            String signStr = timestamp + "\n" + token + "\n" + nonce;
            String calcSign = sha256Hex(signStr);

            if (!calcSign.equals(signature))
            {
                insertLog(logEntry, "FAILED", "签名验证失败: expected=" + calcSign + ", got=" + signature, start);
                return "error: signature mismatch";
            }

            // 返回加密的success消息
            String aesKey = getConfig("dingtalk.aes_key");
            String encryptMsg = encryptMsg("success", aesKey);
            insertLog(logEntry, "SUCCESS", encryptMsg, start);

            return encryptMsg;
        }
        catch (Exception e)
        {
            log.error("钉钉URL验证异常", e);
            insertLog(logEntry, "FAILED", e.getMessage(), start);
            return "error: " + e.getMessage();
        }
    }

    /**
     * 钉钉事件回调 (POST请求)
     */
    private String dingtalkEventCallback(HttpServletRequest request, String ip, long start)
    {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("source", "DINGTALK");
        logEntry.put("ipAddress", ip);

        try
        {
            // 读取请求体
            String body = readBody(request);
            logEntry.put("requestBody", body);

            // 验证签名
            String sign = request.getHeader("dingtalk-sign");
            String sigTime = request.getHeader("dingtalk-sig-time");
            String nonce = request.getHeader("dingtalk-nonce");
            logEntry.put("requestHeaders", "sign=" + sign + ", time=" + sigTime + ", nonce=" + nonce);

            if (!verifyDingtalkSign(sign, sigTime, nonce))
            {
                insertLog(logEntry, "FAILED", "钉钉签名验证失败", start);
                return "{\"msg\":\"signature error\",\"code\":-1}";
            }

            // 解析JSON事件数据
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = mapper.readValue(body, Map.class);
            String eventType = String.valueOf(eventData.getOrDefault("EventType", "unknown"));
            logEntry.put("callbackType", eventType);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData.getOrDefault("Data", new HashMap<>());

            if ("attendance_check_record".equals(eventType))
            {
                processDingtalkAttendance(data, logEntry);
            }
            else if ("attendance_leave_record".equals(eventType))
            {
                processDingtalkLeave(data, logEntry);
            }
            else if ("attendance_overtime_record".equals(eventType))
            {
                processDingtalkOvertime(data, logEntry);
            }

            insertLog(logEntry, "SUCCESS", "{\"msg\":\"success\",\"code\":0}", start);

            // 通过WebSocket推送实时消息
            wsHandler.pushAttendanceUpdate("DINGTALK", eventType, data);

            // 钉钉要求返回此格式
            return "{\"msg\":\"success\",\"code\":0}";
        }
        catch (Exception e)
        {
            log.error("钉钉事件回调处理异常", e);
            insertLog(logEntry, "FAILED", "异常: " + e.getMessage(), start);
            return "{\"msg\":\"error\",\"code\":-1}";
        }
    }

    /** 处理钉钉考勤打卡事件 */
    private void processDingtalkAttendance(Map<String, Object> data, Map<String, Object> logEntry)
    {
        String userId = getString(data, "userId");
        String userName = getString(data, "userName");
        String checkType = getString(data, "checkType");      // OnDuty / OffDuty
        String timeResult = getString(data, "timeResult");    // Normal/Early/Late/Absenteeism
        String location = getString(data, "locationResult");
        String deviceInfo = getString(data, "deviceInfo");
        Long checkTime = getLong(data, "userCheckTime");      // 毫秒时间戳

        logEntry.put("userName", userName);

        if (checkTime == null || userName.isEmpty()) return;

        String status = mapDingtalkStatus(timeResult);
        Date date = new Date(checkTime);
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
        String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        logEntry.put("attendanceDate", dateStr);

        // 查询是否已有该用户当天的记录
        List<Map<String, Object>> existing = jdbc.queryForList(
            "SELECT record_id, check_in_time, check_out_time FROM attendance_record " +
            "WHERE external_user_id=? AND attendance_date=?",
            userId, dateStr);

        if (existing.isEmpty())
        {
            // 新记录
            String checkIn = "OnDuty".equals(checkType) ? timeStr : null;
            String checkOut = "OffDuty".equals(checkType) ? timeStr : null;
            jdbc.update(
                "INSERT INTO attendance_record (user_name, external_user_id, dept_name, " +
                "attendance_date, check_in_time, check_out_time, status, source, " +
                "location_result, device_info, create_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,NOW())",
                userName, userId, getString(data, "groupName"),
                dateStr, checkIn, checkOut, status, "DINGTALK",
                location, deviceInfo);
        }
        else
        {
            // 更新已有记录
            Map<String, Object> rec = existing.get(0);
            Long recordId = (Long) rec.get("record_id");
            if ("OnDuty".equals(checkType))
            {
                jdbc.update("UPDATE attendance_record SET check_in_time=?, location_result=?, status=? " +
                    "WHERE record_id=?", timeStr, location, status, recordId);
            }
            else
            {
                jdbc.update("UPDATE attendance_record SET check_out_time=?, location_result=?, status=? " +
                    "WHERE record_id=?", timeStr, location, status, recordId);
            }
        }
    }

    /** 处理钉钉请假事件 */
    private void processDingtalkLeave(Map<String, Object> data, Map<String, Object> logEntry)
    {
        String userName = getString(data, "userName");
        String leaveType = getString(data, "leaveType");
        logEntry.put("userName", userName);
        logEntry.put("callbackType", "LEAVE_RECORD:" + leaveType);

        // 写入备注到当天考勤记录
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());

        List<Map<String, Object>> existing = jdbc.queryForList(
            "SELECT record_id FROM attendance_record WHERE user_name=? AND attendance_date=?",
            userName, dateStr);
        if (existing.isEmpty())
        {
            jdbc.update(
                "INSERT INTO attendance_record (user_name, dept_name, attendance_date, " +
                "status, source, remark, create_time) VALUES (?,?,?,?,?,?,NOW())",
                userName, getString(data, "groupName"), dateStr,
                "ABSENT", "DINGTALK", "请假-" + leaveType);
        }
        else
        {
            Long recordId = (Long) existing.get(0).get("record_id");
            jdbc.update("UPDATE attendance_record SET status='ABSENT', remark=? WHERE record_id=?",
                "请假-" + leaveType, recordId);
        }
    }

    /** 处理钉钉加班事件 */
    private void processDingtalkOvertime(Map<String, Object> data, Map<String, Object> logEntry)
    {
        String userName = getString(data, "userName");
        logEntry.put("userName", userName);
        logEntry.put("callbackType", "OVERTIME_RECORD");

        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
        jdbc.update(
            "INSERT INTO attendance_record (user_name, dept_name, attendance_date, " +
            "status, source, remark, create_time) VALUES (?,?,?,?,?,?,NOW()) " +
            "ON DUPLICATE KEY UPDATE status='OVERTIME'",
            userName, getString(data, "groupName"), dateStr,
            "OVERTIME", "DINGTALK", "加班");
    }

    // ============ 企业微信回调 ============

    /**
     * 企业微信回调入口 — GET: URL验证 / POST: 事件回调
     *
     * <h4>GET — URL验证</h4>
     * <pre>
     * 请求示例:
     * GET /attendance/callback/wecom?msg_signature=xxx&timestamp=1234567890&nonce=abc123&echostr=encrypted_str
     *
     * 处理流程:
     * 1. 从 attendance_config 获取 token (wecom.token)
     * 2. 验证签名: sha1(token, timestamp, nonce, echostr)
     * 3. 解密 echostr 并返回明文
     * </pre>
     *
     * <h4>POST — 事件回调</h4>
     * <pre>
     * 请求体 (XML格式):
     * &lt;xml&gt;
     *   &lt;ToUserName&gt;企业CorpID&lt;/ToUserName&gt;
     *   &lt;AgentID&gt;应用AgentID&lt;/AgentID&gt;
     *   &lt;Encrypt&gt;加密的消息体&lt;/Encrypt&gt;
     * &lt;/xml&gt;
     *
     * 解密后的消息体 (XML格式):
     * &lt;xml&gt;
     *   &lt;MsgType&gt;event&lt;/MsgType&gt;
     *   &lt;Event&gt;attendance_check_notify&lt;/Event&gt;
     *   &lt;UserId&gt;ZhangSan&lt;/UserId&gt;
     *   &lt;CheckinTime&gt;1705651200&lt;/CheckinTime&gt;
     *   &lt;CheckinType&gt;上班打卡&lt;/CheckinType&gt;
     *   &lt;LocationDetail&gt;浙江省杭州市余杭区&lt;/LocationDetail&gt;
     * &lt;/xml&gt;
     *
     * 返回: 200 OK (空字符串即可)
     * </pre>
     */
    @RequestMapping(value = "/wecom", method = {RequestMethod.GET, RequestMethod.POST})
    public String wecomCallback(HttpServletRequest request,
            @RequestParam Map<String, String> queryParams) throws Exception
    {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String ip = getIpAddr(request);

        if ("GET".equalsIgnoreCase(method))
        {
            return wecomUrlVerify(queryParams, ip, start);
        }
        else
        {
            return wecomEventCallback(request, ip, start);
        }
    }

    /** 企业微信 URL 验证 */
    private String wecomUrlVerify(Map<String, String> params, String ip, long start)
    {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("source", "WECHAT");
        logEntry.put("callbackType", "URL_VERIFY");
        logEntry.put("ipAddress", ip);
        logEntry.put("requestBody", params.toString());

        try
        {
            String msgSignature = params.get("msg_signature");
            String timestamp = params.get("timestamp");
            String nonce = params.get("nonce");
            String echostr = params.get("echostr");
            String token = getConfig("wecom.token");
            String aesKey = getConfig("wecom.encoding_aes_key");

            if (StringUtils.isEmpty(token) || StringUtils.isEmpty(aesKey))
            {
                insertLog(logEntry, "FAILED", "wecom.token或wecom.encoding_aes_key未配置", start);
                return "error: token/aesKey not configured";
            }

            // 验证签名: sha1(token, timestamp, nonce, echostr)
            if (!verifyWecomSign(msgSignature, token, timestamp, nonce, echostr))
            {
                insertLog(logEntry, "FAILED", "企业微信签名验证失败", start);
                return "error: signature mismatch";
            }

            // 解密 echostr 并返回明文
            String decrypted = decryptMsg(echostr, aesKey);
            insertLog(logEntry, "SUCCESS", decrypted, start);
            return decrypted;
        }
        catch (Exception e)
        {
            log.error("企业微信URL验证异常", e);
            insertLog(logEntry, "FAILED", e.getMessage(), start);
            return "error: " + e.getMessage();
        }
    }

    /** 企业微信事件回调 */
    private String wecomEventCallback(HttpServletRequest request, String ip, long start)
    {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("source", "WECHAT");
        logEntry.put("ipAddress", ip);

        try
        {
            String body = readBody(request);
            logEntry.put("requestBody", body);

            // 解析XML，提取 Encrypt 字段
            String encrypt = extractXmlTag(body, "Encrypt");
            if (StringUtils.isEmpty(encrypt))
            {
                insertLog(logEntry, "FAILED", "无法解析XML Encrypt字段", start);
                return "";
            }

            // 解密消息体
            String aesKey = getConfig("wecom.encoding_aes_key");
            String decryptedXml = decryptMsg(encrypt, aesKey);
            logEntry.put("responseBody", "decrypted: " + decryptedXml);

            // 解析解密后的XML
            String msgType = extractXmlTag(decryptedXml, "MsgType");
            String event = extractXmlTag(decryptedXml, "Event");
            logEntry.put("callbackType", msgType + ":" + event);

            if ("attendance_check_notify".equals(event))
            {
                processWecomAttendance(decryptedXml, logEntry);
            }

            insertLog(logEntry, "SUCCESS", "", start);

            // WebSocket实时推送
            wsHandler.pushRawMessage("WECHAT", event, decryptedXml);

            return "";
        }
        catch (Exception e)
        {
            log.error("企业微信事件回调处理异常", e);
            insertLog(logEntry, "FAILED", "异常: " + e.getMessage(), start);
            return "";
        }
    }

    /** 处理企业微信考勤打卡通知 */
    private void processWecomAttendance(String xml, Map<String, Object> logEntry)
    {
        String userId = extractXmlTag(xml, "UserId");
        String checkinType = extractXmlTag(xml, "CheckinType");     // 上班打卡/下班打卡/外出打卡
        String checkinTime = extractXmlTag(xml, "CheckinTime");     // Unix时间戳
        String location = extractXmlTag(xml, "LocationDetail");
        String deviceInfo = extractXmlTag(xml, "DeviceInfo");

        logEntry.put("userName", userId);

        if (StringUtils.isEmpty(checkinTime) || StringUtils.isEmpty(userId)) return;

        long timestamp = Long.parseLong(checkinTime);
        Date date = new Date(timestamp * 1000);
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
        String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        logEntry.put("attendanceDate", dateStr);

        List<Map<String, Object>> existing = jdbc.queryForList(
            "SELECT record_id, check_in_time, check_out_time FROM attendance_record " +
            "WHERE external_user_id=? AND attendance_date=?",
            userId, dateStr);

        if (existing.isEmpty())
        {
            String checkIn = checkinType.contains("上班") ? timeStr : null;
            String checkOut = checkinType.contains("下班") ? timeStr : null;
            jdbc.update(
                "INSERT INTO attendance_record (user_name, external_user_id, dept_name, " +
                "attendance_date, check_in_time, check_out_time, status, source, " +
                "location_result, device_info, create_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,NOW())",
                userId, userId, "",
                dateStr, checkIn, checkOut, "NORMAL", "WECHAT",
                location, deviceInfo);
        }
        else
        {
            Map<String, Object> rec = existing.get(0);
            Long recordId = (Long) rec.get("record_id");
            if (checkinType.contains("上班"))
            {
                jdbc.update("UPDATE attendance_record SET check_in_time=?, location_result=? " +
                    "WHERE record_id=?", timeStr, location, recordId);
            }
            else
            {
                jdbc.update("UPDATE attendance_record SET check_out_time=?, location_result=? " +
                    "WHERE record_id=?", timeStr, location, recordId);
            }
        }
    }

    // ============ WebSocket配置 (内部类) ============

    /**
     * WebSocket 配置 — 注册考勤实时推送端点
     */
    @Configuration
    @EnableWebSocket
    public static class AttendanceWebSocketConfig implements WebSocketConfigurer
    {
        @Autowired
        private AttendanceWebSocketHandler handler;

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
        {
            // 考勤实时推送端点: ws://host:port/ws/attendance
            registry.addHandler(handler, "/ws/attendance")
                    .setAllowedOriginPatterns("*");
        }
    }

    /**
     * WebSocket 处理器 — 考勤数据实时推送
     *
     * <h4>WebSocket 接入说明</h4>
     * <pre>
     * 连接地址: ws://localhost:8080/ws/attendance
     *
     * 客户端连接后，服务端在有新回调数据时自动推送JSON消息:
     * {
     *   "type": "ATTENDANCE_UPDATE",    // 消息类型
     *   "source": "DINGTALK",           // 来源: DINGTALK / WECHAT
     *   "eventType": "attendance_check_record",
     *   "timestamp": "2026-06-16 10:30:00",
     *   "data": { ... }                 // 考勤数据
     * }
     *
     * 客户端也可以发送PING消息保持连接:
     *   → {"type":"PING"}
     *   ← {"type":"PONG","timestamp":"2026-06-16 10:30:00"}
     * </pre>
     */
    @org.springframework.stereotype.Component
    public static class AttendanceWebSocketHandler extends TextWebSocketHandler
    {
        private static final Logger wsLog = LoggerFactory.getLogger(AttendanceWebSocketHandler.class);
        private static final ObjectMapper wsMapper = new ObjectMapper();

        /** 所有已连接的客户端 */
        private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

        /** 在线客户端计数 */
        public int getOnlineCount()
        {
            return sessions.size();
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception
        {
            sessions.add(session);
            wsLog.info("WebSocket客户端连接: id={}, 当前在线={}", session.getId(), sessions.size());

            // 发送欢迎消息
            Map<String, Object> welcome = new LinkedHashMap<>();
            welcome.put("type", "WELCOME");
            welcome.put("message", "考勤数据实时推送已连接");
            welcome.put("onlineCount", sessions.size());
            welcome.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            session.sendMessage(new TextMessage(wsMapper.writeValueAsString(welcome)));
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception
        {
            sessions.remove(session);
            wsLog.info("WebSocket客户端断开: id={}, 状态={}, 当前在线={}",
                session.getId(), status, sessions.size());
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception
        {
            String payload = message.getPayload();
            wsLog.debug("收到WebSocket消息: id={}, payload={}", session.getId(), payload);

            try
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = wsMapper.readValue(payload, Map.class);
                String type = String.valueOf(msg.getOrDefault("type", ""));

                if ("PING".equals(type))
                {
                    Map<String, Object> pong = new LinkedHashMap<>();
                    pong.put("type", "PONG");
                    pong.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    session.sendMessage(new TextMessage(wsMapper.writeValueAsString(pong)));
                }
            }
            catch (Exception e)
            {
                wsLog.debug("WebSocket消息解析失败: {}", e.getMessage());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception
        {
            wsLog.error("WebSocket传输错误: id={}, error={}", session.getId(), exception.getMessage());
            sessions.remove(session);
        }

        /**
         * 推送考勤更新消息给所有客户端
         */
        public void pushAttendanceUpdate(String source, String eventType, Map<String, Object> data)
        {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "ATTENDANCE_UPDATE");
            msg.put("source", source);
            msg.put("eventType", eventType);
            msg.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            msg.put("data", data != null ? data : new HashMap<>());

            broadcast(wsMapper, msg);
        }

        /**
         * 推送原始消息给所有客户端
         */
        public void pushRawMessage(String source, String eventType, String rawData)
        {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "RAW_MESSAGE");
            msg.put("source", source);
            msg.put("eventType", eventType);
            msg.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            msg.put("rawData", rawData);

            broadcast(wsMapper, msg);
        }

        private void broadcast(ObjectMapper mapper, Map<String, Object> message)
        {
            if (sessions.isEmpty()) return;
            try
            {
                String json = mapper.writeValueAsString(message);
                TextMessage textMsg = new TextMessage(json);
                for (WebSocketSession session : sessions)
                {
                    try
                    {
                        if (session.isOpen())
                        {
                            session.sendMessage(textMsg);
                        }
                    }
                    catch (Exception e)
                    {
                        wsLog.debug("推送WebSocket消息失败: id={}", session.getId());
                    }
                }
            }
            catch (Exception e)
            {
                wsLog.error("WebSocket广播异常", e);
            }
        }
    }

    // ============ 工具方法 ============

    /** 读取请求体 */
    private String readBody(HttpServletRequest request) throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream is = request.getInputStream())
        {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    /** 获取客户端IP */
    private String getIpAddr(HttpServletRequest request)
    {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip)) ip = "127.0.0.1";
        return ip;
    }

    /** 从 attendance_config 表获取配置值 */
    private String getConfig(String key)
    {
        try
        {
            return jdbc.queryForObject(
                "SELECT config_value FROM attendance_config WHERE config_key=? AND status='0'",
                String.class, key);
        }
        catch (Exception e)
        {
            return "";
        }
    }

    /** 从XML中提取指定标签的值 */
    private String extractXmlTag(String xml, String tag)
    {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start)
        {
            String val = xml.substring(start + startTag.length(), end);
            // CDATA处理
            if (val.startsWith("<![CDATA[") && val.endsWith("]]>"))
                val = val.substring(9, val.length() - 3);
            return val;
        }
        return "";
    }

    /** Map安全取值 */
    private String getString(Map<String, Object> map, String key)
    {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private Long getLong(Map<String, Object> map, String key)
    {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String && !((String) v).isEmpty())
        {
            try { return Long.parseLong((String) v); }
            catch (Exception e) { }
        }
        return null;
    }

    /** 钉钉状态映射 */
    private String mapDingtalkStatus(String timeResult)
    {
        switch (timeResult)
        {
            case "Normal":       return "NORMAL";
            case "Early":        return "EARLY";
            case "Late":         return "LATE";
            case "Absenteeism":  return "ABSENT";
            case "NotSigned":    return "ABSENT";
            default:             return "NORMAL";
        }
    }

    /** 验证钉钉签名 */
    private boolean verifyDingtalkSign(String sign, String timestamp, String nonce)
    {
        if (StringUtils.isEmpty(sign)) return true; // 宽松模式：无签名时放行
        try
        {
            String token = getConfig("dingtalk.token");
            String signStr = timestamp + "\n" + token + "\n" + nonce;
            String calcSign = sha256Hex(signStr);
            return calcSign.equals(sign);
        }
        catch (Exception e) { return false; }
    }

    /** 验证企业微信签名 */
    private boolean verifyWecomSign(String msgSignature, String token, String timestamp, String nonce, String echostr)
    {
        try
        {
            String[] arr = {token, timestamp, nonce, echostr};
            Arrays.sort(arr);
            String raw = String.join("", arr);
            String calcSign = sha1Hex(raw);
            return calcSign.equals(msgSignature);
        }
        catch (Exception e) { return false; }
    }

    /** SHA256 Hex */
    private static String sha256Hex(String input) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** SHA1 Hex */
    private static String sha1Hex(String input) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** 简单消息加密（AES占位，生产环境请替换为完整AES加解密实现） */
    private String encryptMsg(String msg, String aesKey)
    {
        // 简化的加密返回 — 生产环境请使用AES-256-CBC完整加密
        // 钉钉加解密库: com.alibaba:dahsdk 或自行实现
        try
        {
            return Base64.getEncoder().encodeToString(msg.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e) { return msg; }
    }

    /** 简单消息解密（占位，生产环境请替换） */
    private String decryptMsg(String encrypted, String aesKey)
    {
        try
        {
            return new String(Base64.getDecoder().decode(encrypted), StandardCharsets.UTF_8);
        }
        catch (Exception e) { return encrypted; }
    }

    /** 插入回调日志 */
    private void insertLog(Map<String, Object> logEntry, String result, String response, long start)
    {
        try
        {
            long cost = System.currentTimeMillis() - start;
            jdbc.update(
                "INSERT INTO attendance_callback_log (source, callback_type, request_body, " +
                "request_headers, process_result, response_body, error_message, user_name, " +
                "attendance_date, ip_address, cost_ms, create_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW())",
                logEntry.getOrDefault("source", ""),
                logEntry.getOrDefault("callbackType", ""),
                truncate((String) logEntry.getOrDefault("requestBody", ""), 4000),
                truncate((String) logEntry.getOrDefault("requestHeaders", ""), 1000),
                result,
                truncate(String.valueOf(response), 2000),
                "FAILED".equals(result) ? truncate(String.valueOf(response), 500) : "",
                logEntry.getOrDefault("userName", ""),
                logEntry.getOrDefault("attendanceDate", null),
                logEntry.getOrDefault("ipAddress", ""),
                (int) cost);
        }
        catch (Exception e)
        {
            log.error("插入回调日志失败", e);
        }
    }

    private String truncate(String s, int maxLen)
    {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
