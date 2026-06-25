package com.ruoyi.system.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 基于系统 snmpwalk 命令的 SNMP 数据获取服务
 * 通过 ProcessBuilder 调用原生 snmpwalk，获取完整且可靠的 SNMP 数据
 * <p>
 * 当 snmpwalk 命令不可用时，自动回退到 Java 原生 SNMP 实现（{@link SnmpService}），
 * 确保 SNMP 功能在未安装 Net-SNMP 的环境中也能正常工作。
 * </p>
 *
 * @author SmartIoT
 */
@Component
public class SnmpWalkService
{
    private static final Logger log = LoggerFactory.getLogger(SnmpWalkService.class);

    /** snmpwalk 可执行文件路径（支持配置） */
    @Value("${snmp.walk-path:snmpwalk}")
    private String snmpWalkPath;

    /** snmpwalk 命令超时秒数 */
    @Value("${snmp.walk-timeout:4}")
    private int cmdTimeout;

    /** 是否启用 Java 原生 SNMP 回退 */
    @Value("${snmp.native-fallback:true}")
    private boolean nativeFallback;

    /** Java 原生 SNMP 实现（回退方案） */
    @Autowired(required = false)
    private SnmpService snmpService;

    /** snmpwalk 命令是否可用 */
    private boolean snmpWalkAvailable = false;

    /** snmpwalk 可用性检测结果描述 */
    private String availabilityMessage = "";

    /** 常用 OID */
    public static final String SYS_DESCR   = "1.3.6.1.2.1.1.1.0";
    public static final String SYS_NAME    = "1.3.6.1.2.1.1.5.0";
    public static final String SYS_UP_TIME = "1.3.6.1.2.1.1.3.0";
    public static final String SYS_CONTACT = "1.3.6.1.2.1.1.4.0";
    public static final String SYS_LOCATION = "1.3.6.1.2.1.1.6.0";
    public static final String IF_DESCR    = "1.3.6.1.2.1.2.2.1.2";
    public static final String IF_TYPE     = "1.3.6.1.2.1.2.2.1.3";
    public static final String IF_SPEED    = "1.3.6.1.2.1.2.2.1.5";
    public static final String IF_STATUS   = "1.3.6.1.2.1.2.2.1.8";
    public static final String HR_STORAGE_DESCR = "1.3.6.1.2.1.25.2.3.1.3";
    public static final String HR_STORAGE_SIZE  = "1.3.6.1.2.1.25.2.3.1.5";
    public static final String HR_STORAGE_USED  = "1.3.6.1.2.1.25.2.3.1.6";

    /** 默认端口 */
    private static final int DEFAULT_PORT = 161;

    /**
     * 服务启动时检测 snmpwalk 可用性
     */
    @PostConstruct
    public void init()
    {
        detectSnmpWalk();
        if (snmpWalkAvailable)
        {
            log.info("SNMP服务初始化完成: snmpwalk可用 (路径={}), 超时={}秒", snmpWalkPath, cmdTimeout);
        }
        else if (nativeFallback && snmpService != null)
        {
            log.info("SNMP服务初始化完成: snmpwalk不可用, 已启用Java原生SNMP回退模式 ({})", availabilityMessage);
        }
        else if (nativeFallback && snmpService == null)
        {
            log.warn("SNMP服务初始化完成: snmpwalk不可用, Java原生SnmpService未注入, SNMP功能将不可用");
        }
        else
        {
            log.warn("SNMP服务初始化完成: snmpwalk不可用, 且已禁用回退模式, SNMP功能将不可用 ({})", availabilityMessage);
        }
    }

