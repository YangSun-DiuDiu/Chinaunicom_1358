package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.MeetingRoom;

/**
 * 会议室 数据层
 *
 * @author ruoyi
 */
public interface MeetingRoomMapper
{
    /**
     * 查询会议室列表
     *
     * @param meetingRoom 会议室信息
     * @return 会议室集合
     */
    public List<MeetingRoom> selectMeetingRoomList(MeetingRoom meetingRoom);

    /**
     * 根据会议室ID查询信息
     *
     * @param roomId 会议室ID
     * @return 会议室信息
     */
    public MeetingRoom selectMeetingRoomById(Long roomId);

    /**
     * 新增会议室
     *
     * @param meetingRoom 会议室信息
     * @return 结果
     */
    public int insertMeetingRoom(MeetingRoom meetingRoom);

    /**
     * 修改会议室
     *
     * @param meetingRoom 会议室信息
     * @return 结果
     */
    public int updateMeetingRoom(MeetingRoom meetingRoom);

    /**
     * 删除会议室
     *
     * @param roomId 会议室ID
     * @return 结果
     */
    public int deleteMeetingRoomById(Long roomId);

    /**
     * 批量删除会议室
     *
     * @param roomIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteMeetingRoomByIds(Long[] roomIds);
}
