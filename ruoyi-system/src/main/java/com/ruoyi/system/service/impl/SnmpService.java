package com.ruoyi.system.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.Device;

/**
 * SNMP v2c 工具服务
 *
 * 基于原生 UDP Socket 实现 SNMP GET / GETNEXT / WALK 操作，
 * 完成 ASN.1 BER 编解码，不依赖第三方 SNMP 库。
 *
 * @author ruoyi
 */
@Component
public class SnmpService
{
    private static final Logger log = LoggerFactory.getLogger(SnmpService.class);

    /** 默认 SNMP 端口 */
    private static final int DEFAULT_SNMP_PORT = 161;

    /** Socket 超时（毫秒） */
    private static final int SOCKET_TIMEOUT_MS = 1500;

    /** SNMP v2c 版本号 */
    private static final int SNMP_VERSION_2C = 1;

    // ============ ASN.1 标签常量 ============
    private static final int TAG_INTEGER          = 0x02;
    private static final int TAG_OCTET_STRING     = 0x04;
    private static final int TAG_NULL             = 0x05;
    private static final int TAG_OID              = 0x06;
    private static final int TAG_SEQUENCE         = 0x30;
    private static final int TAG_IP_ADDRESS       = 0x40;
    private static final int TAG_COUNTER          = 0x41;
    private static final int TAG_GAUGE            = 0x42;
    private static final int TAG_TIMETICKS        = 0x43;
    private static final int TAG_OPAQUE           = 0x44;

    /** SNMP PDU 类型 */
    private static final int PDU_GET       = 0xA0;
    private static final int PDU_GETNEXT   = 0xA1;
    private static final int PDU_RESPONSE  = 0xA2;

    /** 常用 OID */
    private static final String SYS_DESCR   = "1.3.6.1.2.1.1.1.0";
    private static final String SYS_NAME    = "1.3.6.1.2.1.1.5.0";
    private static final String SYS_UP_TIME = "1.3.6.1.2.1.1.3.0";
    private static final String IF_DESCR    = "1.3.6.1.2.1.2.2.1.2";
    private static final String IF_STATUS   = "1.3.6.1.2.1.2.2.1.8";

    /** NVR / 存储相关 OID (HOST-RESOURCES-MIB) */
    private static final String HR_STORAGE_DESCR  = "1.3.6.1.2.1.25.2.3.1.3";  // 存储描述
    private static final String HR_STORAGE_SIZE   = "1.3.6.1.2.1.25.2.3.1.5";  // 总大小(KB)
    private static final String HR_STORAGE_USED   = "1.3.6.1.2.1.25.2.3.1.6";  // 已用大小(KB)

    // ==================== 公开方法 ====================

    /**
     * SNMP GET － 获取单个 OID 的值
     *
     * @param ip        目标设备 IP
     * @param community SNMP 团体名
     * @param oid       要查询的 OID（点分数字格式，如 1.3.6.1.2.1.1.5.0）
     * @return OID 对应的值（字符串），失败返回 null
     */
    public String snmpGet(String ip, String community, String oid)
    {
        return snmpGet(ip, DEFAULT_SNMP_PORT, community, oid);
    }

    /**
     * SNMP GET － 获取单个 OID 的值（指定端口）
     */
    public String snmpGet(String ip, int port, String community, String oid)
    {
        try
        {
            byte[] request = buildRequest(PDU_GET, community, oid);
            byte[] response = sendUdp(ip, port, request);
            if (response == null)
            {
                return null;
            }
            List<SnmpVarBind> varbinds = decodeResponse(response);
            if (varbinds.isEmpty())
            {
                return null;
            }
            return varbinds.get(0).value;
        }
        catch (Exception e)
        {
            log.error("SNMP GET 失败, ip={}, oid={}", ip, oid, e);
            return null;
        }
    }

    /**
     * SNMP WALK － 遍历某个 OID 子树，返回 OID -> 值的映射
     *
     * @param ip        目标设备 IP
     * @param community SNMP 团体名
     * @param oid       起始 OID（点分数字格式）
     * @return Map<OID, 值>，失败返回空 Map
     */
    public Map<String, String> snmpWalk(String ip, String community, String oid)
    {
        return snmpWalk(ip, DEFAULT_SNMP_PORT, community, oid);
    }

