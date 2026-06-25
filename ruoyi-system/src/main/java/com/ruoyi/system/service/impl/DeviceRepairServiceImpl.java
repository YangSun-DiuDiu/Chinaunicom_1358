package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DeviceRepair;
import com.ruoyi.system.mapper.DeviceRepairMapper;
import com.ruoyi.system.service.IDeviceRepairService;

/**
 * 设备维修工单服务实现
 *
 * @author ruoyi
 */
@Service
public class DeviceRepairServiceImpl implements IDeviceRepairService
{
    @Autowired
    private DeviceRepairMapper repairMapper;

    @Override
    public List<DeviceRepair> selectRepairList(DeviceRepair repair) {
        return repairMapper.selectRepairList(repair);
    }

    @Override
    public DeviceRepair selectRepairById(Long repairId) {
        return repairMapper.selectRepairById(repairId);
    }

    @Override
    public DeviceRepair selectRepairByToken(String token) {
        return repairMapper.selectRepairByToken(token);
    }

    @Override
    public int insertRepair(DeviceRepair repair) {
        return repairMapper.insertRepair(repair);
    }

    @Override
    public int updateRepair(DeviceRepair repair) {
        return repairMapper.updateRepair(repair);
    }

    @Override
    public int deleteRepairById(Long repairId) {
        return repairMapper.deleteRepairById(repairId);
    }

    @Override
    public int transferRepair(Long repairId, String transferTo, String transferToPhone, String reason, String updateBy) {
        DeviceRepair repair = repairMapper.selectRepairById(repairId);
        if (repair == null) throw new RuntimeException("工单不存在");
        if (!"PENDING".equals(repair.getStatus()) && !"ACCEPTED".equals(repair.getStatus())) {
            throw new RuntimeException("当前工单状态不允许转派");
        }
        DeviceRepair update = new DeviceRepair();
        update.setRepairId(repairId);
        update.setTransferFrom(repair.getCurrentResponsible());
        update.setTransferTo(transferTo);
        update.setCurrentResponsible(transferTo);
        if (StringUtils.isNotEmpty(transferToPhone)) {
            update.setCurrentPhone(transferToPhone);
        }
        update.setTransferReason(reason);
        update.setStatus("ASSIGNED");
        update.setUpdateBy(updateBy);
        return repairMapper.updateRepair(update);
    }

    @Override
    public int acceptRepair(Long repairId, String updateBy) {
        DeviceRepair repair = repairMapper.selectRepairById(repairId);
        if (repair == null) throw new RuntimeException("工单不存在");
        if (!"ASSIGNED".equals(repair.getStatus())) {
            throw new RuntimeException("当前工单状态不允许接收");
        }
        DeviceRepair update = new DeviceRepair();
        update.setRepairId(repairId);
        update.setStatus("ACCEPTED");
        update.setUpdateBy(updateBy);
        return repairMapper.updateRepair(update);
    }

    @Override
    public int rejectRepair(Long repairId, String reason, String updateBy) {
        DeviceRepair repair = repairMapper.selectRepairById(repairId);
        if (repair == null) throw new RuntimeException("工单不存在");
        if (!"ASSIGNED".equals(repair.getStatus())) {
            throw new RuntimeException("当前工单状态不允许拒绝");
        }
        DeviceRepair update = new DeviceRepair();
        update.setRepairId(repairId);
        update.setStatus("REJECTED");
        update.setTransferReason(reason);
        // 退回到原责任人
        update.setCurrentResponsible(repair.getTransferFrom());
        update.setUpdateBy(updateBy);
        return repairMapper.updateRepair(update);
    }

    @Override
    public int completeRepairByToken(String token, String repairResult, String photoBefore, String photoAfter,
                                      String hasParts, String partsDesc) {
        DeviceRepair repair = repairMapper.selectRepairByToken(token);
        if (repair == null) throw new RuntimeException("无效的维修确认链接");
        if ("COMPLETED".equals(repair.getStatus())) {
            throw new RuntimeException("该工单已完成维修");
        }
        DeviceRepair update = new DeviceRepair();
        update.setRepairId(repair.getRepairId());
        update.setStatus("COMPLETED");
        update.setRepairResult(repairResult);
        update.setRepairTime(new Date());
        update.setUpdateBy(repair.getCurrentResponsible());
        if (photoBefore != null && !photoBefore.isEmpty()) update.setPhotoBefore(photoBefore);
        if (photoAfter != null && !photoAfter.isEmpty()) update.setPhotoAfter(photoAfter);
        if (hasParts != null) update.setHasParts(hasParts);
        if (partsDesc != null && !partsDesc.isEmpty()) update.setPartsDesc(partsDesc);
        int result = repairMapper.updateRepair(update);

        // 如果使用了配件，逐条记录配件使用情况（逗号/中文逗号分隔）
        if ("1".equals(hasParts) && partsDesc != null && !partsDesc.isEmpty()) {
            String[] parts = partsDesc.split("[,，;；]");
            for (String part : parts) {
                String pn = part.trim();
                if (!pn.isEmpty()) {
                    repairMapper.insertPartsUsage(repair.getRepairId(), pn, repair.getCurrentResponsible());
                }
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> selectWorkloadStats() {
        return repairMapper.selectWorkloadStats();
    }
}
