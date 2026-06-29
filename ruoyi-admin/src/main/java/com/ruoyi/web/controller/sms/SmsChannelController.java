package com.ruoyi.web.controller.sms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.sms.SmsUtil;

/**
 * 短信渠道配置 Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/sms/channel")
public class SmsChannelController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private SmsUtil smsUtil;

    /**
     * 查询渠道列表
     */
    @PreAuthorize("@ss.hasPermi('sms:channel:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        startPage();
        String sql = "SELECT * FROM sys_sms_channel WHERE del_flag='0' ORDER BY channel_id DESC";
        return getDataTable(jdbc.queryForList(sql));
    }

    /**
     * 根据渠道ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('sms:channel:query')")
    @GetMapping("/{channelId}")
    public AjaxResult getInfo(@PathVariable Long channelId)
    {
        Map<String, Object> row = queryOne("SELECT * FROM sys_sms_channel WHERE channel_id=? AND del_flag='0'", channelId);
        return row != null ? success(row) : error("渠道不存在");
    }

    /**
     * 新增渠道
     */
    @PreAuthorize("@ss.hasPermi('sms:channel:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> data)
    {
        data.remove("channel_id");
        data.put("create_by", getUsername());
        data.put("create_time", new Date());
        data.put("del_flag", "0");
        StringBuilder cols = new StringBuilder();
        StringBuilder phs = new StringBuilder();
        List<Object> vals = new ArrayList<>();
        for (Map.Entry<String, Object> e : data.entrySet())
        {
            cols.append(e.getKey()).append(",");
            phs.append("?,");
            vals.add(e.getValue());
        }
        if (vals.isEmpty())
        {
            return error("参数不能为空");
        }
        cols.setLength(cols.length() - 1);
        phs.setLength(phs.length() - 1);
        jdbc.update("INSERT INTO sys_sms_channel(" + cols + ") VALUES(" + phs + ")", vals.toArray());
        return success();
    }

    /**
     * 修改渠道
     */
    @PreAuthorize("@ss.hasPermi('sms:channel:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> data)
    {
        Object channelId = data.remove("channel_id");
        if (channelId == null)
        {
            return error("渠道ID不能为空");
        }
        data.put("update_by", getUsername());
        data.put("update_time", new Date());
        StringBuilder setClause = new StringBuilder();
        List<Object> vals = new ArrayList<>();
        for (Map.Entry<String, Object> e : data.entrySet())
        {
            setClause.append(e.getKey()).append("=?,");
            vals.add(e.getValue());
        }
        if (vals.isEmpty())
        {
            return error("参数不能为空");
        }
        setClause.setLength(setClause.length() - 1);
        vals.add(channelId);
        jdbc.update("UPDATE sys_sms_channel SET " + setClause + " WHERE channel_id=?", vals.toArray());
        return success();
    }

    /**
     * 测试发送
     */
    @PreAuthorize("@ss.hasPermi('sms:channel:test')")
    @PostMapping("/test/{channelId}")
    public AjaxResult testSend(@PathVariable Long channelId, @RequestBody Map<String, Object> body)
    {
        try {
            String phone = (String) body.get("phone");
            String params = (String) body.get("params");
            Long stId = body.get("stId") != null ? ((Number) body.get("stId")).longValue() : null;
            if (StringUtils.isEmpty(phone)) return error("手机号不能为空");

            Map<String, Object> channel = jdbc.queryForMap(
                "SELECT * FROM sys_sms_channel WHERE channel_id=? AND del_flag='0'", channelId);

            Map<String, Object> template = null;
            if (stId != null) {
                template = jdbc.queryForMap(
                    "SELECT * FROM sys_sms_sign_template WHERE st_id=? AND del_flag='0'", stId);
            } else {
                try {
                    template = jdbc.queryForMap(
                        "SELECT * FROM sys_sms_sign_template WHERE channel_id=? AND del_flag='0' AND status='0' LIMIT 1", channelId);
                } catch (Exception ignored) {}
            }

            if (template == null) return error("未找到可用签名模板，请先配置");

            // 黑名单检查
            try {
                Map<String, Object> black = jdbc.queryForMap(
                    "SELECT * FROM sys_sms_blacklist WHERE phone=? AND status='0'", phone);
                if (black != null) return error("该手机号在黑名单中");
            } catch (Exception ignored) {}

            boolean ok = smsUtil.sendByChannel(channel, template, phone, params != null ? params : "{}");
            return ok ? success("发送成功") : error("发送失败，请查看日志");
        } catch (Exception e) {
            return error("发送异常: " + e.getMessage());
        }
    }

    /**
     * 删除渠道（逻辑删除）
     */
    @PreAuthorize("@ss.hasPermi('sms:channel:remove')")
    @DeleteMapping("/{channelIds}")
    public AjaxResult remove(@PathVariable Long[] channelIds)
    {
        for (Long id : channelIds)
        {
            jdbc.update("UPDATE sys_sms_channel SET del_flag='1', update_time=NOW() WHERE channel_id=?", id);
        }
        return success();
    }

    private Map<String, Object> queryOne(String sql, Object... args)
    {
        List<Map<String, Object>> list = jdbc.queryForList(sql, args);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}
