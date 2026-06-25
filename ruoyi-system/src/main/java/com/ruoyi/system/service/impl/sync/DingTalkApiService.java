package com.ruoyi.system.service.impl.sync;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 钉钉开放平台API服务
 * 文档: https://open.dingtalk.com/document/orgapp-server
 *
 * @author ruoyi
 */
@Service
public class DingTalkApiService
{
    private static final Logger log = LoggerFactory.getLogger(DingTalkApiService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private JdbcTemplate jdbc;

    /** 获取API基础URL */
    private String getBaseUrl() {
        String url = getConfig("dingtalk.api_url");
        return url.isEmpty() ? "https://oapi.dingtalk.com" : url;
    }
    /** 获取AppKey */
    private String getAppKey() { return getConfig("dingtalk.app_key"); }
    /** 获取AppSecret */
    private String getAppSecret() { return getConfig("dingtalk.app_secret"); }

    private String getConfig(String key) {
        try {
            return jdbc.queryForObject(
                "SELECT config_value FROM attendance_config WHERE config_key=? AND status='0'",
                String.class, key);
        } catch (Exception e) { return ""; }
    }

    /**
     * 获取钉钉 access_token
     */
    public String getAccessToken()
    {
        String appKey = getAppKey();
        String appSecret = getAppSecret();
        if (appKey.isEmpty() || appSecret.isEmpty())
        {
            log.warn("钉钉AppKey或AppSecret未配置");
            return null;
        }

        try
        {
            String url = getBaseUrl() +"/gettoken?appkey=" + appKey + "&appsecret=" + appSecret;
            String resp = httpGet(url);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(resp, Map.class);
            if (0 == ((Number)result.get("errcode")).intValue())
            {
                return result.get("access_token").toString();
            }
            log.error("获取钉钉token失败: {}", resp);
        }
        catch (Exception e) { log.error("钉钉API调用异常", e); }
        return null;
    }

    /**
     * 获取子部门列表
     * @param token access_token
     * @param parentId 父部门ID，传null获取根部门
     * @return 部门列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDepartmentList(String token, Long parentId)
    {
        List<Map<String, Object>> allDepts = new ArrayList<>();
        try
        {
            String url = getBaseUrl() +"/topapi/v2/department/listsub";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("dept_id", parentId != null ? parentId : 1L);

            String resp = httpPost(url, mapper.writeValueAsString(body), token);
            Map<String, Object> result = mapper.readValue(resp, Map.class);
            if (0 == ((Number)result.get("errcode")).intValue())
            {
                List<Map<String, Object>> depts = (List<Map<String, Object>>) result.get("result");
                if (depts != null)
                {
                    // 为每个字段做标准化映射
                    for (Map<String, Object> d : depts)
                    {
                        Map<String, Object> normalized = new LinkedHashMap<>();
                        normalized.put("dept_id", d.get("dept_id"));
                        normalized.put("name", d.get("name"));
                        normalized.put("parent_id", d.get("parent_id"));
                        allDepts.add(normalized);
                    }
                }
            }
            else
            {
                log.warn("获取钉钉部门列表失败: {}", resp);
            }
        }
        catch (Exception e) { log.error("钉钉部门API异常", e); }
        return allDepts;
    }

    /**
     * 递归获取所有部门
     */
    public List<Map<String, Object>> getAllDepartments(String token)
    {
        List<Map<String, Object>> all = new ArrayList<>();
        collectDepts(token, 1L, all, 0);
        return all;
    }

    private void collectDepts(String token, Long parentId, List<Map<String, Object>> all, int depth)
    {
        if (depth > 10) return; // 防止过深递归
        List<Map<String, Object>> subs = getDepartmentList(token, parentId);
        all.addAll(subs);
        for (Map<String, Object> d : subs)
        {
            Object id = d.get("dept_id");
            if (id != null)
            {
                collectDepts(token, ((Number)id).longValue(), all, depth + 1);
            }
        }
    }

    /**
     * 获取部门用户列表
     * @param token access_token
     * @param deptId 部门ID
     * @param cursor 分页游标
     * @param size 每页大小
     * @return {has_more, next_cursor, users}
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserListByDept(String token, Long deptId, int cursor, int size)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        try
        {
            String url = getBaseUrl() +"/topapi/v2/user/list";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("dept_id", deptId);
            body.put("cursor", cursor);
            body.put("size", size);

            String resp = httpPost(url, mapper.writeValueAsString(body), token);
            Map<String, Object> r = mapper.readValue(resp, Map.class);
            if (0 == ((Number)r.get("errcode")).intValue())
            {
                Map<String, Object> pageResult = (Map<String, Object>) r.get("result");
                result.put("hasMore", pageResult.get("has_more"));
                result.put("nextCursor", pageResult.get("next_cursor"));

                List<Map<String, Object>> users = (List<Map<String, Object>>) pageResult.get("list");
                List<Map<String, Object>> normalized = new ArrayList<>();
                if (users != null)
                {
                    for (Map<String, Object> u : users)
                    {
                        // 标准化为系统字段
                        Map<String, Object> nu = new LinkedHashMap<>();
                        nu.put("userid", u.get("userid"));
                        nu.put("name", u.get("name"));
                        nu.put("mobile", u.get("mobile"));
                        nu.put("email", u.get("email"));
                        nu.put("org_email", u.get("org_email"));
                        nu.put("title", u.get("title"));
                        nu.put("job_number", u.get("job_number"));
                        nu.put("work_place", u.get("work_place"));
                        nu.put("tel", u.get("telephone"));
                        nu.put("hire_date", u.get("hired_date"));
                        nu.put("dept_id_list", u.get("dept_id_list"));
                        normalized.add(nu);
                    }
                }
                result.put("users", normalized);
            }
        }
        catch (Exception e) { log.error("钉钉用户列表API异常", e); }
        return result;
    }

    /**
     * 获取所有用户（遍历部门）
     */
    public List<Map<String, Object>> getAllUsers(String token, List<Map<String, Object>> departments)
    {
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> all = new ArrayList<>();
        for (Map<String, Object> dept : departments)
        {
            Object deptId = dept.get("dept_id");
            if (deptId == null) continue;

            int cursor = 0;
            while (true)
            {
                Map<String, Object> page = getUserListByDept(token, ((Number)deptId).longValue(), cursor, 50);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> users = (List<Map<String, Object>>) page.get("users");
                if (users != null)
                {
                    for (Map<String, Object> u : users)
                    {
                        String uid = String.valueOf(u.get("userid"));
                        if (seen.add(uid)) all.add(u);
                    }
                }
                if (!Boolean.TRUE.equals(page.get("hasMore"))) break;
                cursor = ((Number)page.get("nextCursor")).intValue();
            }
        }
        return all;
    }

    private String httpGet(String urlStr) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        return readResponse(conn);
    }

    private String httpPost(String urlStr, String jsonBody, String token) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null)
        {
            conn.setRequestProperty("x-acs-dingtalk-access-token", token);
        }
        try (OutputStream os = conn.getOutputStream())
        {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws Exception
    {
        int code = conn.getResponseCode();
        try (java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream())
        {
            if (is == null) return "";
            byte[] buf = new byte[4096];
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
