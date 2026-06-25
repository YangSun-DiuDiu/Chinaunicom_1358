package com.ruoyi.web.controller.smart;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 视频流服务 — RTSP帧抓取 + WebSocket推送
 */
@Service
public class VideoStreamService
{
    private static final Logger log = LoggerFactory.getLogger(VideoStreamService.class);

    @Autowired
    private JdbcTemplate jdbc;

    /** WebSocket观看会话: deviceId → sessions */
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> viewers = new ConcurrentHashMap<>();
    /** 帧抓取线程 */
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> grabTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    /** 注册观看者 */
    public void addViewer(Long deviceId, WebSocketSession session)
    {
        viewers.computeIfAbsent(deviceId, k -> new CopyOnWriteArraySet<>()).add(session);
        startGrabbing(deviceId);
    }

    /** 移除观看者 */
    public void removeViewer(Long deviceId, WebSocketSession session)
    {
        CopyOnWriteArraySet<WebSocketSession> set = viewers.get(deviceId);
        if (set != null) { set.remove(session); if (set.isEmpty()) stopGrabbing(deviceId); }
    }

    /** 获取设备RTSP URL */
    public Map<String, String> getDeviceInfo(Long deviceId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT device_name, ip_address, rtsp_port, rtsp_url, username, password, channel, status FROM iot_video_device WHERE device_id=?",
            deviceId);
        if (rows.isEmpty()) return null;

        Map<String, Object> r = rows.get(0);
        Map<String, String> info = new LinkedHashMap<>();
        info.put("deviceName", String.valueOf(r.get("device_name")));
        info.put("ipAddress", String.valueOf(r.get("ip_address")));
        info.put("rtspPort", String.valueOf(r.get("rtsp_port")));
        info.put("rtspUrl", String.valueOf(r.getOrDefault("rtsp_url", "")));
        info.put("username", String.valueOf(r.getOrDefault("username", "admin")));
        info.put("password", String.valueOf(r.getOrDefault("password", "")));
        info.put("channel", String.valueOf(r.getOrDefault("channel", "1")));
        info.put("status", String.valueOf(r.getOrDefault("status", "OFFLINE")));
        return info;
    }

    /** 获取单帧JPEG快照 */
    public byte[] captureSnapshot(Long deviceId)
    {
        Map<String, String> info = getDeviceInfo(deviceId);
        if (info == null) return null;

        String ip = info.get("ipAddress");
        String username = info.get("username");
        String password = info.get("password");

        // 尝试多种快照URL模式
        String[] snapshotPaths = {
            "/ISAPI/Streaming/channels/101/picture",           // 海康
            "/onvif-http/snapshot",                             // ONVIF标准
            "/snapshot.jpg",                                    // 通用
            "/cgi-bin/snapshot.cgi",                            // 大华
            "/webcapture.jpg?command=snap&channel=1",           // 通用
            "/jpg/image.jpg",                                   // 部分品牌
            "/snap.jpg",                                        // 通用
        };

        for (String path : snapshotPaths)
        {
            try
            {
                String url = "http://" + ip + path;
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                if (!username.isEmpty())
                {
                    String auth = username + ":" + password;
                    String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                    conn.setRequestProperty("Authorization", "Basic " + encoded);
                }
                int code = conn.getResponseCode();
                if (code == 200)
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try (InputStream is = conn.getInputStream())
                    {
                        byte[] buf = new byte[4096]; int n;
                        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
                    }
                    byte[] data = bos.toByteArray();
                    if (data.length > 500) return data; // 有效图片
                }
                conn.disconnect();
            }
            catch (Exception e) { /* 尝试下一个路径 */ }
        }
        return null;
    }

    /** 开始抓取帧 */
    private void startGrabbing(Long deviceId)
    {
        if (grabTasks.containsKey(deviceId)) return;

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
            try
            {
                byte[] frame = captureSnapshot(deviceId);
                if (frame != null)
                {
                    String b64 = Base64.getEncoder().encodeToString(frame);
                    String msg = "{\"type\":\"FRAME\",\"deviceId\":" + deviceId + ",\"image\":\"" + b64 + "\"}";
                    broadcast(deviceId, msg);
                }
            }
            catch (Exception e) { log.debug("Frame grab error: {}", e.getMessage()); }
        }, 0, 800, TimeUnit.MILLISECONDS); // ~1.25 FPS

        grabTasks.put(deviceId, task);
    }

    /** 停止抓取 */
    private void stopGrabbing(Long deviceId)
    {
        ScheduledFuture<?> task = grabTasks.remove(deviceId);
        if (task != null) task.cancel(false);
    }

    /** 广播消息到所有观看者 */
    private void broadcast(Long deviceId, String message)
    {
        CopyOnWriteArraySet<WebSocketSession> set = viewers.get(deviceId);
        if (set == null || set.isEmpty()) return;
        TextMessage tm = new TextMessage(message);
        for (WebSocketSession s : set)
        {
            try { if (s.isOpen()) s.sendMessage(tm); }
            catch (Exception e) { /* skip */ }
        }
    }
}
