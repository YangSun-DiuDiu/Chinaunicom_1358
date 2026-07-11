package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.mapper.MeetingBookingMapper;
import com.ruoyi.system.service.IMeetingBookingService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.sms.SmsUtil;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 会议预约 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MeetingBookingServiceImpl implements IMeetingBookingService
{
    private static final Logger log = LoggerFactory.getLogger(MeetingBookingServiceImpl.class);

    @Autowired
    private MeetingBookingMapper meetingBookingMapper;

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private ISysUserService sysUserService;

    /**
     * 查询会议预约列表
     *
     * @param meetingBooking 会议预约信息
     * @return 会议预约集合
     */
    @Override
    public List<MeetingBooking> selectMeetingBookingList(MeetingBooking meetingBooking)
    {
        return meetingBookingMapper.selectMeetingBookingList(meetingBooking);
    }

    /**
     * 根据预约ID查询信息
     *
     * @param bookingId 预约ID
     * @return 会议预约信息
     */
    @Override
    public MeetingBooking selectMeetingBookingById(Long bookingId)
    {
        return meetingBookingMapper.selectMeetingBookingById(bookingId);
    }

    /**
     * 新增会议预约
     *
     * @param meetingBooking 会议预约信息
     * @return 结果
     */
    @Override
    public int insertMeetingBooking(MeetingBooking meetingBooking)
    {
        int rows = meetingBookingMapper.insertMeetingBooking(meetingBooking);
        notifyApproversOfNewBooking(meetingBooking);
        return rows;
    }

    /**
     * 修改会议预约
     *
     * @param meetingBooking 会议预约信息
     * @return 结果
     */
    @Override
    public int updateMeetingBooking(MeetingBooking meetingBooking)
    {
        return meetingBookingMapper.updateMeetingBooking(meetingBooking);
    }

    /**
     * 批量删除会议预约
     *
     * @param bookingIds 需要删除的预约ID
     */
    @Override
    public void deleteMeetingBookingByIds(Long[] bookingIds)
    {
        for (Long bookingId : bookingIds)
        {
            meetingBookingMapper.deleteMeetingBookingById(bookingId);
        }
    }

    /**
     * 判断时间段是否有冲突
     *
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeBookingId 排除的预约ID
     * @return true 有冲突，false 无冲突
     */
    @Override
    public boolean hasConflict(Long roomId, Date startTime, Date endTime, Long excludeBookingId)
    {
        int count = meetingBookingMapper.countConflict(roomId, startTime, endTime, excludeBookingId);
        return count > 0;
    }

    /**
     * 查询今日指定会议室的预约
     *
     * @param roomId 会议室ID
     * @return 今日预约列表
     */
    @Override
    public List<MeetingBooking> selectTodayByRoom(Long roomId)
    {
        return meetingBookingMapper.selectTodayByRoom(roomId);
    }

    /**
     * 查询指定会议室指定日期的已占用时间段
     *
     * @param roomId 会议室ID
     * @param date 日期 (yyyy-MM-dd)
     * @return 预约列表
     */
    @Override
    public List<MeetingBooking> selectSlots(Long roomId, String date)
    {
        return meetingBookingMapper.selectSlots(roomId, date);
    }

    /**
     * 审批会议预约（通过/拒绝），完成后短信通知主持人
     */
    public int approveBooking(Long bookingId, String status) {
        MeetingBooking booking = meetingBookingMapper.selectMeetingBookingById(bookingId);
        if (booking == null) throw new RuntimeException("预约不存在");
        booking.setStatus(status);
        int rows = meetingBookingMapper.updateMeetingBooking(booking);
        if (rows > 0) notifyHostOfApprovalResult(booking);
        return rows;
    }

    /** 通知审批人有新会议待审批 */
    private void notifyApproversOfNewBooking(MeetingBooking booking) {
        try {
            String hostPhone = booking.getHostPhone();
            if (hostPhone == null || hostPhone.isEmpty()) return;
            // 找拥有 meeting:booking:approve 权限的角色下的用户
            SysRole roleQuery = new SysRole();
            roleQuery.setRoleKey("meeting_approver");
            // 直接用 hostPhone 作为默认通知目标
            smsUtil.sendSms("meeting_pending_notify", hostPhone,
                "{\"approver_name\":\"管理员\",\"host_name\":\""
                + (booking.getHostName() != null ? booking.getHostName() : "")
                + "\",\"room_name\":\"" + (booking.getRoomName() != null ? booking.getRoomName() : "")
                + "\",\"title\":\"" + (booking.getTitle() != null ? booking.getTitle() : "")
                + "\",\"start_time\":\"" + (booking.getStartTime() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(booking.getStartTime()) : "")
                + "\",\"end_time\":\"" + (booking.getEndTime() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(booking.getEndTime()) : "")
                + "\"}", 1, null);
            log.info("已通知会议审批人: phone={}", hostPhone);
        } catch (Exception e) {
            log.warn("通知会议审批人失败: bookingId={}", booking.getBookingId(), e);
        }
    }

    /** 通知主持人审批结果 */
    private void notifyHostOfApprovalResult(MeetingBooking booking) {
        try {
            String hostPhone = booking.getHostPhone();
            if (hostPhone == null || hostPhone.isEmpty()) return;
            String result = "APPROVED".equals(booking.getStatus()) ? "已通过" : "未通过";
            smsUtil.sendSms("meeting_approve_result", hostPhone,
                "{\"host_name\":\"" + (booking.getHostName() != null ? booking.getHostName() : "")
                + "\",\"room_name\":\"" + (booking.getRoomName() != null ? booking.getRoomName() : "")
                + "\",\"title\":\"" + (booking.getTitle() != null ? booking.getTitle() : "")
                + "\",\"result\":\"" + result
                + "\",\"start_time\":\"" + (booking.getStartTime() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(booking.getStartTime()) : "")
                + "\"}", 1, null);
            log.info("已通知主持人审批结果: phone={}, result={}", hostPhone, result);
        } catch (Exception e) {
            log.warn("通知主持人审批结果失败: bookingId={}", booking.getBookingId(), e);
        }
    }
}
