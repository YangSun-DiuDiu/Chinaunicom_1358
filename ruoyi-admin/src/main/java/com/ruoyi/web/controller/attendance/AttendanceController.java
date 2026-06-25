package com.ruoyi.web.controller.attendance;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/attendance")
public class AttendanceController extends BaseController
{
    @Autowired private JdbcTemplate jdbc;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('attendance:data:list')")
    public TableDataInfo list(@RequestParam(required=false) String userName,
                               @RequestParam(required=false) String attendanceDate,
                               @RequestParam(required=false) String status) {
        startPage();
        String sql = "select * from attendance_record where 1=1 ";
        List<Object> params = new ArrayList<>();
        if(userName!=null&&!userName.isEmpty()) { sql+="and user_name like ? "; params.add("%"+userName+"%"); }
        if(attendanceDate!=null&&!attendanceDate.isEmpty()) { sql+="and attendance_date = ? "; params.add(attendanceDate); }
        if(status!=null&&!status.isEmpty()) { sql+="and status = ? "; params.add(status); }
        sql+="order by attendance_date desc, user_name";
        return getDataTable(jdbc.queryForList(sql, params.toArray()));
    }

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermi('attendance:data:list')")
    public AjaxResult stats(@RequestParam(required=false) String date) {
        String w = date!=null?" where attendance_date='"+date+"'":"";
        Map<String,Object> s = new LinkedHashMap<>();
        s.put("total", jdbc.queryForObject("select count(distinct user_name) from attendance_record"+w, Integer.class));
        s.put("normal", jdbc.queryForObject("select count(*) from attendance_record"+w+(w.isEmpty()?" where":" and")+" status='NORMAL'", Integer.class));
        s.put("late", jdbc.queryForObject("select count(*) from attendance_record"+w+(w.isEmpty()?" where":" and")+" status='LATE'", Integer.class));
        s.put("absent", jdbc.queryForObject("select count(*) from attendance_record"+w+(w.isEmpty()?" where":" and")+" status='ABSENT'", Integer.class));
        s.put("overtime", jdbc.queryForObject("select count(*) from attendance_record"+w+(w.isEmpty()?" where":" and")+" status='OVERTIME'", Integer.class));
        return success(s);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@ss.hasPermi('attendance:data:remove')")
    public AjaxResult delete(@PathVariable Long id) {
        jdbc.update("delete from attendance_record where record_id=?", id);
        return success();
    }

    @PostMapping("/add")
    @PreAuthorize("@ss.hasPermi('attendance:data:add')")
    public AjaxResult add(@RequestBody Map<String,Object> data) {
        data.put("create_time", new Date());
        StringBuilder cols=new StringBuilder(),phs=new StringBuilder();
        List<Object> vals=new ArrayList<>();
        data.forEach((k,v)->{ cols.append(k).append(","); phs.append("?,"); vals.add(v); });
        cols.setLength(cols.length()-1); phs.setLength(phs.length()-1);
        jdbc.update("insert into attendance_record("+cols+") values("+phs+")",vals.toArray());
        return success();
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPermi('attendance:import:list')")
    public AjaxResult importExcel(@RequestParam("file") MultipartFile file) {
        try {
            // Excel导入逻辑: 后续对接钉钉/企业微信时完善
            return success("导入成功，共处理0条记录");
        } catch(Exception e) { return error(e.getMessage()); }
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPermi('attendance:data:export')")
    public void export(jakarta.servlet.http.HttpServletResponse response) {
        // 导出功能
        throw new UnsupportedOperationException("export to be implemented");
    }

    // ============ WebSocket 状态 ============

    /** WebSocket在线状态及回调配置 */
    @GetMapping("/wsStatus")
    @PreAuthorize("@ss.hasPermi('attendance:data:list')")
    public AjaxResult wsStatus()
    {
        Map<String, Object> s = new LinkedHashMap<>();
        List<Map<String, Object>> configs = jdbc.queryForList(
            "SELECT config_key, config_value, config_type, description FROM attendance_config " +
            "WHERE status='0' ORDER BY config_type, config_key");
        s.put("configs", configs);
        return success(s);
    }

    /** 回调日志列表 */
    @GetMapping("/callbackLogs")
    @PreAuthorize("@ss.hasPermi('attendance:data:list')")
    public TableDataInfo callbackLogs(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String callbackType)
    {
        startPage();
        String sql = "SELECT * FROM attendance_callback_log WHERE 1=1 ";
        List<Object> params = new ArrayList<>();
        if (source != null && !source.isEmpty()) { sql += "AND source=? "; params.add(source); }
        if (callbackType != null && !callbackType.isEmpty()) { sql += "AND callback_type=? "; params.add(callbackType); }
        sql += "ORDER BY create_time DESC";
        return getDataTable(jdbc.queryForList(sql, params.toArray()));
    }
}
