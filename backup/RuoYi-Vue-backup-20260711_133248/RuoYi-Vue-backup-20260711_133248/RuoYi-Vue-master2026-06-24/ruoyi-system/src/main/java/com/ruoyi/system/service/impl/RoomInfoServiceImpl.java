package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.RoomInfo;
import com.ruoyi.system.mapper.RoomInfoMapper;
import com.ruoyi.system.service.IRoomInfoService;

/**
 * 房间信息 服务层实现
 *
 * @author ruoyi
 */
@Service
public class RoomInfoServiceImpl implements IRoomInfoService
{
    @Autowired
    private RoomInfoMapper roomInfoMapper;

    /**
     * 查询房间列表
     *
     * @param roomInfo 房间信息
     * @return 房间集合
     */
    @Override
    public List<RoomInfo> selectRoomInfoList(RoomInfo roomInfo)
    {
        return roomInfoMapper.selectRoomInfoList(roomInfo);
    }

    /**
     * 根据房间ID查询信息
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    @Override
    public RoomInfo selectRoomInfoById(Long roomId)
    {
        return roomInfoMapper.selectRoomInfoById(roomId);
    }

    /**
     * 查询房间卡片列表（支持公寓ID/楼栋/楼层/状态筛选）
     *
     * @param roomInfo 房间信息
     * @return 房间集合
     */
    @Override
    public List<RoomInfo> selectRoomCardList(RoomInfo roomInfo)
    {
        return roomInfoMapper.selectRoomCardList(roomInfo);
    }

    /**
     * 新增房间
     *
     * @param roomInfo 房间信息
     * @return 结果
     */
    @Override
    public int insertRoomInfo(RoomInfo roomInfo)
    {
        return roomInfoMapper.insertRoomInfo(roomInfo);
    }

    /**
     * 修改房间
     *
     * @param roomInfo 房间信息
     * @return 结果
     */
    @Override
    public int updateRoomInfo(RoomInfo roomInfo)
    {
        return roomInfoMapper.updateRoomInfo(roomInfo);
    }

    /**
     * 批量删除房间
     *
     * @param roomIds 需要删除的房间ID
     */
    @Override
    public void deleteRoomInfoByIds(Long[] roomIds)
    {
        for (Long roomId : roomIds)
        {
            roomInfoMapper.deleteRoomInfoById(roomId);
        }
    }
}