    /**
     * 检测 snmpwalk 命令是否可用
     */
    private void detectSnmpWalk()
    {
        // 方式1: 检查是否是绝对路径且文件存在
        File f = new File(snmpWalkPath);
        if (f.isAbsolute() && f.exists() && f.canExecute())
        {
            snmpWalkAvailable = true;
            return;
        }

        // 方式2: 尝试在PATH中查找
        try
        {
            ProcessBuilder pb = new ProcessBuilder(snmpWalkPath, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(3, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0)
            {
                snmpWalkAvailable = true;
                return;
            }
            if (!finished)
            {
                p.destroyForcibly();
            }
        }
        catch (Exception e)
        {
            // snmpwalk 不可用
        }

        snmpWalkAvailable = false;

        // 构建详细的不可用消息
        StringBuilder sb = new StringBuilder();
        sb.append("snmpwalk命令 '").append(snmpWalkPath).append("' 不可用");
        if (f.isAbsolute())
        {
            sb.append(" (文件不存在或无执行权限)");
        }
        else
        {
            sb.append(" (不在系统PATH中)");
        }
        availabilityMessage = sb.toString();
    }

    /**
     * 判断当前是否应使用 Java 原生回退
     */
    private boolean useNativeFallback()
    {
        return !snmpWalkAvailable && nativeFallback && snmpService != null;
    }

    /**
     * 获取当前SNMP实现模式描述
     */
    public String getModeDescription()
    {
        if (snmpWalkAvailable)
        {
            return "snmpwalk原生模式 (路径: " + snmpWalkPath + ")";
        }
        if (useNativeFallback())
        {
            return "Java原生SNMP回退模式 (snmpwalk不可用, 使用UDP Socket实现)";
        }
        return "SNMP功能不可用 (" + availabilityMessage + ")";
    }

    /**
     * snmpwalk 是否可用
     */
    public boolean isSnmpWalkAvailable()
    {
        return snmpWalkAvailable;
    }

    /**
     * 构建带端口的IP地址字符串(ip:port)，省略默认161端口以兼容旧版本snmpwalk
     */
    private String hostPort(String ip, int port) {
        return port == DEFAULT_PORT ? ip : ip + ":" + port;
    }

    /**
     * 执行 snmpwalk 命令（优先使用原生snmpwalk，不可用时回退到Java实现）
     * @param ip 设备IP
     * @param port SNMP端口
     * @param community 团体名
     * @param oid 目标OID
     * @return OID=值 的 Map，失败返回空Map
     */
    public Map<String, String> walk(String ip, int port, String community, String oid)
    {
        if (ip == null || ip.isEmpty() || community == null || community.isEmpty() || oid == null)
        {
            return new LinkedHashMap<>();
        }

        if (snmpWalkAvailable)
        {
            return walkNative(ip, port, community, oid);
        }

        if (useNativeFallback())
        {
            log.debug("snmpwalk不可用, 使用Java原生SNMP walk: ip={}, oid={}", ip, oid);
            try
            {
                return snmpService.snmpWalk(ip, port, community, oid);
            }
            catch (Exception e)
            {
                log.error("Java原生SNMP walk异常: ip={}, oid={}, error={}", ip, oid, e.getMessage());
                return new LinkedHashMap<>();
            }
        }

        log.warn("SNMP功能不可用: snmpwalk未安装且未启用回退模式");
        return new LinkedHashMap<>();
    }

    /**
     * 原生 snmpwalk 调用实现
     */
    private Map<String, String> walkNative(String ip, int port, String community, String oid)
    {
        Map<String, String> result = new LinkedHashMap<>();

        try
        {
            // -t: 每次请求超时(秒), -r: 重试次数, 增加稳定性
            ProcessBuilder pb = new ProcessBuilder(
                    snmpWalkPath, "-v2c", "-c", community,
                    "-t", String.valueOf(Math.max(3, cmdTimeout / 2)),  // 单次请求超时
                    "-r", "1",                                           // 重试1次
                    "-O", "n", hostPort(ip, port), oid);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 先读取全部输出（避免缓冲区满导致进程阻塞），再等待退出
            StringBuilder output = new StringBuilder();
            StringBuilder errOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    output.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(cmdTimeout, TimeUnit.SECONDS);
            if (!finished)
            {
                process.destroyForcibly();
                // 即使超时，如果有部分数据也返回
                log.warn("snmpwalk 超时但已有{}条数据: ip={}, oid={}, 已读取{}字节",
                    output.toString().split("\n").length, ip, oid, output.length());
            }

            // 解析已读取的输出
            for (String line : output.toString().split("\n"))
            {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 格式: OID = TYPE: Value
                int eqIdx = line.indexOf('=');
                if (eqIdx < 0) continue;

                String key = line.substring(0, eqIdx).trim();
                String val = line.substring(eqIdx + 1).trim();

                // 去掉 TYPE: 前缀 (如 "STRING: xxx" → "xxx")
                int colonIdx = val.indexOf(": ");
                if (colonIdx > 0 && colonIdx < 20)
                {
                    val = val.substring(colonIdx + 2).trim();
                }
                // 去掉首尾引号及空白
                val = val.replaceAll("^[\"']|[\"']$", "").trim();
                result.put(key, val);
            }

            log.debug("snmpwalk 完成: ip={}, oid={}, 获取{}条", ip, oid, result.size());
        }
        catch (Exception e)
        {
            log.error("snmpwalk 异常: ip={}, oid={}, error={}", ip, oid, e.getMessage());
        }
        return result;
    }

    /**
     * 执行 snmpget 获取单个 OID（优先原生，不可用时回退Java实现）
     */
    public String get(String ip, int port, String community, String oid)
    {
        if (snmpWalkAvailable)
        {
            Map<String, String> result = walkNative(ip, port, community, oid);
            if (result.isEmpty()) return null;
            for (String val : result.values())
            {
                return val;
            }
            return null;
        }

        if (useNativeFallback())
        {
            log.debug("snmpwalk不可用, 使用Java原生SNMP get: ip={}, oid={}", ip, oid);
            try
            {
                return snmpService.snmpGet(ip, port, community, oid);
            }
            catch (Exception e)
            {
                log.error("Java原生SNMP get异常: ip={}, oid={}, error={}", ip, oid, e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * 获取设备基本信息
     */
    public Map<String, String> getDeviceInfo(String ip, int port, String community)
    {
        Map<String, String> info = new LinkedHashMap<>();

        if (snmpWalkAvailable)
        {
            info.put("sysName", get(ip, port, community, SYS_NAME));
            info.put("sysDescr", get(ip, port, community, SYS_DESCR));
            info.put("sysUpTime", get(ip, port, community, SYS_UP_TIME));
            info.put("sysContact", get(ip, port, community, SYS_CONTACT));
            info.put("sysLocation", get(ip, port, community, SYS_LOCATION));
            return info;
        }

        if (useNativeFallback())
        {
            log.debug("snmpwalk不可用, 使用Java原生SNMP获取设备信息: ip={}", ip);
            info.put("sysName", snmpService.snmpGet(ip, port, community, SYS_NAME));
            info.put("sysDescr", snmpService.snmpGet(ip, port, community, SYS_DESCR));
            info.put("sysUpTime", snmpService.snmpGet(ip, port, community, SYS_UP_TIME));
            info.put("sysContact", snmpService.snmpGet(ip, port, community, SYS_CONTACT));
            info.put("sysLocation", snmpService.snmpGet(ip, port, community, SYS_LOCATION));
            return info;
        }

        return info;
    }

    /**
     * 获取端口列表（含名称、类型、速率、状态）
     */
    public List<Map<String, String>> getPorts(String ip, int port, String community)
    {
        if (!snmpWalkAvailable && useNativeFallback())
        {
            return getPortsNative(ip, port, community);
        }
        return getPortsViaWalk(ip, port, community);
    }

    /**
     * 通过 snmpwalk 获取端口列表
     */
    private List<Map<String, String>> getPortsViaWalk(String ip, int port, String community)
    {
        List<Map<String, String>> ports = new ArrayList<>();

        Map<String, String> descrMap  = walk(ip, port, community, IF_DESCR);
        Map<String, String> typeMap   = walk(ip, port, community, IF_TYPE);
        Map<String, String> speedMap  = walk(ip, port, community, IF_SPEED);
        Map<String, String> statusMap = walk(ip, port, community, IF_STATUS);

        // 构建后缀索引: suffix → value
        Map<String, String> typeBySuffix   = indexBySuffix(typeMap, IF_TYPE);
        Map<String, String> speedBySuffix  = indexBySuffix(speedMap, IF_SPEED);
        Map<String, String> statusBySuffix = indexBySuffix(statusMap, IF_STATUS);

        for (Map.Entry<String, String> e : descrMap.entrySet())
        {
            String suffix = e.getKey().substring(e.getKey().lastIndexOf('.') + 1);
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", e.getValue());
            entry.put("index", suffix);
            entry.put("type", resolveIfType(typeBySuffix.getOrDefault(suffix, "")));
            entry.put("speed", resolveIfSpeed(speedBySuffix.getOrDefault(suffix, "0")));
            entry.put("status", resolveIfStatus(statusBySuffix.getOrDefault(suffix, "0")));
            ports.add(entry);
        }
        return ports;
    }

    /**
     * 通过 Java 原生 SNMP 获取端口列表
     */
    private List<Map<String, String>> getPortsNative(String ip, int port, String community)
    {
        List<Map<String, String>> ports = new ArrayList<>();
        try
        {
            Map<String, String> descrMap  = snmpService.snmpWalk(ip, port, community, IF_DESCR);
            Map<String, String> typeMap   = snmpService.snmpWalk(ip, port, community, IF_TYPE);
            Map<String, String> speedMap  = snmpService.snmpWalk(ip, port, community, IF_SPEED);
            Map<String, String> statusMap = snmpService.snmpWalk(ip, port, community, IF_STATUS);

            // 构建后缀索引: suffix → value
            Map<String, String> typeBySuffix   = indexBySuffix(typeMap, IF_TYPE);
            Map<String, String> speedBySuffix  = indexBySuffix(speedMap, IF_SPEED);
            Map<String, String> statusBySuffix = indexBySuffix(statusMap, IF_STATUS);

            for (Map.Entry<String, String> e : descrMap.entrySet())
            {
                String key = e.getKey();
                String suffix;
                int dot = key.lastIndexOf('.');
                if (dot >= 0)
                {
                    suffix = key.substring(dot + 1);
                }
                else
                {
                    suffix = key;
                }

                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("name", e.getValue());
                entry.put("index", suffix);
                entry.put("type", resolveIfType(typeBySuffix.getOrDefault(suffix, "")));
                entry.put("speed", resolveIfSpeed(speedBySuffix.getOrDefault(suffix, "0")));
                entry.put("status", resolveIfStatus(statusBySuffix.getOrDefault(suffix, "0")));
                ports.add(entry);
            }
        }
        catch (Exception e)
        {
            log.error("Java原生SNMP获取端口列表异常: ip={}, error={}", ip, e.getMessage());
        }
        return ports;
    }

    /** 按 OID 后缀建立索引: 取最后一个数字段作为 key */
    private Map<String, String> indexBySuffix(Map<String, String> oidMap, String baseOid)
    {
        Map<String, String> result = new LinkedHashMap<>();
        String prefix = baseOid.endsWith(".") ? baseOid : baseOid + ".";
        for (Map.Entry<String, String> e : oidMap.entrySet())
        {
            String key = e.getKey();
            if (key.startsWith(prefix))
            {
                result.put(key.substring(prefix.length()), e.getValue());
            }
            else
            {
                // fallback: 取最后一个点后面的数字
                int dot = key.lastIndexOf('.');
                if (dot >= 0) result.put(key.substring(dot + 1), e.getValue());
            }
        }
        return result;
    }

    /**
     * 获取存储使用情况
     */
    public Map<String, Object> getStorageInfo(String ip, int port, String community)
    {
        Map<String, Object> info = new LinkedHashMap<>();
        Map<String, String> descrMap = walk(ip, port, community, HR_STORAGE_DESCR);
        Map<String, String> sizeMap  = walk(ip, port, community, HR_STORAGE_SIZE);
        Map<String, String> usedMap  = walk(ip, port, community, HR_STORAGE_USED);

        List<Map<String, String>> list = new ArrayList<>();
        long totalKB = 0, totalUsedKB = 0;

        for (Map.Entry<String, String> e : descrMap.entrySet())
        {
            String key = e.getKey();
            String descr = e.getValue();
            long sizeKB = parseLong(sizeMap.getOrDefault(key, "0"));
            long usedKB = parseLong(usedMap.getOrDefault(key, "0"));
            totalKB += sizeKB;
            totalUsedKB += usedKB;

            Map<String, String> item = new LinkedHashMap<>();
            item.put("description", descr);
            item.put("totalKB", String.valueOf(sizeKB));
            item.put("usedKB", String.valueOf(usedKB));
            list.add(item);
        }

        info.put("storageList", list);
        info.put("totalSizeGB", String.format("%.2f", totalKB / 1024.0 / 1024.0));
        info.put("totalUsedGB", String.format("%.2f", totalUsedKB / 1024.0 / 1024.0));
        return info;
    }

    private static long parseLong(String s)
    {
        try { return Long.parseLong(s.trim()); }
        catch (Exception e) { return 0; }
    }

    private static String resolveIfType(String typeCode)
    {
        switch (typeCode)
        {
            case "6":  return "以太网";
            case "7":  return "802.3";
            case "23": return "PPP";
            case "24": return "回环";
            case "53": return "VLAN";
            default:   return typeCode.isEmpty() ? "未知" : "Type-" + typeCode;
        }
    }

    private static String resolveIfSpeed(String speed)
    {
        try
        {
            long bps = Long.parseLong(speed.trim());
            if (bps >= 1_000_000_000L) return String.format("%.1f Gbps", bps / 1_000_000_000.0);
            if (bps >= 1_000_000L)      return String.format("%.0f Mbps", bps / 1_000_000.0);
            if (bps >= 1_000L)          return String.format("%.0f Kbps", bps / 1_000.0);
            return bps + " bps";
        }
        catch (Exception e) { return "未知"; }
    }

    private static String resolveIfStatus(String statusCode)
    {
        switch (statusCode)
        {
            case "1": return "up";
            case "2": return "down";
            case "3": return "testing";
            default:  return "unknown";
        }
    }
}
