package com.ruoyi.system.service.impl.sync;

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
 * 企业微信API服务
 * 文档: https://developer.work.weixin.qq.com/document/path/90664
 *
 * @author ruoyi
 */
@Service
public class WeComApiService
{
    private static final Logger log = LoggerFactory.getLogger(WeComApiService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private JdbcTemplate jdbc;

    /** 获取API基础URL */
    private String getBaseUrl() {
        String url = getConfig("wecom.api_url");
        return url.isEmpty() ? "https://qyapi.weixin.qq.com/cgi-bin" : url;
    }
    private String getCorpId() { return getConfig("wecom.corp_id"); }
    private String getSecret() { return getConfig("wecom.secret"); }

    private String getConfig(String key) {
        try {
            return jdbc.queryForObject(
                "SELECT config_value FROM attendance_config WHERE config_key=? AND status='0'",
                String.class, key);
        } catch (Exception e) { return ""; }
    }

    /**
     * 获取企业微信 access_token
     */
    public String getAccessToken()
    {
        String corpId = getCorpId();
        String secret = getSecret();
        if (corpId.isEmpty() || secret.isEmpty())
        {
            log.warn("企业微信CorpID或Secret未配置");
            return null;
        }

        try
        {
            String url = getBaseUrl() +"/gettoken?corpid=" + corpId + "&corpsecret=" + secret;
            String resp = httpGet(url);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(resp, Map.class);
            if (0 == ((Number)result.get("errcode")).intValue())
            {
                return result.get("access_token").toString();
            }
            log.error("获取企业微信token失败: {}", resp);
        }
        catch (Exception e) { log.error("企业微信API调用异常", e); }
        return null;
    }

    /**
     * 获取部门列表
     * @param token access_token
     * @param parentId 父部门ID (null=根部门)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDepartmentList(String token, Long parentId)
    {
        List<Map<String, Object>> all = new ArrayList<>();
        try
        {
            String url = getBaseUrl() +"/department/list?access_token=" + token;
            if (parentId != null) url += "&id=" + parentId;

            String resp = httpGet(url);
            Map<String, Object> result = mapper.readValue(resp, Map.class);
            if (0 == ((Number)result.get("errcode")).intValue())
            {
                List<Map<String, Object>> depts = (List<Map<String, Object>>) result.get("department");
                if (depts != null)
                {
                    for (Map<String, Object> d : depts)
                    {
                        Map<String, Object> nd = new LinkedHashMap<>();
                        nd.put("id", d.get("id"));
                        nd.put("name", d.get("name"));
                        nd.put("parentid", d.get("parentid"));
                        nd.put("order", d.get("order"));
                        all.add(nd);
                    }
                }
            }
            else
            {
                log.warn("获取企业微信部门列表失败: {}", resp);
            }
        }
        catch (Exception e) { log.error("企业微信部门API异常", e); }
        return all;
    }

    /**
     * 递归获取所有部门
     */
    public List<Map<String, Object>> getAllDepartments(String token)
    {
        List<Map<String, Object>> all = new ArrayList<>();
        collectDepts(token, null, all, 0);
        return all;
    }

    private void collectDepts(String token, Long parentId, List<Map<String, Object>> all, int depth)
    {
        if (depth > 10) return;
        List<Map<String, Object>> subs = getDepartmentList(token, parentId);
        all.addAll(subs);
        for (Map<String, Object> d : subs)
        {
            Object id = d.get("id");
            if (id != null)
            {
                collectDepts(token, ((Number)id).longValue(), all, depth + 1);
            }
        }
    }

    /**
     * 获取部门成员列表
     * @param token access_token
     * @param deptId 部门ID
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserListByDept(String token, Long deptId)
    {
        List<Map<String, Object>> normalized = new ArrayList<>();
        try
        {
            String url = getBaseUrl() +"/user/list?access_token=" + token +
                "&department_id=" + deptId + "&fetch_child=1";

            String resp = httpGet(url);
            Map<String, Object> result = mapper.readValue(resp, Map.class);
            if (0 == ((Number)result.get("errcode")).intValue())
            {
                List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("userlist");
                if (users != null)
                {
                    for (Map<String, Object> u : users)
                    {
                        Map<String, Object> nu = new LinkedHashMap<>();
                        nu.put("userid", u.get("userid"));
                        nu.put("name", u.get("name"));
                        nu.put("mobile", u.get("mobile"));
                        nu.put("email", u.get("email"));
                        nu.put("department", u.get("department"));
                        nu.put("position", u.get("position"));
                        nu.put("gender", u.get("gender"));
                        nu.put("telephone", u.get("telephone"));
                        nu.put("alias", u.get("alias"));
                        nu.put("address", u.get("address"));
                        nu.put("avatar", u.get("avatar"));
                        normalized.add(nu);
                    }
                }
            }
        }
        catch (Exception e) { log.error("企业微信用户列表API异常", e); }
        return normalized;
    }

    /**
     * 获取所有用户
     */
    public List<Map<String, Object>> getAllUsers(String token, List<Map<String, Object>> departments)
    {
        List<Map<String, Object>> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> dept : departments)
        {
            Object deptId = dept.get("id");
            if (deptId == null) continue;
            List<Map<String, Object>> users = getUserListByDept(token, ((Number)deptId).longValue());
            for (Map<String, Object> u : users)
            {
                String uid = String.valueOf(u.get("userid"));
                if (seen.add(uid)) all.add(u);
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
