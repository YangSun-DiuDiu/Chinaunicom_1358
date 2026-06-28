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
 * 短信业务绑定 Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/sms/biz")
public class SmsBizController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 查询业务绑定列表
     */
    @PreAuthorize("@ss.hasPermi('sms:biz:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        startPage();
        String sql = "SELECT * FROM sys_sms_biz WHERE del_flag='0' ORDER BY biz_id DESC";
        return getDataTable(jdbc.queryForList(sql));
    }

    /**
     * 根据业务ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('sms:biz:query')")
    @GetMapping("/{bizId}")
    public AjaxResult getInfo(@PathVariable Long bizId)
    {
        Map<String, Object> row = queryOne("SELECT * FROM sys_sms_biz WHERE biz_id=? AND del_flag='0'", bizId);
        return row != null ? success(row) : error("业务绑定不存在");
    }

    /**
     * 新增业务绑定
     */
    @PreAuthorize("@ss.hasPermi('sms:biz:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> data)
    {
        data.remove("biz_id");
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
        jdbc.update("INSERT INTO sys_sms_biz(" + cols + ") VALUES(" + phs + ")", vals.toArray());
        return success();
    }

    /**
     * 修改业务绑定
     */
    @PreAuthorize("@ss.hasPermi('sms:biz:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> data)
    {
        Object bizId = data.remove("biz_id");
        if (bizId == null)
        {
            return error("业务ID不能为空");
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
        vals.add(bizId);
        jdbc.update("UPDATE sys_sms_biz SET " + setClause + " WHERE biz_id=?", vals.toArray());
        return success();
    }

    /**
     * 删除业务绑定（逻辑删除）
     */
    @PreAuthorize("@ss.hasPermi('sms:biz:remove')")
    @DeleteMapping("/{bizIds}")
    public AjaxResult remove(@PathVariable Long[] bizIds)
    {
        for (Long id : bizIds)
        {
            jdbc.update("UPDATE sys_sms_biz SET del_flag='1', update_time=NOW() WHERE biz_id=?", id);
        }
        return success();
    }

    private Map<String, Object> queryOne(String sql, Object... args)
    {
        List<Map<String, Object>> list = jdbc.queryForList(sql, args);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}
