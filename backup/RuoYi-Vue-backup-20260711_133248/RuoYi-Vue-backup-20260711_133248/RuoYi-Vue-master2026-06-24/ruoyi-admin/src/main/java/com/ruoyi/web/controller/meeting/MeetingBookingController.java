package com.ruoyi.web.controller.meeting;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.domain.MeetingRoom;
import com.ruoyi.system.service.IMeetingBookingService;
import com.ruoyi.system.service.IMeetingRoomService;

/**
 * 会议预约 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/meeting/booking")
public class MeetingBookingController extends BaseController
{
    @Autowired
    private IMeetingBookingService meetingBookingService;

    @Autowired
    private IMeetingRoomService meetingRoomService;

    /**
     * 获取会议预约分页列表
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:list')")
    @GetMapping("/list")
    public TableDataInfo list(MeetingBooking meetingBooking)
    {
        startPage();
        List<MeetingBooking> list = meetingBookingService.selectMeetingBookingList(meetingBooking);
        return getDataTable(list);
    }

    /**
     * 根据预约ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:query')")
    @GetMapping(value = "/{bookingId}")
    public AjaxResult getInfo(@PathVariable Long bookingId)
    {
        return success(meetingBookingService.selectMeetingBookingById(bookingId));
    }

    /**
     * 新增会议预约（含冲突检测）
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:add')")
    @Log(title = "会议预约管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MeetingBooking meetingBooking)
    {
        // 校验会议室是否存在
        MeetingRoom room = meetingRoomService.selectMeetingRoomById(meetingBooking.getRoomId());
        if (room == null)
        {
            return error("会议室不存在");
        }
        // 冲突检测
        if (meetingBookingService.hasConflict(meetingBooking.getRoomId(),
                meetingBooking.getStartTime(), meetingBooking.getEndTime(), null))
        {
            return error("该时间段会议室已被预约，请更换时间");
        }
        // 设置会议室名称
        meetingBooking.setRoomName(room.getRoomName());
        meetingBooking.setCreateBy(SecurityUtils.getUsername());
        return toAjax(meetingBookingService.insertMeetingBooking(meetingBooking));
    }

    /**
     * 修改会议预约
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:edit')")
    @Log(title = "会议预约管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MeetingBooking meetingBooking)
    {
        // 冲突检测（排除当前预约）
        if (meetingBookingService.hasConflict(meetingBooking.getRoomId(),
                meetingBooking.getStartTime(), meetingBooking.getEndTime(), meetingBooking.getBookingId()))
        {
            return error("该时间段会议室已被预约，请更换时间");
        }
        meetingBooking.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(meetingBookingService.updateMeetingBooking(meetingBooking));
    }

    /**
     * 删除会议预约
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:remove')")
    @Log(title = "会议预约管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{bookingIds}")
    public AjaxResult remove(@PathVariable Long[] bookingIds)
    {
        meetingBookingService.deleteMeetingBookingByIds(bookingIds);
        return success();
    }

    /**
     * 审批通过
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:approve')")
    @Log(title = "会议预约管理", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{bookingId}")
    public AjaxResult approve(@PathVariable Long bookingId)
    {
        MeetingBooking booking = meetingBookingService.selectMeetingBookingById(bookingId);
        if (booking == null)
        {
            return error("预约记录不存在");
        }
        if (!"PENDING".equals(booking.getStatus()))
        {
            return error("仅待审批状态的预约可以审批");
        }
        booking.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(meetingBookingService.approveBooking(bookingId, "APPROVED"));
    }

    /**
     * 审批拒绝
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:reject')")
    @Log(title = "会议预约管理", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{bookingId}")
    public AjaxResult reject(@PathVariable Long bookingId)
    {
        MeetingBooking booking = meetingBookingService.selectMeetingBookingById(bookingId);
        if (booking == null)
        {
            return error("预约记录不存在");
        }
        if (!"PENDING".equals(booking.getStatus()))
        {
            return error("仅待审批状态的预约可以拒绝");
        }
        booking.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(meetingBookingService.approveBooking(bookingId, "REJECTED"));
    }

    /**
     * 取消预约
     */
    @PreAuthorize("@ss.hasPermi('meeting:booking:cancel')")
    @Log(title = "会议预约管理", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{bookingId}")
    public AjaxResult cancel(@PathVariable Long bookingId)
    {
        MeetingBooking booking = meetingBookingService.selectMeetingBookingById(bookingId);
        if (booking == null)
        {
            return error("预约记录不存在");
        }
        if ("CANCELLED".equals(booking.getStatus()))
        {
            return error("预约已取消，无需重复操作");
        }
        booking.setStatus("CANCELLED");
        booking.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(meetingBookingService.updateMeetingBooking(booking));
    }

    /**
     * 获取指定会议室指定日期的已占用时间段
     */
    @GetMapping("/slots/{roomId}")
    public AjaxResult slots(@PathVariable Long roomId, @RequestParam String date)
    {
        List<MeetingBooking> list = meetingBookingService.selectSlots(roomId, date);
        return success(list);
    }
}
