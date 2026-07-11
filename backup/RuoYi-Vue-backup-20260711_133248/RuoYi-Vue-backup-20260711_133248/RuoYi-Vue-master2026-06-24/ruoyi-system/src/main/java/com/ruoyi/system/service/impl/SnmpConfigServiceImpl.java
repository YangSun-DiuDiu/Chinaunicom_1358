package com.ruoyi.system.service.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.service.ISnmpConfigService;

/**
 * SNMP配置文件管理服务实现
 */
@Service
public class SnmpConfigServiceImpl implements ISnmpConfigService
{
    private static final Logger log = LoggerFactory.getLogger(SnmpConfigServiceImpl.class);

    /** Categraf SNMP配置目录 */
    @Value("${snmp.categraf.confDir:/opt/categraf/conf/input.snmp}")
    private String categrafConfDir;

    /** 后端中转目录 */
    @Value("${snmp.upload.tempDir:/home/ruoyi/uploadPath/categraf_snmp/temp}")
    private String tempDir;

    @Override
    public Map<String, Object> deployConfig(Device device, MultipartFile file, String templateType) throws Exception
    {
        Map<String, Object> result = new HashMap<>();
        String fileName;
        String content;

        if (file != null && !file.isEmpty())
        {
            // 自定义上传模式：校验文件
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (!validateToml(content))
            {
                result.put("success", false);
                result.put("msg", "TOML文件格式校验失败");
                return result;
            }
            fileName = "dev_" + (device.getIpAddress() != null ? device.getIpAddress().replace(".", "_") : IdUtils.fastSimpleUUID()) + ".toml";
        }
        else
        {
            // 模板模式：根据设备类型生成配置
            fileName = "dev_" + (device.getIpAddress() != null ? device.getIpAddress().replace(".", "_") : IdUtils.fastSimpleUUID()) + ".toml";
            content = generateTemplateConfig(device, templateType);
        }

        // 1. 保存到中转目录
        Files.createDirectories(Paths.get(tempDir));
        Path tempPath = Paths.get(tempDir, fileName);
        Files.write(tempPath, content.getBytes(StandardCharsets.UTF_8));

        // 2. 复制到Categraf目录
        Files.createDirectories(Paths.get(categrafConfDir));
        Path targetPath = Paths.get(categrafConfDir, fileName);
        Files.copy(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 3. 计算MD5
        String md5 = md5(content);

        // 4. 更新设备记录
        device.setSnmpConfigFile(fileName);
        device.setConfigContent(content);
        device.setConfigMd5(md5);

        result.put("success", true);
        result.put("fileName", fileName);
        result.put("md5", md5);
        result.put("content", content);
        return result;
    }

    @Override
    public void removeConfig(Device device) throws Exception
    {
        String fileName = device.getSnmpConfigFile();
        if (fileName == null || fileName.isEmpty()) return;

        // 移入bak目录
        Path src = Paths.get(categrafConfDir, fileName);
        if (Files.exists(src))
        {
            Path bakDir = Paths.get(categrafConfDir, "bak");
            Files.createDirectories(bakDir);
            Files.move(src, Paths.get(bakDir.toString(), fileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public boolean validateToml(String content)
    {
        if (content == null || content.trim().isEmpty()) return false;
        // 基本校验：必须以 [[instances]] 或 [inputs.snmp] 开头
        String trimmed = content.trim();
        return trimmed.contains("[[instances]]") || trimmed.contains("[inputs.snmp]")
            || trimmed.contains("agent_host_ip") || trimmed.contains("[[inputs.snmp]]");
    }

    private String generateTemplateConfig(Device device, String templateType)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("# SNMP采集配置 - ").append(device.getDeviceName()).append("\n");
        sb.append("# 自动生成，设备类型: ").append(templateType != null ? templateType : "默认").append("\n\n");
        sb.append("[[instances]]\n");
        sb.append("agent_host_ip = \"").append(device.getIpAddress() != null ? device.getIpAddress() : "").append("\"\n");
        sb.append("agent_host_port = ").append(device.getSnmpPort() != null ? device.getSnmpPort() : 161).append("\n");
        String version = "v2c".equals(device.getSnmpVersion()) ? "2c" : (device.getSnmpVersion() != null ? device.getSnmpVersion() : "2c");
        sb.append("snmp_version = \"").append(version).append("\"\n");
        sb.append("community = \"").append(device.getSnmpCommunity() != null ? device.getSnmpCommunity() : "public").append("\"\n");
        sb.append("interval = \"5m\"\n");
        return sb.toString();
    }

    private String md5(String input) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
