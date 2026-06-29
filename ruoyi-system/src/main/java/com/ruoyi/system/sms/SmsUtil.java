package com.ruoyi.system.sms;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.VisitorAppointment;

/**
 * 短信统一发送入口——五表驱动多厂商路由
 *
 * @author ruoyi
 */
@Component
public class SmsUtil
{
    private static final Logger log = LoggerFactory.getLogger(SmsUtil.class);

    @Autowired
    private JdbcTemplate jdbc;

    /** 驱动缓存（单例复用） */
    private final Map<String, SmsDriver> driverCache = new HashMap<>();

    /**
     * 统一发送入口
     *
     * @param bizCode  业务编码（sys_sms_biz.biz_code）
     * @param phone    手机号码
     * @param params   模板参数（JSON字符串）
     * @param sendMode 发送模式：1即时 2定时
     * @param sendTime 定时发送时间（即时为null）
     * @return true成功 false失败
     */
    public boolean sendSms(String bizCode, String phone, String params, int sendMode, String sendTime)
    {
        if (StringUtils.isEmpty(bizCode) || StringUtils.isEmpty(phone))
        {
            log.warn("sendSms参数不完整: bizCode={}, phone={}", bizCode, phone);
            return false;
        }

        // 1. 查询业务绑定
        Map<String, Object> biz = queryOne("SELECT * FROM sys_sms_biz WHERE biz_code=? AND status='0' AND del_flag='0'", bizCode);
        if (biz == null)
        {
            log.error("业务编码不存在或已禁用: bizCode={}", bizCode);
            return false;
        }

        // 2. 查询签名模板
        Long stId = getLong(biz, "st_id");
        Map<String, Object> st = queryOne("SELECT * FROM sys_sms_sign_template WHERE st_id=? AND status='0' AND del_flag='0'", stId);
        if (st == null)
        {
            log.error("签名模板不存在或已禁用: st_id={}", stId);
            return false;
        }

        // 3. 查询主渠道
        Long channelId = getLong(st, "channel_id");
        Map<String, Object> channel = queryOne("SELECT * FROM sys_sms_channel WHERE channel_id=? AND status='0' AND del_flag='0'", channelId);
        if (channel == null)
        {
            log.error("短信渠道不存在或已禁用: channel_id={}", channelId);
            return false;
        }

        // 4. 黑名单检查
        if (isBlacklisted(phone))
        {
            log.warn("手机号在黑名单中，拒绝发送: phone={}", phone);
            return false;
        }

        // 5. 频率限制检查
        int minuteLimit = getInt(biz, "minute_limit", 5);
        int dayLimit = getInt(biz, "day_limit", 100);
        if (!checkRateLimit(phone, minuteLimit, dayLimit))
        {
            log.warn("短信发送频率超限: phone={}, minuteLimit={}, dayLimit={}", phone, minuteLimit, dayLimit);
            return false;
        }

        // 6. 定时模式——仅入库
        if (sendMode == 2)
        {
            insertLog(bizCode, getString(channel, "channel_type"), phone,
                    getString(st, "template_code"), getString(st, "sign_name"),
                    params, sendMode, sendTime, "WAIT", 0, "");
            log.info("定时短信已入库: bizCode={}, phone={}, sendTime={}", bizCode, phone, sendTime);
            return true;
        }

        // 7. 即时模式——路由驱动发送
        String channelType = getString(channel, "channel_type");
        SmsDriver driver = resolveDriver(channelType, channel);

        if (driver == null)
        {
            log.error("无法解析短信驱动: channelType={}", channelType);
            insertLog(bizCode, channelType, phone, getString(st, "template_code"),
                    getString(st, "sign_name"), params, 1, null, "FAIL", 0,
                    "无法解析驱动：" + channelType);
            return false;
        }

        boolean success = sendWithRetry(driver, channel, st, phone, params,
                getInt(channel, "retry_count", 0));

        // 8. 故障切换至备用渠道
        if (!success)
        {
            Long backupChannelId = getLong(biz, "backup_channel_id");
            if (backupChannelId != null && backupChannelId > 0)
            {
                Map<String, Object> backupChannel = queryOne(
                        "SELECT * FROM sys_sms_channel WHERE channel_id=? AND status='0' AND del_flag='0'",
                        backupChannelId);
                if (backupChannel != null)
                {
                    log.info("主渠道发送失败，切换至备用渠道: channel_id={}", backupChannelId);
                    SmsDriver backupDriver = resolveDriver(
                            getString(backupChannel, "channel_type"), backupChannel);
                    if (backupDriver != null)
                    {
                        success = sendWithRetry(backupDriver, backupChannel, st, phone, params,
                                getInt(backupChannel, "retry_count", 0));
                    }
                }
            }
        }

        String taskStatus = success ? "SUCCESS" : "FAIL";
        String resultMsg = success ? "发送成功" : "发送失败";
        insertLog(bizCode, channelType, phone, getString(st, "template_code"),
                getString(st, "sign_name"), params, 1, null, taskStatus, 0, resultMsg);

        return success;
    }

