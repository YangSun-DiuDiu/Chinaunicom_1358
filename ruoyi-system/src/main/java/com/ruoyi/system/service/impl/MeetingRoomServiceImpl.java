package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MeetingRoom;
import com.ruoyi.system.mapper.MeetingRoomMapper;
import com.ruoyi.system.service.IMeetingRoomService;

/**
 * 会议室 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MeetingRoomServiceImpl implements IMeetingRoomService
{
    @Autowired
    private MeetingRoomMapper meetingRoomMapper;

    /**
     * 查询会议室列表
     *
     * @param meetingRoom 会议室信息
     * @return 会议室集合
     */
    @Override
    public List<MeetingRoom> selectMeetingRoomList(MeetingRoom meetingRoom)
    {
        return meetingRoomMapper.selectMeetingRoomList(meetingRoom);
    }

    /**
     * 根据会议室ID查询信息
     *
     * @param roomId 会议室ID
     * @return 会议室信息
     */
    @Override
    public MeetingRoom selectMeetingRoomById(Long roomId)
    {
        return meetingRoomMapper.selectMeetingRoomById(roomId);
    }

    /**
     * 新增会议室
     *
     * @param meetingRoom 会议室信息
     * @return 结果
     */
    @Override
    public int insertMeetingRoom(MeetingRoom meetingRoom)
    {
        return meetingRoomMapper.insertMeetingRoom(meetingRoom);
    }

    /**
     * 修改会议室
     *
     * @param meetingRoom 会议室信息
     * @return 结果
     */
    @Override
    public int updateMeetingRoom(MeetingRoom meetingRoom)
    {
        return meetingRoomMapper.updateMeetingRoom(meetingRoom);
    }

    /**
     * 批量删除会议室
     *
     * @param roomIds 需要删除的会议室ID
     */
    @Override
    public void deleteMeetingRoomByIds(Long[] roomIds)
    {
        for (Long roomId : roomIds)
        {
            meetingRoomMapper.deleteMeetingRoomById(roomId);
        }
    }
}
