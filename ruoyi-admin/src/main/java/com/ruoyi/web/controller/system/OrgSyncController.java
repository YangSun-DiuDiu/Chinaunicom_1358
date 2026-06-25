package com.ruoyi.web.controller.system;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.impl.sync.DingTalkApiService;
import com.ruoyi.system.service.impl.sync.WeComApiService;

/**
 * 组织架构同步控制器 — 从钉钉/企业微信导入部门与人员
 *
 * <h3>钉钉导入数据格式</h3>
 * <pre>
 * {
 *   "departments": [
 *     { "dept_id": 1, "name": "总公司", "parent_id": 0 },
 *     { "dept_id": 2, "name": "技术部", "parent_id": 1 }
 *   ],
 *   "users": [
 *     { "userid": "zhangsan", "name": "张三", "dept_id_list": [2], "mobile": "13800001111",
 *       "email": "zhangsan@example.com", "title": "工程师", "hire_date": "2024-01-15" }
 *   ]
 * }
 * </pre>
 *
 * <h3>企业微信导入数据格式</h3>
 * <pre>
 * {
 *   "departments": [
 *     { "id": 1, "name": "总公司", "parentid": 0, "order": 1 },
 *     { "id": 2, "name": "技术部", "parentid": 1, "order": 2 }
 *   ],
 *   "users": [
 *     { "userid": "zhangsan", "name": "张三", "department": [2], "mobile": "13800001111",
 *       "email": "zhangsan@example.com", "position": "工程师" }
 *   ]
 * }
 * </pre>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/orgSync")
public class OrgSyncController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(OrgSyncController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DingTalkApiService dingTalkApi;

    @Autowired
    private WeComApiService weComApi;

    /**
     * 通过API实时同步钉钉组织架构（直接调用钉钉开放平台API）
     */
    @PostMapping("/apiSync/dingtalk")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult apiSyncDingtalk()
    {
        long start = System.currentTimeMillis();
        try
        {
            // 1. 获取access_token
            String token = dingTalkApi.getAccessToken();
            if (token == null) return error("无法获取钉钉access_token，请检查AppKey/AppSecret配置");

            // 2. 获取所有部门
            List<Map<String, Object>> departments = dingTalkApi.getAllDepartments(token);
            log.info("钉钉API获取到{}个部门", departments.size());

            // 3. 获取所有用户
            List<Map<String, Object>> users = dingTalkApi.getAllUsers(token, departments);
            log.info("钉钉API获取到{}个用户", users.size());

            // 4. 构造导入body并调用导入
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("departments", departments);
            body.put("users", users);

            AjaxResult result = doImport(body, "DINGTALK");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null)
            {
                data.put("apiDeptCount", departments.size());
                data.put("apiUserCount", users.size());
                data.put("totalCostMs", System.currentTimeMillis() - start);
            }
            return result;
        }
        catch (Exception e)
        {
            log.error("钉钉API同步异常", e);
            return error("钉钉API同步失败: " + e.getMessage());
        }
    }

    /**
     * 通过API实时同步企业微信组织架构（直接调用企业微信API）
     */
    @PostMapping("/apiSync/wecom")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult apiSyncWecom()
    {
        long start = System.currentTimeMillis();
        try
        {
            // 1. 获取access_token
            String token = weComApi.getAccessToken();
            if (token == null) return error("无法获取企业微信access_token，请检查CorpID/Secret配置");

            // 2. 获取所有部门
            List<Map<String, Object>> departments = weComApi.getAllDepartments(token);
            log.info("企业微信API获取到{}个部门", departments.size());

            // 3. 获取所有用户
            List<Map<String, Object>> users = weComApi.getAllUsers(token, departments);
            log.info("企业微信API获取到{}个用户", users.size());

            // 4. 构造导入body并调用导入
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("departments", departments);
            body.put("users", users);

            AjaxResult result = doImport(body, "WECHAT");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null)
            {
                data.put("apiDeptCount", departments.size());
                data.put("apiUserCount", users.size());
                data.put("totalCostMs", System.currentTimeMillis() - start);
            }
            return result;
        }
        catch (Exception e)
        {
            log.error("企业微信API同步异常", e);
            return error("企业微信API同步失败: " + e.getMessage());
        }
    }

    /**
     * 导入钉钉组织架构（JSON手动录入）
     */
    @PostMapping("/dingtalk")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult importDingtalk(@RequestBody Map<String, Object> body)
    {
        return doImport(body, "DINGTALK");
    }

    /**
     * 导入企业微信组织架构
     */
    @PostMapping("/wecom")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult importWecom(@RequestBody Map<String, Object> body)
    {
        return doImport(body, "WECHAT");
    }

    /**
     * 预览导入数据（不入库，仅解析）
     */
    @PostMapping("/preview")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult preview(@RequestBody Map<String, Object> body)
    {
        String platform = getStr(body, "platform");
        if (platform.isEmpty()) return error("请指定platform");

        Map<String, Object> result = new LinkedHashMap<>();
        try
        {
            List<Map<String, Object>> deptList = parseDepartments(body, platform);
            List<Map<String, Object>> userList = parseUsers(body, platform);
            result.put("deptCount", deptList.size());
            result.put("userCount", userList.size());
            result.put("departments", deptList);
            result.put("users", userList);
            return success(result);
        }
        catch (Exception e)
        {
            return error("解析失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private AjaxResult doImport(Map<String, Object> body, String platform)
    {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        int deptImported = 0, deptSkipped = 0;
        int userImported = 0, userSkipped = 0;

        try
        {
            // 1. 解析部门
            List<Map<String, Object>> deptList = parseDepartments(body, platform);

            // 按 parent 排序，确保父部门先创建
            deptList.sort((a, b) -> {
                String pa = getStr(a, platform.equals("WECHAT") ? "parentid" : "parent_id");
                String pb = getStr(b, platform.equals("WECHAT") ? "parentid" : "parent_id");
                return pa.compareTo(pb);
            });

            Map<String, Long> extDeptIdMap = new HashMap<>(); // external_id → local dept_id

            for (Map<String, Object> d : deptList)
            {
                String extId = getStr(d, platform.equals("WECHAT") ? "id" : "dept_id");
                String name = getStr(d, "name");
                String extParentId = getStr(d, platform.equals("WECHAT") ? "parentid" : "parent_id");
                String order = getStr(d, "order");

                if (extId.isEmpty() || name.isEmpty()) { deptSkipped++; continue; }

                // 已存在则跳过
                List<Map<String, Object>> exist = jdbc.queryForList(
                    "SELECT dept_id FROM sys_dept WHERE external_dept_id=? AND external_source=? AND del_flag='0'",
                    extId, platform);
                if (!exist.isEmpty())
                {
                    extDeptIdMap.put(extId, ((Number)exist.get(0).get("dept_id")).longValue());
                    deptSkipped++;
                    continue;
                }

                // 查找父部门
                Long parentId = 100L; // 默认根部门
                if (!extParentId.isEmpty() && !"0".equals(extParentId))
                {
                    Long localParent = extDeptIdMap.get(extParentId);
                    if (localParent == null)
                    {
                        // 尝试从DB查找
                        List<Map<String, Object>> p = jdbc.queryForList(
                            "SELECT dept_id FROM sys_dept WHERE external_dept_id=? AND external_source=?",
                            extParentId, platform);
                        if (!p.isEmpty()) localParent = ((Number)p.get(0).get("dept_id")).longValue();
                    }
                    if (localParent != null) parentId = localParent;
                }

                int orderNum = 0;
                try { orderNum = Integer.parseInt(order); } catch (Exception e) { }

                // 扩展字段
                String deptCode = getStr(d, "dept_code");

                jdbc.update(
                    "INSERT INTO sys_dept (parent_id, ancestors, dept_name, dept_code, order_num, leader, phone, email, " +
                    "status, del_flag, external_dept_id, external_source, external_parent_id, dept_type, create_by, create_time) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    parentId, "", name, deptCode, orderNum, "", "", "", "0", "0", extId, platform, extParentId, platform,
                    SecurityUtils.getUsername());

                // 获取自增ID
                Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                extDeptIdMap.put(extId, newId);

                // 更新ancestors
                if (parentId != 100L)
                {
                    String ancestors = jdbc.queryForObject(
                        "SELECT CONCAT(ancestors, ',', dept_id) FROM sys_dept WHERE dept_id=?", String.class, parentId);
                    jdbc.update("UPDATE sys_dept SET ancestors=? WHERE dept_id=?", ancestors, newId);
                }
                else
                {
                    jdbc.update("UPDATE sys_dept SET ancestors=? WHERE dept_id=?", String.valueOf(parentId), newId);
                }

                deptImported++;
            }

            // 2. 解析用户
            List<Map<String, Object>> userList = parseUsers(body, platform);

            for (Map<String, Object> u : userList)
            {
                String extUserId = getStr(u, "userid");
                String name = getStr(u, "name");
                String mobile = getStr(u, "mobile");
                String email = getStr(u, "email");
                String title = getStr(u, platform.equals("WECHAT") ? "position" : "title");
                String hireDate = getStr(u, "hire_date");

                if (extUserId.isEmpty() || name.isEmpty()) { userSkipped++; continue; }

                // 已存在则跳过
                List<Map<String, Object>> exist = jdbc.queryForList(
                    "SELECT user_id FROM sys_user WHERE external_user_id=? AND external_source=? AND del_flag='0'",
                    extUserId, platform);
                if (!exist.isEmpty()) { userSkipped++; continue; }

                // 查找部门
                List<Object> deptIds = new ArrayList<>();
                Object deptRaw = u.get(platform.equals("WECHAT") ? "department" : "dept_id_list");
                if (deptRaw instanceof List)
                {
                    for (Object item : (List<?>) deptRaw)
                    {
                        String extDeptId = String.valueOf(item);
                        Long localDeptId = extDeptIdMap.get(extDeptId);
                        if (localDeptId == null)
                        {
                            List<Map<String, Object>> dp = jdbc.queryForList(
                                "SELECT dept_id FROM sys_dept WHERE external_dept_id=? AND external_source=?",
                                extDeptId, platform);
                            if (!dp.isEmpty()) localDeptId = ((Number)dp.get(0).get("dept_id")).longValue();
                        }
                        if (localDeptId != null) deptIds.add(localDeptId);
                    }
                }
                else if (deptRaw instanceof Number)
                {
                    String extDeptId = String.valueOf(deptRaw);
                    Long localDeptId = extDeptIdMap.get(extDeptId);
                    if (localDeptId == null)
                    {
                        List<Map<String, Object>> dp = jdbc.queryForList(
                            "SELECT dept_id FROM sys_dept WHERE external_dept_id=? AND external_source=?",
                            extDeptId, platform);
                        if (!dp.isEmpty()) localDeptId = ((Number)dp.get(0).get("dept_id")).longValue();
                    }
                    if (localDeptId != null) deptIds.add(localDeptId);
                }

                Long deptId = deptIds.isEmpty() ? 103L : ((Number)deptIds.get(0)).longValue();

                // 生成登录账号
                String userName = extUserId.length() <= 30 ? extUserId : extUserId.substring(0, 30);

                // 提取扩展字段（钉钉/企业微信差异字段）
                String jobNumber = getStr(u, "job_number");           // 钉钉工号
                String alias = getStr(u, "alias");                    // 企业微信别名
                String orgEmail = getStr(u, "org_email");             // 钉钉企业邮箱
                String telephone = getStr(u, platform.equals("WECHAT") ? "telephone" : "tel"); // 座机
                String workPlace = getStr(u, "work_place");           // 钉钉工作地点
                String address = getStr(u, "address");                // 企业微信地址
                String avatarUrl = getStr(u, "avatar");               // 头像URL
                String gender = getStr(u, "gender");                  // 性别(企微:1男2女)

                // 未映射的扩展字段存入ext_attr JSON
                Map<String, Object> extAttrMap = new LinkedHashMap<>();
                for (String key : u.keySet())
                {
                    // 跳过已映射的字段
                    if (java.util.Arrays.asList("userid","name","mobile","email","title","position",
                        "hire_date","dept_id_list","department","job_number","alias","org_email",
                        "tel","telephone","work_place","address","avatar","gender","user_id").contains(key))
                        continue;
                    extAttrMap.put(key, u.get(key));
                }
                String extAttrJson = extAttrMap.isEmpty() ? "" : mapper.writeValueAsString(extAttrMap);

                // 性别映射
                String sex = "2"; // 未知
                if ("1".equals(gender) || "男".equals(gender)) sex = "0";
                else if ("2".equals(gender) || "女".equals(gender)) sex = "1";

                jdbc.update(
                    "INSERT INTO sys_user (dept_id, user_name, job_number, nick_name, alias_name, " +
                    "external_user_id, external_source, external_dept_id, " +
                    "email, org_email, phonenumber, telephone, sex, " +
                    "position, hire_date, work_place, address, avatar_url, ext_attr, " +
                    "password, status, del_flag, create_by, create_time) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    deptId, userName, jobNumber, name, alias,
                    extUserId, platform, deptIds.isEmpty() ? "" : String.valueOf(deptIds.get(0)),
                    email, orgEmail, mobile, telephone, sex,
                    title, hireDate.isEmpty() ? null : java.sql.Date.valueOf(hireDate),
                    workPlace, address, avatarUrl, extAttrJson,
                    SecurityUtils.encryptPassword("123456"), "0", "0",
                    SecurityUtils.getUsername());

                userImported++;
            }

            long cost = System.currentTimeMillis() - start;
            result.put("deptImported", deptImported);
            result.put("deptSkipped", deptSkipped);
            result.put("userImported", userImported);
            result.put("userSkipped", userSkipped);
            result.put("costMs", cost);

            log.info("组织架构导入完成: platform={}, dept={}/{}, user={}/{}, cost={}ms",
                platform, deptImported, deptSkipped, userImported, userSkipped, cost);

            return success(result);
        }
        catch (Exception e)
        {
            log.error("组织架构导入异常: platform={}, error={}", platform, e.getMessage(), e);
            return error("导入失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseDepartments(Map<String, Object> body, String platform)
    {
        Object deptsObj = body.get("departments");
        if (deptsObj instanceof List) return (List<Map<String, Object>>) deptsObj;

        // 兼容钉钉API原始返回格式
        Object resultObj = body.get("result");
        if (resultObj instanceof Map)
        {
            Object listObj = ((Map<?, ?>) resultObj).get(platform.equals("WECHAT") ? "department" : "dept_list");
            if (listObj instanceof List) return (List<Map<String, Object>>) listObj;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseUsers(Map<String, Object> body, String platform)
    {
        Object usersObj = body.get("users");
        if (usersObj instanceof List) return (List<Map<String, Object>>) usersObj;

        // 兼容钉钉API原始返回格式
        Object resultObj = body.get("result");
        if (resultObj instanceof Map)
        {
            Object listObj = ((Map<?, ?>) resultObj).get(platform.equals("WECHAT") ? "userlist" : "user_list");
            if (listObj instanceof List) return (List<Map<String, Object>>) listObj;
        }
        return new ArrayList<>();
    }

    /**
     * 获取API配置
     */
    @GetMapping("/apiConfig")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult getApiConfig()
    {
        List<Map<String, Object>> configs = jdbc.queryForList(
            "SELECT config_key, config_value, config_type, description FROM attendance_config " +
            "WHERE status='0' AND config_type IN ('DINGTALK','WECHAT') ORDER BY config_type, config_key");
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> c : configs)
        {
            result.put(String.valueOf(c.get("config_key")), String.valueOf(c.get("config_value")));
        }
        return success(result);
    }

    /**
     * 保存API配置
     */
    @PostMapping("/apiConfig")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public AjaxResult saveApiConfig(@RequestBody Map<String, String> body)
    {
        for (Map.Entry<String, String> e : body.entrySet())
        {
            String key = e.getKey();
            String value = e.getValue() != null ? e.getValue() : "";
            if (!key.startsWith("dingtalk.") && !key.startsWith("wecom.")) continue;

            int updated = jdbc.update(
                "UPDATE attendance_config SET config_value=?, update_time=NOW() WHERE config_key=?",
                value, key);
            if (updated == 0)
            {
                String type = key.startsWith("wecom.") ? "WECHAT" : "DINGTALK";
                jdbc.update(
                    "INSERT INTO attendance_config (config_key, config_value, config_type, description, status, create_by, create_time) " +
                    "VALUES (?,?,?,?,?,?,NOW())",
                    key, value, type, "用户配置", "0", SecurityUtils.getUsername());
            }
        }
        return success();
    }

    private String getStr(Map<String, Object> map, String key)
    {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : "";
    }
}
