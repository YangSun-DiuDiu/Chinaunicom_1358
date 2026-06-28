package com.ruoyi.system.sms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.StringUtils;

/**
 * 通用HTTP短信驱动——支持GET/POST/POST-JSON，模板变量替换
 *
 * @author ruoyi
 */
public class GenericHttpDriver implements SmsDriver
{
    private static final Logger log = LoggerFactory.getLogger(GenericHttpDriver.class);

    @Override
    public boolean send(Map<String, Object> channel, Map<String, Object> template, String phone, String params)
    {
        String endpoint = getString(channel, "endpoint");
        String apiMethod = getString(channel, "api_method");
        String apiBodyTemplate = getString(channel, "api_body_template");
        String apiSuccessRule = getString(channel, "api_success_rule");
        String signName = getString(template, "sign_name");
        int timeout = getInt(channel, "timeout", 5000);

        if (StringUtils.isEmpty(endpoint))
        {
            log.error("GenericHttpDriver: endpoint 未配置");
            return false;
        }

        if (StringUtils.isEmpty(apiMethod))
        {
            apiMethod = "POST";
        }

        try
        {
            String requestBody = null;
            if (StringUtils.isNotEmpty(apiBodyTemplate))
            {
                requestBody = apiBodyTemplate
                        .replace("${phone}", phone != null ? phone : "")
                        .replace("${code}", params != null ? params : "")
                        .replace("${sign}", signName != null ? signName : "");
            }

            String responseBody;
            if ("GET".equalsIgnoreCase(apiMethod))
            {
                responseBody = httpGet(endpoint, requestBody, timeout);
            }
            else if ("POST-JSON".equalsIgnoreCase(apiMethod))
            {
                responseBody = httpPostJson(endpoint, requestBody, timeout);
            }
            else
            {
                responseBody = httpPost(endpoint, requestBody, timeout);
            }

            boolean success = checkSuccess(responseBody, apiSuccessRule);
            if (success)
            {
                log.info("GenericHttpDriver 发送成功: phone={}, response={}", phone, responseBody);
            }
            else
            {
                log.error("GenericHttpDriver 发送失败: phone={}, response={}, successRule={}",
                        phone, responseBody, apiSuccessRule);
            }
            return success;
        }
        catch (Exception e)
        {
            log.error("GenericHttpDriver 请求异常: phone={}, endpoint={}", phone, endpoint, e);
            return false;
        }
    }

    /**
     * GET请求
     */
    private String httpGet(String urlStr, String queryString, int timeout) throws Exception
    {
        String fullUrl = urlStr;
        if (StringUtils.isNotEmpty(queryString))
        {
            fullUrl += (urlStr.contains("?") ? "&" : "?") + queryString;
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        return readResponse(conn);
    }

    /**
     * POST表单请求
     */
    private String httpPost(String urlStr, String body, int timeout) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (StringUtils.isNotEmpty(body))
        {
            try (OutputStream os = conn.getOutputStream())
            {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }
        return readResponse(conn);
    }

    /**
     * POST-JSON请求
     */
    private String httpPostJson(String urlStr, String body, int timeout) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        if (StringUtils.isNotEmpty(body))
        {
            try (OutputStream os = conn.getOutputStream())
            {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }
        return readResponse(conn);
    }

    /**
     * 读取响应
     */
    private String readResponse(HttpURLConnection conn) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 根据成功规则判断是否成功
     */
    private boolean checkSuccess(String responseBody, String successRule)
    {
        if (StringUtils.isEmpty(successRule))
        {
            return StringUtils.isNotEmpty(responseBody);
        }
        return responseBody != null && responseBody.contains(successRule);
    }

    private String getString(Map<String, Object> map, String key)
    {
        Object val = map != null ? map.get(key) : null;
        return val != null ? val.toString() : "";
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue)
    {
        Object val = map != null ? map.get(key) : null;
        if (val instanceof Number)
        {
            return ((Number) val).intValue();
        }
        if (val != null)
        {
            try
            {
                return Integer.parseInt(val.toString());
            }
            catch (NumberFormatException e)
            {
                // fall through to default
            }
        }
        return defaultValue;
    }
}