    /**
     * SNMP WALK － 遍历某个 OID 子树（指定端口）
     */
    public Map<String, String> snmpWalk(String ip, int port, String community, String oid)
    {
        Map<String, String> result = new LinkedHashMap<>();
        try
        {
            String currentOid = oid;
            int walkCount = 0;
            int maxWalk = 5000; // 防止无限循环

            while (walkCount < maxWalk)
            {
                byte[] request = buildRequest(PDU_GETNEXT, community, currentOid);
                byte[] response = sendUdp(ip, port, request);
                if (response == null)
                {
                    break;
                }
                List<SnmpVarBind> varbinds = decodeResponse(response);
                if (varbinds.isEmpty())
                {
                    break;
                }

                SnmpVarBind vb = varbinds.get(0);

                // 遇到 endOfMibView 或 OID 不再属于目标子树时停止
                if (vb.oid == null || !vb.oid.startsWith(oid + ".") && !vb.oid.equals(oid))
                {
                    // noSuchInstance / endOfMibView 的典型标记:
                    // SNMPv2 错误状态或返回的 OID 不再前缀匹配
                    if (!vb.oid.startsWith(oid))
                    {
                        break;
                    }
                }

                result.put(vb.oid, vb.value);
                currentOid = vb.oid;
                walkCount++;
            }

            log.debug("SNMP WALK 完成, ip={}, oid={}, 条数={}", ip, oid, result.size());
        }
        catch (Exception e)
        {
            log.error("SNMP WALK 失败, ip={}, oid={}", ip, oid, e);
        }
        return result;
    }

    /**
     * 获取设备基本信息（sysName, sysDescr, sysUpTime）
     *
     * @param device 设备对象（需包含 ipAddress, snmpCommunity, snmpPort）
     * @return Map 包含 sysName / sysDescr / sysUpTime，失败返回空 Map
     */
    public Map<String, String> getDeviceInfo(Device device)
    {
        Map<String, String> info = new LinkedHashMap<>();

        if (device == null)
        {
            log.warn("getDeviceInfo: device 为 null");
            return info;
        }

        String ip = device.getIpAddress();
        String community = device.getSnmpCommunity();
        int port = device.getSnmpPort() != null ? device.getSnmpPort() : DEFAULT_SNMP_PORT;

        if (ip == null || ip.isEmpty())
        {
            log.warn("getDeviceInfo: 设备 IP 为空, deviceId={}", device.getDeviceId());
            return info;
        }
        if (community == null || community.isEmpty())
        {
            log.warn("getDeviceInfo: 设备 SNMP 团体名为空, deviceId={}", device.getDeviceId());
            return info;
        }

        log.info("开始通过 SNMP 获取设备信息, deviceId={}, ip={}", device.getDeviceId(), ip);

        // sysName
        String sysName = snmpGet(ip, port, community, SYS_NAME);
        info.put("sysName", sysName != null ? sysName : "");

        // sysDescr
        String sysDescr = snmpGet(ip, port, community, SYS_DESCR);
        info.put("sysDescr", sysDescr != null ? sysDescr : "");

        // sysUpTime
        String sysUpTime = snmpGet(ip, port, community, SYS_UP_TIME);
        info.put("sysUpTime", sysUpTime != null ? sysUpTime : "");

        log.info("SNMP 设备信息获取完成, deviceId={}, sysName={}, sysUpTime={}",
                device.getDeviceId(), info.get("sysName"), info.get("sysUpTime"));

        return info;
    }

