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

/**
 * 短信黑名单 Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/sms/blacklist")
public class SmsBlacklistController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 查询黑名单列表
     */
    @PreAuthorize("@ss.hasPermi('sms:blacklist:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        startPage();
        String sql = "SELECT * FROM sys_sms_blacklist WHERE del_flag='0' ORDER BY create_time DESC";
        return getDataTable(jdbc.queryForList(sql));
    }

    /**
     * 根据手机号获取黑名单详情
     */
    @PreAuthorize("@ss.hasPermi('sms:blacklist:query')")
    @GetMapping("/{phone}")
    public AjaxResult getInfo(@PathVariable String phone)
    {
        Map<String, Object> row = queryOne("SELECT * FROM sys_sms_blacklist WHERE phone=? AND del_flag='0'", phone);
        return row != null ? success(row) : error("黑名单记录不存在");
    }

    /**
     * 新增黑名单
     */
    @PreAuthorize("@ss.hasPermi('sms:blacklist:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> data)
    {
        Object phone = data.get("phone");
        if (phone == null || phone.toString().trim().isEmpty())
        {
            return error("手机号不能为空");
        }
        // 检查是否已存在
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM sys_sms_blacklist WHERE phone=? AND del_flag='0'",
                Integer.class, phone.toString().trim());
        if (count != null && count > 0)
        {
            return error("该手机号已在黑名单中");
        }
        data.put("phone", phone.toString().trim());
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
        jdbc.update("INSERT INTO sys_sms_blacklist(" + cols + ") VALUES(" + phs + ")", vals.toArray());
        return success();
    }

    /**
     * 修改黑名单
     */
    @PreAuthorize("@ss.hasPermi('sms:blacklist:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> data)
    {
        Object phone = data.remove("phone");
        if (phone == null || phone.toString().trim().isEmpty())
        {
            return error("手机号不能为空");
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
        vals.add(phone.toString().trim());
        jdbc.update("UPDATE sys_sms_blacklist SET " + setClause + " WHERE phone=?", vals.toArray());
        return success();
    }

    /**
     * 删除黑名单（逻辑删除）
     */
    @PreAuthorize("@ss.hasPermi('sms:blacklist:remove')")
    @DeleteMapping("/{phone}")
    public AjaxResult remove(@PathVariable String phone)
    {
        int rows = jdbc.update("UPDATE sys_sms_blacklist SET del_flag='1', update_time=NOW() WHERE phone=? AND del_flag='0'", phone);
        return rows > 0 ? success() : error("黑名单记录不存在");
    }

    private Map<String, Object> queryOne(String sql, Object... args)
    {
        List<Map<String, Object>> list = jdbc.queryForList(sql, args);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}
