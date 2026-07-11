<template>
  <div class="board-page">
    <!-- Header -->
    <div class="board-header">
      <div class="room-name">{{ room.roomName }}</div>
      <div class="room-info">
        <span class="info-item">容纳 {{ room.capacity }} 人</span>
        <span class="info-divider">|</span>
        <span class="info-item">{{ room.location }}</span>
      </div>
      <div class="room-equipment" v-if="room.equipment">设备: {{ room.equipment }}</div>
    </div>

    <!-- Status Area -->
    <div class="status-area" :class="statusClass">
      <div class="status-icon">{{ statusIcon }}</div>
      <div class="status-label">{{ statusText }}</div>
      <div class="status-time" v-if="statusTimeDisplay">{{ statusTimeDisplay }}</div>
      <div class="current-booking" v-if="currentStatus === 'IN_USE' && currentBooking">
        <div class="booking-title">{{ currentBooking.title }}</div>
        <div class="booking-time">{{ formatShortTime(currentBooking.startTime) }} - {{ formatShortTime(currentBooking.endTime) }}</div>
        <div class="booking-host" v-if="currentBooking.hostName">{{ currentBooking.hostName }}</div>
      </div>
    </div>

    <!-- Today Schedule -->
    <div class="schedule-section">
      <div class="section-title">今日会议安排</div>
      <div class="schedule-list" v-if="todayBookings.length > 0">
        <div
          v-for="b in todayBookings"
          :key="b.bookingId"
          class="schedule-item"
          :class="scheduleItemClass(b)"
        >
          <div class="schedule-time">{{ formatShortTime(b.startTime) }}</div>
          <div class="schedule-body">
            <div class="schedule-title">{{ b.title }}</div>
            <div class="schedule-meta">
              <span>{{ formatShortTime(b.startTime) }} - {{ formatShortTime(b.endTime) }}</span>
              <span v-if="b.hostName">{{ b.hostName }}</span>
            </div>
          </div>
          <div class="schedule-tag" :class="scheduleTagClass(b)">{{ scheduleTagText(b) }}</div>
        </div>
      </div>
      <div class="schedule-empty" v-else>今日暂无会议安排</div>
    </div>

    <!-- Next Booking Countdown -->
    <div class="countdown-area" v-if="nextBooking && countdownText">
      <div class="countdown-label">下一场会议</div>
      <div class="countdown-time">{{ countdownText }}</div>
      <div class="countdown-booking">
        <div class="booking-title">{{ nextBooking.title }}</div>
        <div class="booking-time">{{ nextBooking.startTime }} - {{ nextBooking.endTime }}</div>
      </div>
    </div>

    <!-- Footer -->
    <div class="board-footer">
      <span>{{ currentTime }}</span>
      <span class="refresh-dot" :class="{ active: refreshing }"></span>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'MeetingBoard',
  data() {
    return {
      roomId: null,
      room: {},
      todayBookings: [],
      currentStatus: 'FREE',
      currentBooking: null,
      nextBooking: null,
      countdownMs: 0,
      currentTime: '',
      refreshing: false,
      countdownTimer: null,
      refreshTimer: null,
      clockTimer: null
    }
  },
  computed: {
    statusClass() {
      const m = { FREE: 'status-free', IN_USE: 'status-inuse', COMING_SOON: 'status-coming' }
      return m[this.currentStatus] || 'status-free'
    },
    statusIcon() {
      const m = { FREE: '', IN_USE: '', COMING_SOON: '' }
      return m[this.currentStatus] || ''
    },
    statusText() {
      const m = { FREE: '空闲', IN_USE: '使用中', COMING_SOON: '即将开始' }
      return m[this.currentStatus] || '空闲'
    },
    statusTimeDisplay() {
      if (this.currentStatus === 'IN_USE' && this.currentBooking) {
        return this.formatShortTime(this.currentBooking.startTime) + ' - ' + this.formatShortTime(this.currentBooking.endTime)
      }
      return ''
    },
    countdownText() {
      if (this.countdownMs <= 0 || !this.nextBooking) return ''
      const totalSeconds = Math.floor(this.countdownMs / 1000)
      const hours = Math.floor(totalSeconds / 3600)
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const seconds = totalSeconds % 60
      if (hours > 0) {
        return hours + '小时 ' + String(minutes).padStart(2, '0') + '分 ' + String(seconds).padStart(2, '0') + '秒'
      }
      return String(minutes).padStart(2, '0') + '分 ' + String(seconds).padStart(2, '0') + '秒'
    }
  },
  mounted() {
    this.roomId = this.$route.query.roomId
    if (!this.roomId) {
      this.room = { roomName: '未指定会议室', capacity: 0, equipment: '', location: '' }
      return
    }
    this.loadBoard()
    this.refreshTimer = setInterval(() => { this.loadRefresh() }, 30000)
    this.countdownTimer = setInterval(() => { this.tickCountdown() }, 1000)
    this.clockTimer = setInterval(() => { this.updateClock() }, 1000)
    this.updateClock()
  },
  beforeDestroy() {
    if (this.refreshTimer) clearInterval(this.refreshTimer)
    if (this.countdownTimer) clearInterval(this.countdownTimer)
    if (this.clockTimer) clearInterval(this.clockTimer)
  },
  methods: {
    loadBoard() {
      request({
        url: '/meeting/board/' + this.roomId,
        method: 'get',
        headers: { isToken: false }
      }).then(res => {
        if (res.code === 200) {
          const d = res.data
          this.room = d.room || {}
          this.todayBookings = d.todayBookings || []
          this.currentStatus = d.currentStatus || 'FREE'
          this.currentBooking = d.currentBooking || null
          this.nextBooking = d.nextBooking || null
          this.updateCountdown()
        }
      }).catch(() => {})
    },
    loadRefresh() {
      // 30秒刷新直接用 loadBoard 完整刷新，确保新预约能显示
      this.loadBoard()
    },
    tickCountdown() {
      if (!this.nextBooking) {
        this.countdownMs = 0
        return
      }
      this.countdownMs -= 1000
      if (this.countdownMs < 0) {
        this.countdownMs = 0
      }
    },
    updateCountdown() {
      if (!this.nextBooking) {
        this.countdownMs = 0
        return
      }
      const target = this.parseTime(this.nextBooking.startTime)
      if (!target) {
        this.countdownMs = 0
        return
      }
      this.countdownMs = target.getTime() - Date.now()
      if (this.countdownMs < 0) {
        this.countdownMs = 0
      }
    },
    updateClock() {
      const now = new Date()
      const y = now.getFullYear()
      const M = String(now.getMonth() + 1).padStart(2, '0')
      const d = String(now.getDate()).padStart(2, '0')
      const h = String(now.getHours()).padStart(2, '0')
      const m = String(now.getMinutes()).padStart(2, '0')
      const s = String(now.getSeconds()).padStart(2, '0')
      this.currentTime = y + '-' + M + '-' + d + ' ' + h + ':' + m + ':' + s
    },
    parseTime(str) {
      if (!str) return null
      const t = new Date(str.replace(/-/g, '/'))
      return isNaN(t.getTime()) ? null : t
    },
    formatShortTime(str) {
      if (!str) return ''
      const parts = str.split(' ')
      if (parts.length > 1) {
        const timePart = parts[1]
        const timeSegments = timePart.split(':')
        return timeSegments[0] + ':' + timeSegments[1]
      }
      return str
    },
    scheduleItemClass(booking) {
      if (!booking) return ''
      if (this.currentStatus === 'IN_USE' && this.currentBooking && this.currentBooking.bookingId === booking.bookingId) {
        return 'item-active'
      }
      const start = this.parseTime(booking.startTime)
      const end = this.parseTime(booking.endTime)
      const now = Date.now()
      if (start && end && start.getTime() <= now && end.getTime() > now) {
        return 'item-active'
      }
      if (start && start.getTime() > now) {
        return 'item-upcoming'
      }
      return 'item-past'
    },
    scheduleTagClass(booking) {
      if (!booking) return ''
      if (this.currentStatus === 'IN_USE' && this.currentBooking && this.currentBooking.bookingId === booking.bookingId) {
        return 'tag-active'
      }
      const start = this.parseTime(booking.startTime)
      const end = this.parseTime(booking.endTime)
      const now = Date.now()
      if (start && end && start.getTime() <= now && end.getTime() > now) {
        return 'tag-active'
      }
      if (start && start.getTime() > now) {
        return 'tag-upcoming'
      }
      return 'tag-past'
    },
    scheduleTagText(booking) {
      if (!booking) return ''
      if (this.currentStatus === 'IN_USE' && this.currentBooking && this.currentBooking.bookingId === booking.bookingId) {
        return '进行中'
      }
      const start = this.parseTime(booking.startTime)
      const end = this.parseTime(booking.endTime)
      const now = Date.now()
      if (start && end && start.getTime() <= now && end.getTime() > now) {
        return '进行中'
      }
      if (start && start.getTime() > now) {
        return '待开始'
      }
      return '已结束'
    }
  }
}
</script>

