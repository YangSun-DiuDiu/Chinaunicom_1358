package com.ruoyi.system.service;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.Device;

/**
 * SNMP配置文件管理服务
 */
public interface ISnmpConfigService
{
    /** 上传/生成设备专属 toml 配置文件并部署到Categraf目录 */
    Map<String, Object> deployConfig(Device device, MultipartFile file, String templateType) throws Exception;

    /** 删除设备时同步删除toml配置文件（移入bak目录） */
    void removeConfig(Device device) throws Exception;

    /** 校验toml文件格式合法性 */
    boolean validateToml(String content);
}
