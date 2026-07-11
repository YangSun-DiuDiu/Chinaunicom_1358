package com.ruoyi.system.service.impl;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.http.HttpUtils;

/**
 * Prometheus HTTP API 查询服务
 */
@Service
public class PrometheusQueryService
{
    private static final Logger log = LoggerFactory.getLogger(PrometheusQueryService.class);

    @Value("${snmp.prometheus.url:http://192.168.1.60:9090}")
    private String prometheusUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 即时查询 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> instantQuery(String query) throws Exception
    {
        String url = prometheusUrl + "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String resp = HttpUtils.sendGet(url);
        Map<String, Object> result = mapper.readValue(resp, Map.class);
        return (Map<String, Object>) result.get("data");
    }

    /** 范围查询（用于ECharts时序曲线） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rangeQuery(String query, String start, String end, String step) throws Exception
    {
        String url = prometheusUrl + "/api/v1/query_range?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&start=" + start + "&end=" + end + "&step=" + step;
        String resp = HttpUtils.sendGet(url);
        Map<String, Object> result = mapper.readValue(resp, Map.class);
        return (Map<String, Object>) result.get("data");
    }

    /** 查询设备实时指标 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> queryDeviceMetrics(String deviceIp)
    {
        try
        {
            String query = "{device_ip=\"" + deviceIp + "\"}";
            Map<String, Object> data = instantQuery(query);
            if (data == null) return Collections.emptyList();
            List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
            if (results == null) return Collections.emptyList();
            List<Map<String, Object>> metrics = new ArrayList<>();
            for (Map<String, Object> r : results)
            {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("metric", ((Map<String, Object>) r.get("metric")).get("__name__"));
                m.put("value", ((List<Object>) r.get("value")).get(1));
                metrics.add(m);
            }
            return metrics;
        }
        catch (Exception e)
        {
            log.warn("Prometheus query failed for device {}: {}", deviceIp, e.getMessage());
            return Collections.emptyList();
        }
    }
}
