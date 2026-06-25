package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.DeviceRepair;

/**
 * 设备维修工单服务接口
 *
 * @author ruoyi
 */
public interface IDeviceRepairService
{
    public List<DeviceRepair> selectRepairList(DeviceRepair repair);
    public DeviceRepair selectRepairById(Long repairId);
    public DeviceRepair selectRepairByToken(String token);
    public int insertRepair(DeviceRepair repair);
    public int updateRepair(DeviceRepair repair);
    public int deleteRepairById(Long repairId);
    /** 转派工单给其他责任人 */
    public int transferRepair(Long repairId, String transferTo, String transferToPhone, String reason, String updateBy);
    /** 接收转派 */
    public int acceptRepair(Long repairId, String updateBy);
    /** 拒绝转派 */
    public int rejectRepair(Long repairId, String reason, String updateBy);
    /** 完成维修(公开接口,通过token确认) */
    public int completeRepairByToken(String token, String repairResult, String photoBefore, String photoAfter,
                                      String hasParts, String partsDesc);
    /** 工作量统计 */
    public List<Map<String, Object>> selectWorkloadStats();
}