    /**
     * 获取设备端口状态（遍历 ifTable）
     *
     * @param device 设备对象（需包含 ipAddress, snmpCommunity, snmpPort）
     * @return Map<端口名称, 状态>（状态: up / down / unknown），失败返回空 Map
     */
    public Map<String, String> getPortStatus(Device device)
    {
        Map<String, String> portStatusMap = new LinkedHashMap<>();

        if (device == null)
        {
            log.warn("getPortStatus: device 为 null");
            return portStatusMap;
        }

        String ip = device.getIpAddress();
        String community = device.getSnmpCommunity();
        int port = device.getSnmpPort() != null ? device.getSnmpPort() : DEFAULT_SNMP_PORT;

        if (ip == null || ip.isEmpty())
        {
            log.warn("getPortStatus: 设备 IP 为空, deviceId={}", device.getDeviceId());
            return portStatusMap;
        }
        if (community == null || community.isEmpty())
        {
            log.warn("getPortStatus: 设备 SNMP 团体名为空, deviceId={}", device.getDeviceId());
            return portStatusMap;
        }

        log.info("开始通过 SNMP 获取端口状态, deviceId={}, ip={}", device.getDeviceId(), ip);

        // 遍历 ifDescr → 端口名称
        Map<String, String> descrMap = snmpWalk(ip, port, community, IF_DESCR);

        // 遍历 ifOperStatus → 端口状态
        Map<String, String> statusMap = snmpWalk(ip, port, community, IF_STATUS);

        // 如果要精确匹配端口名称与端口状态，需要对 OID 后缀做关联
        // ifDescr 和 ifOperStatus 的 OID 前缀不同，但后缀（ifIndex）相同
        // 例: ifDescr.1 = "eth0", ifStatus.1 = "1"
        // 这里简单方案：对 descrMap 按 OID 后缀构建名称映射，再匹配状态

        String descrPrefix = IF_DESCR + ".";
        String statusPrefix = IF_STATUS + ".";

        for (Map.Entry<String, String> descrEntry : descrMap.entrySet())
        {
            String descrOid = descrEntry.getKey();
            String portName = descrEntry.getValue();
            String suffix = descrOid.startsWith(descrPrefix)
                    ? descrOid.substring(descrPrefix.length())
                    : "";

            String statusOid = statusPrefix + suffix;
            String statusValue = statusMap.get(statusOid);
            String statusText = parseIfOperStatus(statusValue);

            portStatusMap.put(portName, statusText);
        }

        log.info("SNMP 端口状态获取完成, deviceId={}, 端口数={}",
                device.getDeviceId(), portStatusMap.size());

        return portStatusMap;
    }

    /**
     * 获取 NVR / 硬盘录像机存储使用情况
     * 通过 SNMP 遍历 HOST-RESOURCES-MIB 获取磁盘存储信息
     *
     * @param device 设备对象（NVR类型）
     * @return Map 包含存储描述、总容量、已用容量等
     */
    public Map<String, Object> getNvrStorageInfo(Device device)
    {
        Map<String, Object> info = new LinkedHashMap<>();

        if (device == null)
        {
            log.warn("getNvrStorageInfo: device 为 null");
            return info;
        }

        String ip = device.getIpAddress();
        String community = device.getSnmpCommunity();
        int port = device.getSnmpPort() != null ? device.getSnmpPort() : DEFAULT_SNMP_PORT;

        if (ip == null || ip.isEmpty() || community == null || community.isEmpty())
        {
            log.warn("getNvrStorageInfo: 设备 IP 或团体名为空");
            return info;
        }

        log.info("开始通过 SNMP 获取 NVR 存储信息, deviceId={}, ip={}", device.getDeviceId(), ip);

        // 获取存储描述
        Map<String, String> descrMap = snmpWalk(ip, port, community, HR_STORAGE_DESCR);
        // 获取存储总大小
        Map<String, String> sizeMap = snmpWalk(ip, port, community, HR_STORAGE_SIZE);
        // 获取存储已用大小
        Map<String, String> usedMap = snmpWalk(ip, port, community, HR_STORAGE_USED);

        List<Map<String, String>> storageList = new ArrayList<>();
        long totalSizeKB = 0;
        long totalUsedKB = 0;

        if (descrMap != null && !descrMap.isEmpty())
        {
            for (Map.Entry<String, String> entry : descrMap.entrySet())
            {
                String oidSuffix = entry.getKey();
                String descr = entry.getValue();

                Map<String, String> storage = new LinkedHashMap<>();
                storage.put("description", descr);

                String sizeStr = sizeMap != null ? sizeMap.getOrDefault(oidSuffix, "0") : "0";
                String usedStr = usedMap != null ? usedMap.getOrDefault(oidSuffix, "0") : "0";

                storage.put("totalKB", sizeStr);
                storage.put("usedKB", usedStr);

                try
                {
                    long sizeKB = Long.parseLong(sizeStr);
                    long usedKB = Long.parseLong(usedStr);
                    totalSizeKB += sizeKB;
                    totalUsedKB += usedKB;
                }
                catch (NumberFormatException e)
                {
                    // ignore
                }

                storageList.add(storage);
            }
        }

        info.put("storageList", storageList);
        info.put("totalSizeKB", totalSizeKB);
        info.put("totalUsedKB", totalUsedKB);
        info.put("totalSizeGB", String.format("%.2f", totalSizeKB / 1024.0 / 1024.0));
        info.put("totalUsedGB", String.format("%.2f", totalUsedKB / 1024.0 / 1024.0));

        log.info("NVR 存储信息获取完成, 总容量={}GB, 已用={}GB",
                info.get("totalSizeGB"), info.get("totalUsedGB"));

        return info;
    }

