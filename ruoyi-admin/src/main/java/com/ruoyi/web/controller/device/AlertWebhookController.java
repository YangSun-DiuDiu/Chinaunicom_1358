package com.ruoyi.web.controller.device;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.IAlertWebhookService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AlertManager 告警 Webhook 接收接口
 *
 * 仅接收请求并做 IP 白名单校验，业务逻辑全部委托给 {@link IAlertWebhookService}。
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/device/alert")
public class AlertWebhookController
{
    private static final Logger log = LoggerFactory.getLogger(AlertWebhookController.class);

    /** 允许访问 Webhook 的 IP 白名单 */
    private static final String[] IP_WHITELIST = { "115.159.194.149", "127.0.0.1" };

    @Autowired
    private IAlertWebhookService alertWebhookService;

    /**
     * 接收 AlertManager 告警推送
     *
     * @param payload AlertManager 告警数据
     * @param request HTTP 请求
     * @return 处理结果
     */
    @PostMapping("/webhook")
    public AjaxResult webhook(@RequestBody Map<String, Object> payload, HttpServletRequest request)
    {
        String clientIp = getClientIp(request);
        if (!isWhitelisted(clientIp))
        {
            log.warn("[AlertWebhook] 非白名单 IP[{}] 尝试访问告警接口，已拒绝", clientIp);
            return AjaxResult.error("forbidden");
        }
        int handledCount = alertWebhookService.handleAlert(payload);
        return AjaxResult.success("handled", handledCount);
    }

    /**
     * 获取客户端真实 IP（TCP 层源 IP）
     *
     * 安全说明：Webhook 白名单场景禁止信任 X-Forwarded-For 等客户端可控请求头，
     * 否则攻击者可通过伪造 X-Forwarded-For: 127.0.0.1 绕过 IP 白名单校验。
     * 此处仅使用 request.getRemoteAddr()，即 TCP 连接的源地址，客户端无法伪造。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request)
    {
        return request.getRemoteAddr();
    }

    /**
     * 校验 IP 是否在白名单中
     *
     * @param clientIp 客户端 IP
     * @return true 在白名单
     */
    private boolean isWhitelisted(String clientIp)
    {
        if (StringUtils.isEmpty(clientIp))
        {
            return false;
        }
        for (String allowed : IP_WHITELIST)
        {
            if (allowed.equals(clientIp))
            {
                return true;
            }
        }
        return false;
    }
}
