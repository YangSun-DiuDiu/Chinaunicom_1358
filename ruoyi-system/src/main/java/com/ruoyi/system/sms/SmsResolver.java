package com.ruoyi.system.sms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/**
 * 短信解析器 —— 「biz_code → 签名模板 → 渠道 → 驱动」解析链的唯一实现（唯一 seam）
 *
 * <p>sendSms（即时）、executeScheduledTasks（定时）、retry（重发）三条路径共用本解析，
 * 消除三处重复的 4 段查询链；驱动解析与实例缓存亦收敛于此。
 * 新厂商接入 = 在 {@link #resolveDriver} 注册一个新的 adapter。</p>
 */
@Component
public class SmsResolver {

    private static final Logger log = LoggerFactory.getLogger(SmsResolver.class);

    @Autowired
    private JdbcTemplate jdbc;

    /** 驱动缓存（单例复用） */
    private final Map<String, SmsDriver> driverCache = new HashMap<>();

    /**
     * 解析业务编码为可执行的发送计划
     *
     * @param bizCode 业务编码（sys_sms_biz.biz_code）
     * @return 解析结果；失败时 {@link SmsPlan#isOk()} 为 false 且 errorMsg 描述失败环节
     */
    public SmsPlan resolve(String bizCode) {
        // 1. 查询业务绑定
        Map<String, Object> biz = queryOne("SELECT * FROM sys_sms_biz WHERE biz_code=? AND status='0' AND del_flag='0'", bizCode);
        if (biz == null) {
            log.error("业务编码不存在或已禁用: bizCode={}", bizCode);
            return SmsPlan.failed("业务编码不存在或已禁用");
        }

        // 2. 查询签名模板
        Long stId = getLong(biz, "st_id");
        Map<String, Object> st = queryOne("SELECT * FROM sys_sms_sign_template WHERE st_id=? AND status='0' AND del_flag='0'", stId);
        if (st == null) {
            log.error("签名模板不存在或已禁用: st_id={}", stId);
            return SmsPlan.failed("签名模板不存在或已禁用");
        }

        // 3. 查询主渠道
        Long channelId = getLong(st, "channel_id");
        Map<String, Object> channel = queryOne("SELECT * FROM sys_sms_channel WHERE channel_id=? AND status='0' AND del_flag='0'", channelId);
        if (channel == null) {
            log.error("短信渠道不存在或已禁用: channel_id={}", channelId);
            return SmsPlan.failed("短信渠道不存在或已禁用");
        }

        // 4. 解析驱动
        String channelType = getString(channel, "channel_type");
        SmsDriver driver = resolveDriver(channelType, channel);
        if (driver == null) {
            log.error("无法解析短信驱动: channelType={}", channelType);
            return SmsPlan.failed("无法解析驱动：" + channelType);
        }

        return new SmsPlan(biz, st, channel, channelType, driver, null);
    }

    /**
     * 查询备用渠道（主渠道发送失败时故障切换用）
     */
    public Map<String, Object> resolveBackupChannel(SmsPlan plan) {
        Long backupChannelId = getLong(plan.getBiz(), "backup_channel_id");
        if (backupChannelId == null || backupChannelId <= 0) {
            return null;
        }
        return queryOne("SELECT * FROM sys_sms_channel WHERE channel_id=? AND status='0' AND del_flag='0'", backupChannelId);
    }

    /**
     * 根据渠道类型解析驱动实例（带缓存）
     */
    public SmsDriver resolveDriver(String channelType, Map<String, Object> channel) {
        if (StringUtils.isEmpty(channelType)) {
            return null;
        }
        String key = channelType.toUpperCase();
        SmsDriver cached = driverCache.get(key);
        if (cached != null) {
            return cached;
        }
        SmsDriver driver;
        switch (key) {
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
     * 查询单行记录
     */
    private Map<String, Object> queryOne(String sql, Object... args) {
        try {
            List<Map<String, Object>> list = jdbc.queryForList(sql, args);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.error("数据库查询异常: sql={}", sql, e);
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map != null ? map.get(key) : null;
        return val != null ? val.toString() : "";
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map != null ? map.get(key) : null;
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val != null) {
            try {
                return Long.parseLong(val.toString());
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return null;
    }
}
