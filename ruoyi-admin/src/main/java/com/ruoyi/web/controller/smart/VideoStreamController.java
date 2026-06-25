package com.ruoyi.web.controller.smart;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
// VideoStreamService is in the same package

/**
 * 视频流控制器 — RTSP抓帧 + WebSocket推送 + 快照接口
 */
@RestController
@RequestMapping("/smart/video")
public class VideoStreamController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(VideoStreamController.class);

    @Autowired
    private VideoStreamService videoStreamService;

    /**
     * 获取视频设备信息（含RTSP拼接URL）
     */
    @GetMapping("/device/{deviceId}")
    @PreAuthorize("@ss.hasPermi('smart:videoPreview:list')")
    public AjaxResult getDeviceInfo(@PathVariable Long deviceId)
    {
        Map<String, String> info = videoStreamService.getDeviceInfo(deviceId);
        if (info == null) return error("设备不存在");

        // 拼接完整RTSP URL
        String ip = info.get("ipAddress");
        String port = info.get("rtspPort");
        String username = info.get("username");
        String password = info.get("password");
        String channel = info.get("channel");
        String customUrl = info.get("rtspUrl");

        String rtspUrl;
        if (!customUrl.isEmpty())
        {
            rtspUrl = customUrl;
        }
        else
        {
            // 拼接默认RTSP URL: rtsp://user:pass@ip:port/Streaming/Channels/101
            String auth = (!username.isEmpty() && !password.isEmpty()) ? username + ":" + password + "@" : "";
            rtspUrl = "rtsp://" + auth + ip + ":" + port + "/Streaming/Channels/" + channel + "01";
        }

        info.put("fullRtspUrl", rtspUrl);
        return success(info);
    }

    /**
     * 获取单帧快照 (JPEG图片)
     */
    @GetMapping(value = "/snapshot/{deviceId}", produces = MediaType.IMAGE_JPEG_VALUE)
    @PreAuthorize("@ss.hasPermi('smart:videoPreview:list')")
    public ResponseEntity<byte[]> snapshot(@PathVariable Long deviceId)
    {
        byte[] data = videoStreamService.captureSnapshot(deviceId);
        if (data != null)
        {
            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .body(data);
        }
        return ResponseEntity.notFound().build();
    }

    // ============ WebSocket 视频流配置 ============

    @Configuration
    @EnableWebSocket
    public static class VideoWebSocketConfig implements WebSocketConfigurer
    {
        @Autowired
        private VideoWebSocketHandler handler;

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
        {
            // ws://host:port/ws/video/{deviceId}
            registry.addHandler(handler, "/ws/video/{deviceId}")
                    .setAllowedOriginPatterns("*");
        }
    }

    @org.springframework.stereotype.Component
    public static class VideoWebSocketHandler extends TextWebSocketHandler
    {
        private static final Logger wsLog = LoggerFactory.getLogger(VideoWebSocketHandler.class);

        @Autowired
        private VideoStreamService videoStreamService;

        private Long extractDeviceId(WebSocketSession session)
        {
            String path = session.getUri().getPath(); // /ws/video/123
            String[] parts = path.split("/");
            try { return Long.parseLong(parts[parts.length - 1]); }
            catch (Exception e) { return null; }
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception
        {
            Long deviceId = extractDeviceId(session);
            if (deviceId == null) { session.close(); return; }

            videoStreamService.addViewer(deviceId, session);
            wsLog.info("视频观看连接: deviceId={}, session={}", deviceId, session.getId());

            // 发送设备信息
            Map<String, String> info = videoStreamService.getDeviceInfo(deviceId);
            if (info != null)
            {
                session.sendMessage(new TextMessage(
                    "{\"type\":\"INFO\",\"deviceName\":\"" + info.get("deviceName") + "\"}"));
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception
        {
            Long deviceId = extractDeviceId(session);
            if (deviceId != null) videoStreamService.removeViewer(deviceId, session);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception
        {
            // 心跳处理
            if ("PING".equals(message.getPayload()))
            {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            }
        }
    }
}
