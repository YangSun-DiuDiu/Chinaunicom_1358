package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.RoomInfo;

/**
 * 房间信息 服务层
 *
 * @author ruoyi
 */
public interface IRoomInfoService
{
    /**
     * 查询房间列表
     *
     * @param roomInfo 房间信息
     * @return 房间集合
     */
    public List<RoomInfo> selectRoomInfoList(RoomInfo roomInfo);

    /**
     * 根据房间ID查询信息
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    public RoomInfo selectRoomInfoById(Long roomId);

    /**
     * 查询房间卡片列表（支持公寓ID/楼栋/楼层/状态筛选）
     *
     * @param roomInfo 房间信息
     * @return 房间集合
     */
    public List<RoomInfo> selectRoomCardList(RoomInfo roomInfo);

    /**
     * 新增房间
     *
     * @param roomInfo 房间信息
     * @return 结果
     */
    public int insertRoomInfo(RoomInfo roomInfo);

    /**
     * 修改房间
     *
     * @param roomInfo 房间信息
     * @return 结果
     */
    public int updateRoomInfo(RoomInfo roomInfo);

    /**
     * 批量删除房间
     *
     * @param roomIds 需要删除的房间ID
     */
    public void deleteRoomInfoByIds(Long[] roomIds);
}