    /**
     * 执行自定义 SNMP 指令（GET 指定 OID）
     *
     * @param device 设备对象
     * @param oid 自定义 OID
     * @return OID 对应的值
     */
    public String executeCustomCommand(Device device, String oid)
    {
        if (device == null || oid == null || oid.isEmpty())
        {
            return null;
        }
        String ip = device.getIpAddress();
        String community = device.getSnmpCommunity();
        int port = device.getSnmpPort() != null ? device.getSnmpPort() : DEFAULT_SNMP_PORT;
        return snmpGet(ip, port, community, oid);
    }

    // ==================== SNMP 请求构建 ====================

    /**
     * 构建 SNMP v2c 请求报文
     *
     * @param pduType   PDU 类型（PDU_GET / PDU_GETNEXT）
     * @param community 团体名
     * @param oid       目标 OID
     * @return 完整 SNMP 报文（字节数组）
     */
    private byte[] buildRequest(int pduType, String community, String oid) throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        // --- PDU 内部: 可变绑定 ---
        ByteArrayOutputStream varbindList = new ByteArrayOutputStream();
        ByteArrayOutputStream varbind = new ByteArrayOutputStream();
        varbind.write(encodeOid(oid));       // ObjectName
        varbind.write(encodeNull());         // Value (unused for GET/GETNEXT)
        byte[] varbindBytes = varbind.toByteArray();
        varbindList.write(encodeTagLength(TAG_SEQUENCE, varbindBytes.length));
        varbindList.write(varbindBytes);

        byte[] varbindListBytes = varbindList.toByteArray();

        // --- PDU 头部: request-id(0), error(0), error-index(0) ---
        ByteArrayOutputStream pduBody = new ByteArrayOutputStream();
        pduBody.write(encodeInt(0));  // request-id (取巧用 0)
        pduBody.write(encodeInt(0));  // error-status
        pduBody.write(encodeInt(0));  // error-index
        pduBody.write(varbindListBytes);

        byte[] pduBodyBytes = pduBody.toByteArray();

        // PDU 整体用 context-specific 标签包裹
        ByteArrayOutputStream pdu = new ByteArrayOutputStream();
        pdu.write(encodeTagLength(pduType, pduBodyBytes.length));
        pdu.write(pduBodyBytes);
        byte[] pduBytes = pdu.toByteArray();

        // --- 外层 Sequence ---
        ByteArrayOutputStream seqBody = new ByteArrayOutputStream();
        seqBody.write(encodeInt(SNMP_VERSION_2C));       // version
        seqBody.write(encodeOctetString(community));     // community
        seqBody.write(pduBytes);                         // PDU

        byte[] seqBodyBytes = seqBody.toByteArray();
        buf.write(encodeTagLength(TAG_SEQUENCE, seqBodyBytes.length));
        buf.write(seqBodyBytes);

