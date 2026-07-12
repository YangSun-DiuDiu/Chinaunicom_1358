package com.ruoyi.system.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * DeepSeek 大模型客户端（Anthropic 兼容接口）
 *
 * <p>基于 JDK 11+ 原生 {@link HttpClient} 调用 DeepSeek 的 Anthropic 兼容 /v1/messages 接口，
 * 用于网络报告等 AI 生成场景。调用失败或异常时统一降级返回 null，不向上抛出异常。</p>
 */
@Component
public class DeepSeekClient
{
    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    /** DeepSeek Anthropic 兼容接口基础地址 */
    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    /** API 密钥 */
    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    /** 使用的模型名称 */
    @Value("${ai.deepseek.model}")
    private String model;

    /** 请求超时时间（毫秒），默认 60000 */
    @Value("${ai.deepseek.timeout:60000}")
    private long timeout;

    /**
     * 调用大模型进行对话生成
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 模型返回的文本内容；调用失败或异常时返回 null（调用方负责降级处理）
     */
    public String chat(String systemPrompt, String userPrompt)
    {
        try
        {
            // 构造 Anthropic 兼容请求体
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("max_tokens", 4000);
            body.put("system", systemPrompt);
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);
            body.put("messages", messages);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeout))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/messages"))
                    .timeout(Duration.ofMillis(timeout))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200)
            {
                log.error("DeepSeek 调用失败, status={}, body={}", response.statusCode(), response.body());
                return null;
            }

            // 解析 Anthropic 格式响应: { "content": [ { "type": "text", "text": "..." } ] }
            JSONObject json = JSON.parseObject(response.body());
            JSONArray content = json.getJSONArray("content");
            if (content == null || content.isEmpty())
            {
                log.error("DeepSeek 响应缺少 content 字段, body={}", response.body());
                return null;
            }
            return content.getJSONObject(0).getString("text");
        }
        catch (Exception e)
        {
            log.error("DeepSeek 调用异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
