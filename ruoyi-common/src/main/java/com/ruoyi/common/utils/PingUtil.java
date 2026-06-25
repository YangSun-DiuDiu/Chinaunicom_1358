package com.ruoyi.common.utils;

import java.net.InetAddress;

/**
 * 通用Ping巡检工具类
 * 用于设备管理和智能化管理设备的网络连通性检测
 */
public class PingUtil
{
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int MAX_LATENCY = 5000;

    public static class PingResult
    {
        public boolean reachable;
        public long latency;
        public PingResult(boolean r, long l) { this.reachable = r; this.latency = l; }
    }

    public static PingResult ping(String ip) { return ping(ip, DEFAULT_TIMEOUT); }

    public static PingResult ping(String ip, int timeoutMs)
    {
        if (ip == null || ip.isEmpty()) return new PingResult(false, -1L);
        long start = System.currentTimeMillis();
        try
        {
            InetAddress inet = InetAddress.getByName(ip);
            boolean r = inet.isReachable(timeoutMs);
            long lat = System.currentTimeMillis() - start;
            if (r && lat > MAX_LATENCY) r = false;
            return new PingResult(r, lat);
        }
        catch (Exception e) { return new PingResult(false, System.currentTimeMillis() - start); }
    }
}