    /**
     * 定时任务触发——扫描待执行任务并发送
     */
    public void executeScheduledTasks()
    {
        List<Map<String, Object>> tasks = jdbc.queryForList(
                "SELECT * FROM sys_sms_log WHERE send_mode=2 AND task_status='WAIT' AND send_time <= NOW() ORDER BY create_time ASC LIMIT 100");
        if (tasks.isEmpty())
        {
            return;
        }
        log.info("执行定时短信任务，待发送数量: {}", tasks.size());
        for (Map<String, Object> task : tasks)
        {
            Long logId = getLong(task, "log_id");
            String bizCode = getString(task, "biz_code");
            String phone = getString(task, "phone");
            String params = getString(task, "param");

            Map<String, Object> biz = queryOne(
                    "SELECT * FROM sys_sms_biz WHERE biz_code=? AND status='0' AND del_flag='0'", bizCode);
            if (biz == null)
            {
                jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg='业务编码已失效' WHERE log_id=?", logId);
                continue;
            }

            Long stId = getLong(biz, "st_id");
            Map<String, Object> st = queryOne(
                    "SELECT * FROM sys_sms_sign_template WHERE st_id=? AND status='0' AND del_flag='0'", stId);
            if (st == null)
            {
                jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg='签名模板已失效' WHERE log_id=?", logId);
                continue;
            }

            Long channelId = getLong(st, "channel_id");
            Map<String, Object> channel = queryOne(
                    "SELECT * FROM sys_sms_channel WHERE channel_id=? AND status='0' AND del_flag='0'", channelId);
            if (channel == null)
            {
                jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg='渠道已失效' WHERE log_id=?", logId);
                continue;
            }

            String channelType = getString(channel, "channel_type");
            SmsDriver driver = resolveDriver(channelType, channel);
            if (driver == null)
            {
                jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg=? WHERE log_id=?",
                        "无法解析驱动：" + channelType, logId);
                continue;
            }

            boolean success = driver.send(channel, st, phone, params);
            String taskStatus = success ? "SUCCESS" : "FAIL";
            String resultMsg = success ? "定时发送成功" : "定时发送失败";
            jdbc.update("UPDATE sys_sms_log SET task_status=?, result_msg=?, update_time=NOW() WHERE log_id=?",
                    taskStatus, resultMsg, logId);
            log.info("定时短信发送完成: logId={}, result={}", logId, taskStatus);
        }
    }

    /**
     * 撤销定时任务
     */
    public boolean cancelScheduled(Long logId)
    {
        if (logId == null)
        {
            return false;
        }
        int rows = jdbc.update(
                "UPDATE sys_sms_log SET task_status='CANCEL', update_time=NOW() WHERE log_id=? AND send_mode=2 AND task_status='WAIT'",
                logId);
        if (rows > 0)
        {
            log.info("定时短信已撤销: logId={}", logId);
            return true;
        }
        log.warn("定时短信撤销失败（任务不存在或状态不可撤销）: logId={}", logId);
        return false;
    }

    /**
     * 重发短信
     */
    public boolean retry(Long logId)
    {
        if (logId == null)
        {
            return false;
        }
        Map<String, Object> logRow = queryOne("SELECT * FROM sys_sms_log WHERE log_id=?", logId);
        if (logRow == null)
        {
            log.warn("短信日志不存在: logId={}", logId);
            return false;
        }

        String bizCode = getString(logRow, "biz_code");
        String phone = getString(logRow, "phone");
        String params = getString(logRow, "param");
        int retryCount = getInt(logRow, "retry_count", 0);

        // 查询业务绑定获取模板和渠道
        Map<String, Object> biz = queryOne(
                "SELECT * FROM sys_sms_biz WHERE biz_code=? AND status='0' AND del_flag='0'", bizCode);
        if (biz == null)
        {
            jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg='业务编码已失效' WHERE log_id=?", logId);
            return false;
        }

        Long stId = getLong(biz, "st_id");
        Map<String, Object> st = queryOne(
                "SELECT * FROM sys_sms_sign_template WHERE st_id=? AND status='0' AND del_flag='0'", stId);
        if (st == null)
        {
            jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg='签名模板已失效' WHERE log_id=?", logId);
            return false;
        }

        Long channelId = getLong(st, "channel_id");
        Map<String, Object> channel = queryOne(
                "SELECT * FROM sys_sms_channel WHERE channel_id=? AND status='0' AND del_flag='0'", channelId);
        if (channel == null)
        {
            jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg='渠道已失效' WHERE log_id=?", logId);
            return false;
        }

        String channelType = getString(channel, "channel_type");
        SmsDriver driver = resolveDriver(channelType, channel);
        if (driver == null)
        {
            jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg=? WHERE log_id=?",
                    "无法解析驱动：" + channelType, logId);
            return false;
        }

        boolean success = driver.send(channel, st, phone, params);
        int newRetryCount = retryCount + 1;
        String taskStatus = success ? "SUCCESS" : "FAIL";
        String resultMsg = success ? "重发成功" : "重发失败";
        jdbc.update("UPDATE sys_sms_log SET task_status=?, retry_count=?, result_msg=?, update_time=NOW() WHERE log_id=?",
                taskStatus, newRetryCount, resultMsg, logId);
        log.info("短信重发完成: logId={}, result={}, retryCount={}", logId, taskStatus, newRetryCount);
        return success;
    }

    /**
     * 测试渠道发送 — 跳过业务绑定、限流、主备切换，直接调驱动
     */
    public boolean sendByChannel(Map<String, Object> channel, Map<String, Object> template, String phone, String params)
    {
        String channelType = getString(channel, "channel_type");
        SmsDriver driver = resolveDriver(channelType, channel);
        if (driver == null)
        {
            log.error("未知渠道类型: {}", channelType);
            return false;
        }
        try
        {
            return driver.send(channel, template, phone, params);
        }
        catch (Exception e)
        {
            log.error("测试发送失败: channel={}, phone={}", channelType, phone, e);
            return false;
        }
    }

    /**
     * 根据渠道类型解析驱动实例
     */
    private SmsDriver resolveDriver(String channelType, Map<String, Object> channel)
    {
        if (StringUtils.isEmpty(channelType))
        {
            return null;
        }
        String key = channelType.toUpperCase();
        SmsDriver cached = driverCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        SmsDriver driver;
        switch (key)
        {
            case "ALIYUN":
                String ak = getString(channel, "access_key_id");
                String sk = getString(channel, "access_key_secret");
                driver = new AliyunSmsDriver(ak, sk);
                break;
            case "TENCENT":
                driver = new TencentSmsDriver();
                break;
            case "API":
                driver = new GenericHttpDriver();
                break;
            default:
                log.error("不支持的渠道类型: {}", channelType);
                return null;
        }
        driverCache.put(key, driver);
        return driver;
    }

    /**
     * 带重试的发送
     */
    private boolean sendWithRetry(SmsDriver driver, Map<String, Object> channel,
            Map<String, Object> st, String phone, String params, int maxRetry)
    {
        for (int i = 0; i <= maxRetry; i++)
        {
            if (i > 0)
            {
                log.info("短信发送重试: 第{}次, phone={}", i, phone);
            }
            boolean success = driver.send(channel, st, phone, params);
            if (success)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查黑名单
     */
    private boolean isBlacklisted(String phone)
    {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM sys_sms_blacklist WHERE phone=? AND status='0' AND del_flag='0'",
                Integer.class, phone);
        return count != null && count > 0;
    }

    /**
     * 检查频率限制（分钟限流 + 日限流）
     */
    private boolean checkRateLimit(String phone, int minuteLimit, int dayLimit)
    {
        // 分钟限流
        Integer minuteCount = jdbc.queryForObject(
                "SELECT COUNT(1) FROM sys_sms_log WHERE phone=? AND create_time >= DATE_SUB(NOW(), INTERVAL 1 MINUTE) AND task_status='SUCCESS'",
                Integer.class, phone);
        if (minuteCount != null && minuteCount >= minuteLimit)
        {
            log.warn("分钟发送频率超限: phone={}, minuteCount={}, minuteLimit={}", phone, minuteCount, minuteLimit);
            return false;
        }

        // 日限流
        Integer dayCount = jdbc.queryForObject(
                "SELECT COUNT(1) FROM sys_sms_log WHERE phone=? AND create_time >= CURDATE() AND task_status='SUCCESS'",
                Integer.class, phone);
        if (dayCount != null && dayCount >= dayLimit)
        {
            log.warn("日发送频率超限: phone={}, dayCount={}, dayLimit={}", phone, dayCount, dayLimit);
            return false;
        }

        return true;
    }

    /**
     * 写入发送日志
     */
    private void insertLog(String bizCode, String channelType, String phone,
            String templateCode, String signName, String param,
            int sendMode, String sendTime, String taskStatus, int retryCount, String resultMsg)
    {
        try
        {
            jdbc.update(
                    "INSERT INTO sys_sms_log (biz_code, channel_type, phone, template_code, sign_name, param, "
                            + "send_mode, send_time, task_status, retry_count, result_msg, create_time) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    bizCode, channelType, phone, templateCode, signName, param,
                    sendMode, sendTime, taskStatus, retryCount, resultMsg);
        }
        catch (Exception e)
        {
            log.error("短信日志写入失败: bizCode={}, phone={}", bizCode, phone, e);
        }
    }

    /**
     * 查询单行记录
     */
    private Map<String, Object> queryOne(String sql, Object... args)
    {
        try
        {
            List<Map<String, Object>> list = jdbc.queryForList(sql, args);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }
        catch (Exception e)
        {
            log.error("数据库查询异常: sql={}", sql, e);
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key)
    {
        Object val = map != null ? map.get(key) : null;
        return val != null ? val.toString() : "";
    }

    private Long getLong(Map<String, Object> map, String key)
    {
        Object val = map != null ? map.get(key) : null;
        if (val instanceof Number)
        {
            return ((Number) val).longValue();
        }
        if (val != null)
        {
            try
            {
                return Long.parseLong(val.toString());
            }
            catch (NumberFormatException e)
            {
                // fall through
            }
        }
        return null;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue)
    {
        Object val = map != null ? map.get(key) : null;
        if (val instanceof Number)
        {
            return ((Number) val).intValue();
        }
        if (val != null)
        {
            try
            {
                return Integer.parseInt(val.toString());
            }
            catch (NumberFormatException e)
            {
                // fall through
            }
        }
        return defaultValue;
    }

    // ============ 兼容旧调用方的桥接方法 ============

    /**
     * 兼容旧版 sendSms(recipient, phoneNumber, content, bizType, bizId) 调用
     */
    public void sendSms(String recipient, String phoneNumber, String content, String bizType, Long bizId)
    {
        String params = "{\"content\":\"" + escapeJson(content) + "\"}";
        sendSms(bizType, phoneNumber, params, 1, null);
    }

    /**
     * 发送设备离线告警短信
     */
    public void sendDeviceOfflineAlert(Device device)
    {
        String content = "设备离线告警：设备「" + safeStr(device.getDeviceName()) + "」（IP: " + safeStr(device.getIpAddress())
                + "，位置: " + safeStr(device.getLocation()) + "）已离线，请及时排查处理。";
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                "DEVICE_OFFLINE", device.getDeviceId());
    }

    /**
     * 发送设备上线恢复短信
     */
    public void sendDeviceOnlineAlert(Device device)
    {
        String content = "设备上线通知：设备「" + safeStr(device.getDeviceName()) + "」（IP: " + safeStr(device.getIpAddress())
                + "，位置: " + safeStr(device.getLocation()) + "）已恢复正常在线状态。";
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                "DEVICE_ONLINE", device.getDeviceId());
    }

    /**
     * 发送设备维修通知短信
     */
    public void sendRepairAlert(Device device)
    {
        String content = "设备离线告警，设备：" + safeStr(device.getDeviceName())
                + "，已离线，请及时处理。";
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                "REPAIR", device.getDeviceId());
    }

    /**
     * 发送访客审批通过短信
     */
    public void sendVisitorApprovalSms(VisitorAppointment appointment)
    {
        String passCode = appointment.getPassCode() != null ? appointment.getPassCode() : "";
        String content = "访客审批通知：" + safeStr(appointment.getVisitorName()) + "您好，欢迎来访！"
                + "被访人：" + safeStr(appointment.getHostName())
                + "（" + safeStr(appointment.getHostDept()) + "），"
                + "通行码：" + passCode
                + "，通行链接：http://1.94.26.126:3000/pass/" + passCode;
        sendSms(appointment.getVisitorName(), appointment.getVisitorPhone(), content,
                "VISITOR_APPROVE", appointment.getAppointmentId());
    }

    /**
     * 发送测试短信
     */
    public void sendTestSms(String phoneNumber)
    {
        String content = "测试短信，验证码：" + (int) (Math.random() * 9000 + 1000) + "，您正在测试短信发送功能。";
        sendSms("测试用户", phoneNumber, content, "TEST", null);
    }

    private String escapeJson(String str)
    {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String safeStr(String str)
    {
        return str != null ? str : "未知";
    }
}
