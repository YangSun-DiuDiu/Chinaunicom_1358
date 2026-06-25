package com.ruoyi.web.controller.device;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/device/parts")
public class DevicePartsController extends BaseController
{
    @Autowired private JdbcTemplate jdbc;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('device:parts:list')")
    public TableDataInfo list(@RequestParam(required=false) String partName) {
        startPage();
        String sql = "select * from iot_parts where 1=1 ";
        if(partName!=null && !partName.isEmpty()) { sql+="and part_name like ? "; return getDataTable(jdbc.queryForList(sql,"%"+partName+"%")); }
        return getDataTable(jdbc.queryForList(sql+"order by create_time desc"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('device:parts:query')")
    public AjaxResult get(@PathVariable Long id) { return success(jdbc.queryForMap("select * from iot_parts where part_id=?",id)); }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('device:parts:add')")
    public AjaxResult add(@RequestBody Map<String,Object> data) {
        data.put("create_time",new Date());
        buildInsert("iot_parts",data,"part_id"); return success();
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('device:parts:edit')")
    public AjaxResult edit(@RequestBody Map<String,Object> data) {
        if(data.containsKey("part_id")) buildUpdate("iot_parts",data,"part_id");
        return success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('device:parts:remove')")
    public AjaxResult del(@PathVariable Long id) { jdbc.update("delete from iot_parts where part_id=?",id); return success(); }

    @GetMapping("/usage/list")
    public TableDataInfo usageList() {
        startPage();
        return getDataTable(jdbc.queryForList(
            "select u.*, r.repair_no from iot_parts_usage u " +
            "left join iot_device_repair r on u.repair_id = r.repair_id " +
            "order by u.used_time desc"));
    }

    private void buildInsert(String table, Map<String,Object> data, String idCol) {
        StringBuilder cols=new StringBuilder(),phs=new StringBuilder();
        List<Object> vals=new ArrayList<>();
        data.forEach((k,v)->{ if(!idCol.equals(k)){ cols.append(k).append(","); phs.append("?,"); vals.add(v); } });
        cols.setLength(cols.length()-1); phs.setLength(phs.length()-1);
        jdbc.update("insert into "+table+"("+cols+") values("+phs+")",vals.toArray());
    }

    private void buildUpdate(String table, Map<String,Object> data, String idCol) {
        StringBuilder sb=new StringBuilder("update "+table+" set ");
        List<Object> vals=new ArrayList<>();
        data.forEach((k,v)->{ if(!idCol.equals(k)){ sb.append(k).append("=?,"); vals.add(v); } });
        sb.setLength(sb.length()-1); sb.append(" where ").append(idCol).append("=?");
        vals.add(data.get(idCol));
        jdbc.update(sb.toString(),vals.toArray());
    }
}
