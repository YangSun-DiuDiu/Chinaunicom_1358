package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.DeviceRepair;

/**
 * 设备维修工单Mapper
 *
 * @author ruoyi
 */
public interface DeviceRepairMapper
{
    public List<DeviceRepair> selectRepairList(DeviceRepair repair);
    public DeviceRepair selectRepairById(Long repairId);
    public DeviceRepair selectRepairByToken(String token);
    public int insertRepair(DeviceRepair repair);
    public int updateRepair(DeviceRepair repair);
    public int deleteRepairById(Long repairId);
    /** 工作量统计: 按责任人统计各状态工单数量 */
    public List<Map<String, Object>> selectWorkloadStats();
    /** 记录配件使用 */
    public int insertPartsUsage(Long repairId, String partName, String usedBy);
}