<style scoped>
* { box-sizing: border-box; margin: 0; padding: 0; }

.board-page {
  min-height: 100vh;
  background: #1a1a2e;
  color: #e0e0e0;
  font-family: -apple-system, "Microsoft YaHei", "PingFang SC", sans-serif;
  display: flex;
  flex-direction: column;
  padding: 24px 32px;
}

/* Header */
.board-header {
  text-align: center;
  padding: 16px 0 24px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.room-name {
  font-size: 42px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.room-info {
  font-size: 18px;
  color: #a0a0b8;
}
.info-divider {
  margin: 0 12px;
  color: #3a3a5c;
}
.room-equipment {
  font-size: 15px;
  color: #707088;
  margin-top: 6px;
}

/* Status Area */
.status-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  margin: 20px 0;
  border-radius: 16px;
  transition: background 0.5s ease;
}
.status-free {
  background: linear-gradient(135deg, #16213e, #1a3a1a);
  border: 2px solid rgba(76, 175, 80, 0.3);
}
.status-inuse {
  background: linear-gradient(135deg, #2d1f1f, #4a2020);
  border: 2px solid rgba(244, 67, 54, 0.4);
}
.status-coming {
  background: linear-gradient(135deg, #1e2a38, #3d3520);
  border: 2px solid rgba(255, 152, 0, 0.4);
}

.status-icon {
  font-size: 64px;
  margin-bottom: 8px;
}
.status-label {
  font-size: 48px;
  font-weight: 700;
  letter-spacing: 6px;
}
.status-free .status-label { color: #4caf50; }
.status-inuse .status-label { color: #f44336; }
.status-coming .status-label { color: #ff9800; }

.status-time {
  font-size: 20px;
  color: #a0a0b8;
  margin-top: 8px;
}

.current-booking {
  text-align: center;
  margin-top: 16px;
  padding: 12px 40px;
  background: rgba(255,255,255,0.06);
  border-radius: 8px;
}
.current-booking .booking-title {
  font-size: 22px;
  color: #ffffff;
  font-weight: 600;
}
.current-booking .booking-time {
  font-size: 17px;
  color: #9090a8;
  margin-top: 4px;
}
.current-booking .booking-host {
  font-size: 15px;
  color: #707088;
  margin-top: 2px;
}

/* Schedule Section */
.schedule-section {
  margin-bottom: 20px;
}
.section-title {
  font-size: 20px;
  color: #9090a8;
  margin-bottom: 12px;
  padding-left: 4px;
  letter-spacing: 1px;
}

.schedule-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 6px;
  border-radius: 8px;
  background: rgba(255,255,255,0.04);
  border-left: 3px solid transparent;
  transition: background 0.3s;
}
.schedule-item.item-active {
  background: rgba(244, 67, 54, 0.12);
  border-left-color: #f44336;
}
.schedule-item.item-upcoming {
  background: rgba(255, 152, 0, 0.08);
  border-left-color: #ff9800;
}
.schedule-item.item-past {
  opacity: 0.45;
}

.schedule-time {
  font-size: 22px;
  font-weight: 700;
  color: #c0c0d0;
  min-width: 60px;
  text-align: center;
  margin-right: 16px;
}

.schedule-body {
  flex: 1;
  min-width: 0;
}
.schedule-title {
  font-size: 17px;
  color: #e0e0e0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-meta {
  font-size: 13px;
  color: #707088;
  margin-top: 3px;
}
.schedule-meta span {
  margin-right: 12px;
}

.schedule-tag {
  font-size: 13px;
  padding: 3px 10px;
  border-radius: 12px;
  white-space: nowrap;
  margin-left: 12px;
}
.schedule-tag.tag-active {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
}
.schedule-tag.tag-upcoming {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
}
.schedule-tag.tag-past {
  background: rgba(255,255,255,0.05);
  color: #555;
}

.schedule-empty {
  text-align: center;
  font-size: 16px;
  color: #555;
  padding: 32px 0;
}

/* Countdown Area */
.countdown-area {
  text-align: center;
  padding: 20px;
  background: rgba(255, 152, 0, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(255, 152, 0, 0.2);
  margin-bottom: 20px;
}
.countdown-label {
  font-size: 15px;
  color: #9090a8;
  margin-bottom: 4px;
}
.countdown-time {
  font-size: 36px;
  font-weight: 700;
  color: #ff9800;
  letter-spacing: 2px;
}
.countdown-booking {
  margin-top: 8px;
}
.countdown-booking .booking-title {
  font-size: 17px;
  color: #e0e0e0;
}
.countdown-booking .booking-time {
  font-size: 14px;
  color: #808098;
  margin-top: 2px;
}

/* Footer */
.board-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #555;
  padding: 12px 0 8px;
  gap: 10px;
}
.refresh-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #555;
  transition: background 0.3s;
}
.refresh-dot.active {
  background: #4caf50;
}

/* Responsive - Tablet landscape 1024x768 */
@media (max-width: 1024px) {
  .board-page { padding: 16px 20px; }
  .room-name { font-size: 32px; }
  .status-icon { font-size: 48px; }
  .status-label { font-size: 36px; }
  .countdown-time { font-size: 28px; }
  .board-footer { font-size: 18px; }
}
</style>