        return buf.toByteArray();
    }

    // ==================== UDP 通信 ====================

    /**
     * 通过 UDP 发送 SNMP 报文并接收响应
     *
     * @param ip      目标 IP
     * @param port    目标端口
     * @param request 请求报文
     * @return 响应报文字节数组，失败返回 null
     */
    private byte[] sendUdp(String ip, int port, byte[] request)
    {
        DatagramSocket socket = null;
        try
        {
            InetAddress address = InetAddress.getByName(ip);
            socket = new DatagramSocket();
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            // 发送
            DatagramPacket sendPacket = new DatagramPacket(request, request.length, address, port);
            socket.send(sendPacket);

            // 接收
            byte[] buf = new byte[65536];
            DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
            socket.receive(recvPacket);

            byte[] response = new byte[recvPacket.getLength()];
            System.arraycopy(recvPacket.getData(), 0, response, 0, recvPacket.getLength());
            return response;
        }
        catch (SocketTimeoutException e)
        {
            log.warn("SNMP 请求超时, ip={}, port={}", ip, port);
            return null;
        }
        catch (IOException e)
        {
            log.error("SNMP UDP 通信异常, ip={}, port={}", ip, port, e);
            return null;
        }
        finally
        {
            if (socket != null)
            {
                socket.close();
            }
        }
    }

    // ==================== SNMP 响应解码 ====================

    /**
     * 解码 SNMP 响应报文，提取可变绑定列表
     */
    private List<SnmpVarBind> decodeResponse(byte[] data) throws IOException
    {
        List<SnmpVarBind> result = new ArrayList<>();
        BERParser parser = new BERParser(data);

        // 外层 SEQUENCE
        parser.expectTag(TAG_SEQUENCE);
        int outerLen = parser.readLength();
        int outerEnd = parser.pos() + outerLen;

        // version (INTEGER) — 跳过
        parser.expectTag(TAG_INTEGER);
        int verLen = parser.readLength();
        parser.skip(verLen);

        // community (OCTET STRING) — 跳过
        parser.expectTag(TAG_OCTET_STRING);
        int comLen = parser.readLength();
        parser.skip(comLen);

        // 响应 PDU (0xA2)
        int pduTag = parser.readTag();
        if (pduTag != PDU_RESPONSE)
        {
            log.warn("SNMP 响应 PDU 标签异常: 0x{} (期望 0xA2)", Integer.toHexString(pduTag));
            return result;
        }
        int pduLen = parser.readLength();
        int pduEnd = parser.pos() + pduLen;

        // request-id (INTEGER)
        parser.expectTag(TAG_INTEGER);
        int reqIdLen = parser.readLength();
        parser.skip(reqIdLen);

        // error-status (INTEGER)
        parser.expectTag(TAG_INTEGER);
        int errStatLen = parser.readLength();
        int errorStatus = parser.readIntValue(errStatLen);
        if (errorStatus != 0)
        {
            log.warn("SNMP 响应错误状态: {}", errorStatus);
        }

        // error-index (INTEGER)
        parser.expectTag(TAG_INTEGER);
        int errIdxLen = parser.readLength();
        parser.skip(errIdxLen);

        // variable-bindings (SEQUENCE OF)
        parser.expectTag(TAG_SEQUENCE);
        int vbListLen = parser.readLength();
        int vbListEnd = parser.pos() + vbListLen;

        while (parser.pos() < vbListEnd && parser.pos() < pduEnd)
        {
            parser.expectTag(TAG_SEQUENCE);
            int vbLen = parser.readLength();
            int vbEnd = parser.pos() + vbLen;

            // OID
            parser.expectTag(TAG_OID);
            int oidLen = parser.readLength();
            String oid = parser.readOid(oidLen);

            // Value（任意类型）
            String value = parser.readAnyValue();

            result.add(new SnmpVarBind(oid, value));

            // 确保位置正确（跳过可能的填充）
            parser.seek(vbEnd);
        }

        parser.seek(outerEnd);
        return result;
    }

    // ==================== ASN.1 编码工具方法 ====================

    /**
     * 编码 Tag + Length 头部
     */
    private static byte[] encodeTagLength(int tag, int length) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) tag);
        out.write(encodeLength(length));
        return out.toByteArray();
    }

    /**
     * 编码 ASN.1 Length 字段
     */
    private static byte[] encodeLength(int length)
    {
        if (length < 128)
        {
            return new byte[] { (byte) length };
        }
        // 长格式: 0x80 | n, 后跟 n 字节的大端表示
        byte[] lenBytes = toMinimalBytes(length);
        byte[] result = new byte[1 + lenBytes.length];
        result[0] = (byte) (0x80 | lenBytes.length);
        System.arraycopy(lenBytes, 0, result, 1, lenBytes.length);
        return result;
    }

    /**
     * 编码 INTEGER
     */
    private static byte[] encodeInt(int value) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) TAG_INTEGER);
        byte[] valBytes = toMinimalBytes(value);
        out.write(encodeLength(valBytes.length));
        out.write(valBytes);
        return out.toByteArray();
    }

    /**
     * 编码 OCTET STRING
     */
    private static byte[] encodeOctetString(String value) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) TAG_OCTET_STRING);
        byte[] strBytes = value.getBytes("US-ASCII");
        out.write(encodeLength(strBytes.length));
        out.write(strBytes);
        return out.toByteArray();
    }

    /**
     * 编码 NULL
     */
    private static byte[] encodeNull() throws IOException
    {
        return new byte[] { (byte) TAG_NULL, 0x00 };
    }

    /**
     * 编码 OID
     *
     * 如 1.3.6.1.2.1.1.5.0
     * 第一个子标识符 = 40 * 1 + 3 = 43 (0x2B)
     * 后续每个用 base-128（7-bit）编码
     */
    private static byte[] encodeOid(String oid) throws IOException
    {
        String[] parts = oid.split("\\.");
        int[] subids = new int[parts.length];
        for (int i = 0; i < parts.length; i++)
        {
            subids[i] = Integer.parseInt(parts[i]);
        }

        ByteArrayOutputStream oidBytes = new ByteArrayOutputStream();

        // 前两个子标识符合并编码
        if (subids.length >= 2)
        {
            oidBytes.write(40 * subids[0] + subids[1]);
        }

        // 后续子标识符用 base-128 编码
        for (int i = 2; i < subids.length; i++)
        {
            byte[] encoded = encodeBase128(subids[i]);
            oidBytes.write(encoded);
        }

        byte[] oidData = oidBytes.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) TAG_OID);
        out.write(encodeLength(oidData.length));
        out.write(oidData);
        return out.toByteArray();
    }

    /**
     * Base-128 编码（7-bit 分组，高位置 1 表示还有后续字节）
     */
    private static byte[] encodeBase128(int value)
    {
        if (value < 128)
        {
            return new byte[] { (byte) value };
        }
        // 计算需要多少字节
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        while (value > 0)
        {
            temp.write(value & 0x7F);
            value >>= 7;
        }
        byte[] raw = temp.toByteArray();
        byte[] result = new byte[raw.length];
        for (int i = 0; i < raw.length; i++)
        {
            // 最后一个字节（最高位）MSB = 0，其余 MSB = 1
            if (i == raw.length - 1)
            {
                result[i] = raw[i];
            }
            else
            {
                result[i] = (byte) (raw[i] | 0x80);
            }
        }
        // 反转（大端序）
        reverse(result);
        return result;
    }

    /**
     * 整数转最小字节表示（大端）
     */
    private static byte[] toMinimalBytes(int value)
    {
        if (value == 0)
        {
            return new byte[] { 0 };
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int v = value;
        while (v != 0 && v != -1)
        {
            out.write(v & 0xFF);
            v >>= 8;
        }
        byte[] raw = out.toByteArray();
        reverse(raw);
        return raw;
    }

    private static void reverse(byte[] arr)
    {
        for (int i = 0, j = arr.length - 1; i < j; i++, j--)
        {
            byte tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 将 ifOperStatus 数值转换为可读字符串
     *
     * @param statusValue SNMP 返回的状态值（字符串形式的整数）
     * @return up / down / testing / unknown / 其他
     */
    private String parseIfOperStatus(String statusValue)
    {
        if (statusValue == null || statusValue.isEmpty())
        {
            return "unknown";
        }
        try
        {
            int status = Integer.parseInt(statusValue.trim());
            switch (status)
            {
                case 1:  return "up";
                case 2:  return "down";
                case 3:  return "testing";
                case 4:  return "unknown";
                case 5:  return "dormant";
                case 6:  return "notPresent";
                case 7:  return "lowerLayerDown";
                default: return String.valueOf(status);
            }
        }
        catch (NumberFormatException e)
        {
            return statusValue;
        }
    }

    // ==================== 内部类 ====================

    /**
     * 可变绑定
     */
    private static class SnmpVarBind
    {
        final String oid;
        final String value;

        SnmpVarBind(String oid, String value)
        {
            this.oid = oid;
            this.value = value;
        }
    }

    /**
     * ASN.1 BER 解码器
     */
    private static class BERParser
    {
        private final byte[] data;
        private int offset;

        BERParser(byte[] data)
        {
            this.data = data;
            this.offset = 0;
        }

        int pos()
        {
            return offset;
        }

        void seek(int position)
        {
            if (position >= 0 && position <= data.length)
            {
                this.offset = position;
            }
        }

        int readTag() throws IOException
        {
            checkBounds(1);
            return data[offset++] & 0xFF;
        }

        void expectTag(int expectedTag) throws IOException
        {
            int tag = readTag();
            if (tag != expectedTag)
            {
                throw new IOException(String.format(
                        "ASN.1 标签不匹配: 期望 0x%02X, 实际 0x%02X (位置=%d)",
                        expectedTag, tag, offset - 1));
            }
        }

        int readLength() throws IOException
        {
            checkBounds(1);
            int b = data[offset++] & 0xFF;
            if (b < 128)
            {
                return b;
            }
            int numBytes = b & 0x7F;
            if (numBytes == 0)
            {
                throw new IOException("ASN.1 不定长格式不支持");
            }
            checkBounds(numBytes);
            int length = 0;
            for (int i = 0; i < numBytes; i++)
            {
                length = (length << 8) | (data[offset++] & 0xFF);
            }
            return length;
        }

        void skip(int numBytes)
        {
            offset += numBytes;
        }

        int readIntValue(int length) throws IOException
        {
            checkBounds(length);
            int value = 0;
            for (int i = 0; i < length; i++)
            {
                value = (value << 8) | (data[offset++] & 0xFF);
            }
            return value;
        }

        String readOid(int length) throws IOException
        {
            checkBounds(length);
            int end = offset + length;

            // 第一个字节解出前两个子标识符
            int first = data[offset++] & 0xFF;
            StringBuilder sb = new StringBuilder();
            sb.append(first / 40).append('.').append(first % 40);

            // 后续每个子标识符用 base-128 解码
            while (offset < end)
            {
                long subid = 0;
                int b;
                do
                {
                    checkBounds(1);
                    b = data[offset++] & 0xFF;
                    subid = (subid << 7) | (b & 0x7F);
                }
                while ((b & 0x80) != 0 && offset <= end);
                sb.append('.').append(subid);
            }

            return sb.toString();
        }

        /**
         * 读取任意 ASN.1 值并转为字符串
         */
        String readAnyValue() throws IOException
        {
            checkBounds(1);
            int tag = data[offset++] & 0xFF;
            checkBounds(1);
            int len = readLength();
            checkBounds(len);

            int start = offset;
            String value;
            switch (tag)
            {
                case TAG_INTEGER:
                {
                    // 简单整数解析（<= 4 字节）
                    int v = 0;
                    for (int i = 0; i < len; i++)
                    {
                        v = (v << 8) | (data[offset++] & 0xFF);
                    }
                    value = String.valueOf(v);
                    break;
                }
                case TAG_OCTET_STRING:
                case TAG_IP_ADDRESS:
                {
                    value = new String(data, offset, len, "US-ASCII");
                    // IP Address 特殊处理：4 字节转点分十进制
                    if (tag == TAG_IP_ADDRESS && len == 4)
                    {
                        StringBuilder ip = new StringBuilder();
                        for (int i = 0; i < 4; i++)
                        {
                            if (i > 0) ip.append('.');
                            ip.append(data[offset + i] & 0xFF);
                        }
                        value = ip.toString();
                    }
                    offset += len;
                    break;
                }
                case TAG_NULL:
                {
                    value = "";
                    offset += len;
                    break;
                }
                case TAG_OID:
                {
                    value = readOid(len);
                    break;
                }
                case TAG_TIMETICKS:
                {
                    long v = 0;
                    for (int i = 0; i < len; i++)
                    {
                        v = (v << 8) | (data[offset++] & 0xFF);
                    }
                    value = String.valueOf(v); // 单位: 1/100 秒
                    break;
                }
                case TAG_COUNTER:
                case TAG_GAUGE:
                {
                    long v = 0;
                    for (int i = 0; i < len; i++)
                    {
                        v = (v << 8) | (data[offset++] & 0xFF);
                    }
                    value = String.valueOf(v);
                    break;
                }
                default:
                {
                    // 未识别的类型: 读为十六进制
                    StringBuilder hex = new StringBuilder();
                    for (int i = 0; i < len && i < 64; i++)
                    {
                        hex.append(String.format("%02X", data[offset + i] & 0xFF));
                    }
                    value = hex.toString();
                    offset += len;
                    break;
                }
            }

            // 确保 offset 前进到值末尾
            offset = start + len;
            return value;
        }

        private void checkBounds(int needed) throws IOException
        {
            if (offset + needed > data.length)
            {
                throw new IOException(String.format(
                        "ASN.1 解码越界: offset=%d, needed=%d, length=%d",
                        offset, needed, data.length));
            }
        }
    }
}
