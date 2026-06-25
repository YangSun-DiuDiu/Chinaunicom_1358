package com.ruoyi.web.controller.smart;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/smart/access/permission")
public class SmartPermissionController extends BaseController
{
    @Autowired private JdbcTemplate jdbc;

    // ==================== 人员通行权限 ====================
    @GetMapping("/person/list")
    @PreAuthorize("@ss.hasPermi('smart:personPerm:list')")
    public TableDataInfo personList() {
        startPage();
        return getDataTable(jdbc.queryForList("select * from iot_person_access_permission order by create_time desc"));
    }

    @PostMapping("/person")
    @PreAuthorize("@ss.hasPermi('smart:personPerm:add')")
    public AjaxResult personAdd(@RequestBody Map<String,Object> data) {
        data.put("create_time", new Date());
        StringBuilder cols=new StringBuilder(),vals=new StringBuilder();
        List<Object> params=new ArrayList<>();
        data.forEach((k,v)->{if(!"permId".equals(k)){cols.append(k).append(",");vals.append("?,");params.add(v);}});
        cols.setLength(cols.length()-1); vals.setLength(vals.length()-1);
        jdbc.update("insert into iot_person_access_permission("+cols+") values("+vals+")",params.toArray());
        return success();
    }

    @DeleteMapping("/person/{id}")
    @PreAuthorize("@ss.hasPermi('smart:personPerm:remove')")
    public AjaxResult personDel(@PathVariable Long id) {
        jdbc.update("delete from iot_person_access_permission where perm_id=?", id);
        return success();
    }

    // ==================== 车辆通行权限 ====================
    @GetMapping("/vehicle/list")
    @PreAuthorize("@ss.hasPermi('smart:vehiclePerm:list')")
    public TableDataInfo vehicleList() {
        startPage();
        return getDataTable(jdbc.queryForList("select * from iot_vehicle_access_permission order by create_time desc"));
    }

    @PostMapping("/vehicle")
    @PreAuthorize("@ss.hasPermi('smart:vehiclePerm:add')")
    public AjaxResult vehicleAdd(@RequestBody Map<String,Object> data) {
        data.put("create_time", new Date());
        StringBuilder cols=new StringBuilder(),vals=new StringBuilder();
        List<Object> params=new ArrayList<>();
        data.forEach((k,v)->{if(!"permId".equals(k)){cols.append(k).append(",");vals.append("?,");params.add(v);}});
        cols.setLength(cols.length()-1); vals.setLength(vals.length()-1);
        jdbc.update("insert into iot_vehicle_access_permission("+cols+") values("+vals+")",params.toArray());
        return success();
    }

    @DeleteMapping("/vehicle/{id}")
    @PreAuthorize("@ss.hasPermi('smart:vehiclePerm:remove')")
    public AjaxResult vehicleDel(@PathVariable Long id) {
        jdbc.update("delete from iot_vehicle_access_permission where perm_id=?", id);
        return success();
    }

    // ==================== 人员车辆绑定 ====================
    @GetMapping("/vehicle-binding/list")
    public TableDataInfo bindingList() {
        startPage();
        return getDataTable(jdbc.queryForList("select * from iot_person_vehicle order by create_time desc"));
    }

    @PostMapping("/vehicle-binding")
    public AjaxResult bindingAdd(@RequestBody Map<String,Object> data) {
        data.put("create_time", new Date());
        StringBuilder cols=new StringBuilder(),vals=new StringBuilder();
        List<Object> params=new ArrayList<>();
        data.forEach((k,v)->{if(!"bindId".equals(k)){cols.append(k).append(",");vals.append("?,");params.add(v);}});
        cols.setLength(cols.length()-1); vals.setLength(vals.length()-1);
        jdbc.update("insert into iot_person_vehicle("+cols+") values("+vals+")",params.toArray());
        return success();
    }

    @DeleteMapping("/vehicle-binding/{id}")
    public AjaxResult bindingDel(@PathVariable Long id) {
        jdbc.update("delete from iot_person_vehicle where bind_id=?", id);
        return success();
    }
}
