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
 * 短信签名模板 Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/sms/signtemplate")
public class SmsSignTemplateController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 查询签名模板列表
     */
    @PreAuthorize("@ss.hasPermi('sms:signtemplate:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        startPage();
        String sql = "SELECT * FROM sys_sms_sign_template WHERE del_flag='0' ORDER BY st_id DESC";
        return getDataTable(jdbc.queryForList(sql));
    }

    /**
     * 根据签名模板ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('sms:signtemplate:query')")
    @GetMapping("/{stId}")
    public AjaxResult getInfo(@PathVariable Long stId)
    {
        Map<String, Object> row = queryOne("SELECT * FROM sys_sms_sign_template WHERE st_id=? AND del_flag='0'", stId);
        return row != null ? success(row) : error("签名模板不存在");
    }

    /**
     * 新增签名模板
     */
    @PreAuthorize("@ss.hasPermi('sms:signtemplate:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> data)
    {
        data.remove("st_id");
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
        jdbc.update("INSERT INTO sys_sms_sign_template(" + cols + ") VALUES(" + phs + ")", vals.toArray());
        return success();
    }

    /**
     * 修改签名模板
     */
    @PreAuthorize("@ss.hasPermi('sms:signtemplate:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> data)
    {
        Object stId = data.remove("st_id");
        if (stId == null)
        {
            return error("签名模板ID不能为空");
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
        vals.add(stId);
        jdbc.update("UPDATE sys_sms_sign_template SET " + setClause + " WHERE st_id=?", vals.toArray());
        return success();
    }

    /**
     * 删除签名模板（逻辑删除）
     */
    @PreAuthorize("@ss.hasPermi('sms:signtemplate:remove')")
    @DeleteMapping("/{stIds}")
    public AjaxResult remove(@PathVariable Long[] stIds)
    {
        for (Long id : stIds)
        {
            jdbc.update("UPDATE sys_sms_sign_template SET del_flag='1', update_time=NOW() WHERE st_id=?", id);
        }
        return success();
    }

    private Map<String, Object> queryOne(String sql, Object... args)
    {
        List<Map<String, Object>> list = jdbc.queryForList(sql, args);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}
