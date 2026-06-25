package com.ruoyi.web.controller.device;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DeviceRepair;
import com.ruoyi.system.service.IDeviceRepairService;

/**
 * 设备维修公开接口——无需登录，通过短信链接访问
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/device/repair/public")
public class DeviceRepairPublicController extends BaseController
{
    @Autowired
    private IDeviceRepairService repairService;

    /** 根据token查询工单信息 */
    @GetMapping("/detail")
    public AjaxResult detail(@RequestParam String token) {
        if (StringUtils.isEmpty(token)) return error("无效链接");
        DeviceRepair repair = repairService.selectRepairByToken(token);
        if (repair == null) return error("无效的维修确认链接");
        return success(repair);
    }

    /** 维修照片上传（免登录） */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return error("文件为空");
            // 手动保存文件
            String uploadDir = RuoYiConfig.getUploadPath() + "/repair";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
            String newName = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
            File dest = new File(dir, newName);
            file.transferTo(dest);
            String url = "/profile/repair/" + newName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            return ajax;
        } catch (Exception e) {
            return error("上传失败: " + e.getMessage());
        }
    }

    /** 提交维修完成确认（含照片、配件） */
    @PostMapping("/complete")
    public AjaxResult complete(@RequestBody Map<String, String> params) {
        String token = params.get("token");
        String result = params.get("result");
        String photoBefore = params.get("photoBefore");
        String photoAfter = params.get("photoAfter");
        String hasParts = params.get("hasParts");
        String partsDesc = params.get("partsDesc");
        if (StringUtils.isEmpty(token)) return error("无效链接");
        try {
            repairService.completeRepairByToken(token, result, photoBefore, photoAfter, hasParts, partsDesc);
            return success("维修完成确认成功");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
