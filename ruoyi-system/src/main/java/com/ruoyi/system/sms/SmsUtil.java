package com.ruoyi.system.sms;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/**
 * 短信统一发送入口——五表驱动多厂商路由
 *
 * <p>解析链（biz → 签名模板 → 渠道 → 驱动）已收敛至 {@link SmsResolver}，
 * 即时发送 / 定时任务 / 重发三条路径共用同一解析 seam。</p>
 *
 * @author ruoyi
 */
@Component
public class SmsUtil
{
    private static final Logger log = LoggerFactory.getLogger(SmsUtil.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SmsResolver smsResolver;

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

        // 1. 解析发送计划（业务绑定 + 签名模板 + 渠道 + 驱动）
        SmsPlan plan = smsResolver.resolve(bizCode);
        if (!plan.isOk())
        {
            return false;
        }
        Map<String, Object> biz = plan.getBiz();
        Map<String, Object> st = plan.getSt();
        Map<String, Object> channel = plan.getChannel();
        String channelType = plan.getChannelType();

        // 2. 黑名单检查
        if (isBlacklisted(phone))
        {
            log.warn("手机号在黑名单中，拒绝发送: phone={}", phone);
            return false;
        }

        // 3. 频率限制检查
        int minuteLimit = getInt(biz, "minute_limit", 5);
        int dayLimit = getInt(biz, "day_limit", 100);
        if (!checkRateLimit(phone, minuteLimit, dayLimit))
        {
            log.warn("短信发送频率超限: phone={}, minuteLimit={}, dayLimit={}", phone, minuteLimit, dayLimit);
            return false;
        }

        // 4. 定时模式——仅入库
        if (sendMode == 2)
        {
            insertLog(bizCode, channelType, phone,
                    getString(st, "template_code"), getString(st, "sign_name"),
                    params, sendMode, sendTime, "WAIT", 0, "");
            log.info("定时短信已入库: bizCode={}, phone={}, sendTime={}", bizCode, phone, sendTime);
            return true;
        }

        // 5. 即时模式——路由驱动发送
        boolean success = sendWithRetry(plan.getDriver(), channel, st, phone, params,
                getInt(channel, "retry_count", 0));

        // 6. 故障切换至备用渠道
        if (!success)
        {
            Map<String, Object> backupChannel = smsResolver.resolveBackupChannel(plan);
            if (backupChannel != null)
            {
                log.info("主渠道发送失败，切换至备用渠道: channel_id={}", getLong(backupChannel, "channel_id"));
                SmsDriver backupDriver = smsResolver.resolveDriver(
                        getString(backupChannel, "channel_type"), backupChannel);
                if (backupDriver != null)
                {
                    success = sendWithRetry(backupDriver, backupChannel, st, phone, params,
                            getInt(backupChannel, "retry_count", 0));
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

            SmsPlan plan = smsResolver.resolve(bizCode);
            if (!plan.isOk())
            {
                jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg=?, update_time=NOW() WHERE log_id=?",
                        plan.getErrorMsg(), logId);
                continue;
            }

            boolean success = plan.getDriver().send(plan.getChannel(), plan.getSt(), phone, params);
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

        // 解析发送计划（业务绑定 + 模板 + 渠道 + 驱动）
        SmsPlan plan = smsResolver.resolve(bizCode);
        if (!plan.isOk())
        {
            jdbc.update("UPDATE sys_sms_log SET task_status='FAIL', result_msg=?, update_time=NOW() WHERE log_id=?",
                    plan.getErrorMsg(), logId);
            return false;
        }

        boolean success = plan.getDriver().send(plan.getChannel(), plan.getSt(), phone, params);
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
        SmsDriver driver = smsResolver.resolveDriver(channelType, channel);
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
     * 兼容旧版 sendSms(recipient, phoneNumber, content, bizType, bizId) 调用。
     *
     * <p>仅保留给测试入口（{@link #sendTestSms}）使用；生产链路请使用
     * {@link #sendSms(String, String, String, int, String)}（bizCode 模板驱动）。</p>
     */
    public void sendSms(String recipient, String phoneNumber, String content, String bizType, Long bizId)
    {
        String params = "{\"content\":\"" + escapeJson(content) + "\"}";
        sendSms(bizType, phoneNumber, params, 1, null);
    }

    /**
     * 发送测试短信（管理端「短信配置-测试发送」入口）
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
}
