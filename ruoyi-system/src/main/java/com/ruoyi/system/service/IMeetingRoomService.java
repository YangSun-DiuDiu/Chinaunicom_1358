package com.ruoyi.system.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.domain.MeetingRoom;

/**
 * 会议室 服务层
 *
 * @author ruoyi
 */
public interface IMeetingRoomService
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
     * 批量删除会议室
     *
     * @param roomIds 需要删除的会议室ID
     */
    public void deleteMeetingRoomByIds(Long[] roomIds);

    /**
     * 计算会议室当前使用状态（会议室看板/状态总览公共逻辑）
     * <ul>
     *   <li>IN_USE：当前有 APPROVED 预约正在进行中（startTime <= now < endTime）</li>
     *   <li>COMING_SOON：当前空闲，但下一预约在 15 分钟内开始</li>
     *   <li>FREE：当前空闲，且无即将开始的预约</li>
     * </ul>
     *
     * @param todayList 会议室当日预约列表（已按开始时间升序）
     * @return 状态码 IN_USE / COMING_SOON / FREE
     */
    public String calcCurrentStatus(List<MeetingBooking> todayList);

    /**
     * 获取当前正在进行的预约（APPROVED 且 startTime <= now < endTime）
     *
     * @param todayList 会议室当日预约列表
     * @return 当前预约，无则为 null
     */
    public MeetingBooking getCurrentBooking(List<MeetingBooking> todayList);

    /**
     * 获取下一场即将开始的预约（APPROVED 且 startTime > now 中最近的一场）
     *
     * @param todayList 会议室当日预约列表
     * @return 下一预约，无则为 null
     */
    public MeetingBooking getNextBooking(List<MeetingBooking> todayList);
}
