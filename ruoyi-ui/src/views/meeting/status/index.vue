<template>
  <div class="app-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color:#F56C6C">{{ statCount.inUse }}</div>
          <div class="stat-label">使用中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color:#E6A23C">{{ statCount.comingSoon }}</div>
          <div class="stat-label">即将开始</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color:#67C23A">{{ statCount.free }}</div>
          <div class="stat-label">空闲</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color:#909399">{{ statCount.disabled }}</div>
          <div class="stat-label">停用</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row class="mb8">
      <el-col :span="12">
        <span class="page-desc">各会议室当前使用状态，每 30 秒自动刷新</span>
      </el-col>
      <el-col :span="12" style="text-align:right">
        <el-button type="primary" plain icon="el-icon-refresh" size="mini" :loading="loading" @click="getStatus">手动刷新</el-button>
      </el-col>
    </el-row>
    <el-table v-loading="loading" :data="list" :header-cell-style="{ background: '#f8f9fb' }">
      <el-table-column label="会议室" prop="room.roomName" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="所属部门" prop="room.deptName" min-width="120" :show-overflow-tooltip="true"/>
      <el-table-column label="状态" min-width="100" align="center">
        <template slot-scope="{row}">
          <el-tag v-if="row.currentStatus === 'IN_USE'" type="danger" size="small">使用中</el-tag>
          <el-tag v-else-if="row.currentStatus === 'COMING_SOON'" type="warning" size="small">即将开始</el-tag>
          <el-tag v-else-if="row.room.status === '1'" type="info" size="small">停用</el-tag>
          <el-tag v-else type="success" size="small">空闲</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前会议" min-width="200">
        <template slot-scope="{row}">
          <div v-if="row.currentBooking">
            <div>{{ row.currentBooking.title }}</div>
            <div class="cell-sub">{{ formatTime(row.currentBooking.startTime) }} ~ {{ formatTime(row.currentBooking.endTime) }}</div>
          </div>
          <span v-else class="cell-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="下一场" min-width="200">
        <template slot-scope="{row}">
          <div v-if="row.nextBooking">
            <div>{{ row.nextBooking.title }}</div>
            <div class="cell-sub">{{ formatTime(row.nextBooking.startTime) }} ~ {{ formatTime(row.nextBooking.endTime) }}</div>
          </div>
          <span v-else class="cell-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="今日预约" prop="todayBookings.length" min-width="90" align="center"/>
      <el-table-column label="操作" min-width="110" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleViewBookings(row)">预约记录</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 今日预约记录弹窗 -->
    <el-dialog :title="bookingDialog.title" :visible.sync="bookingDialog.open" width="640px" append-to-body>
      <el-empty v-if="bookingDialog.list.length === 0" description="今日暂无预约记录"/>
      <el-timeline v-else>
        <el-timeline-item
          v-for="(b, idx) in bookingDialog.list"
          :key="idx"
          :timestamp="formatTime(b.startTime) + ' ~ ' + formatTime(b.endTime)"
          :type="bookingStatusType(b.status)">
          <div class="booking-title">{{ b.title }}</div>
          <div class="cell-sub">主持人：{{ b.hostName || '—' }}</div>
          <div class="cell-sub">状态：{{ bookingStatusText(b.status) }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>
<style scoped>
  .stat-card { text-align: center; }
  .stat-value { font-size: 32px; font-weight: 700; line-height: 1.2; }
  .stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
  .page-desc { font-size: 14px; color: #909399; }
  .cell-sub { font-size: 12px; color: #909399; }
  .cell-muted { color: #c0c4cc; }
  .booking-title { font-size: 14px; font-weight: 600; }
</style>
<script>
import { statusList } from '@/api/meeting/room'
export default {
  name: 'MeetingStatus',
  data() {
    return {
      loading: false,
      list: [],
      statCount: { inUse: 0, comingSoon: 0, free: 0, disabled: 0 },
      timer: null,
      bookingDialog: { open: false, title: '', list: [] }
    }
  },
  created() {
    this.getStatus()
    // 每 30 秒自动刷新当前状态
    this.timer = setInterval(() => { this.getStatus() }, 30000)
  },
  beforeDestroy() {
    if (this.timer) { clearInterval(this.timer); this.timer = null }
  },
  methods: {
    /** 拉取会议室状态总览 */
    getStatus() {
      this.loading = true
      statusList().then(response => {
        this.list = response.data || []
        this.computeStats()
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    /** 统计各状态数量 */
    computeStats() {
      const stat = { inUse: 0, comingSoon: 0, free: 0, disabled: 0 }
      this.list.forEach(row => {
        if (row.room.status === '1') { stat.disabled += 1 }
        else if (row.currentStatus === 'IN_USE') { stat.inUse += 1 }
        else if (row.currentStatus === 'COMING_SOON') { stat.comingSoon += 1 }
        else { stat.free += 1 }
      })
      this.statCount = stat
    },
    /** 查看某会议室今日预约记录 */
    handleViewBookings(row) {
      this.bookingDialog.title = '「' + row.room.roomName + '」今日预约记录'
      this.bookingDialog.list = row.todayBookings || []
      this.bookingDialog.open = true
    },
    /** 时间格式化（yyyy-MM-dd HH:mm:ss → HH:mm） */
    formatTime(time) {
      if (!time) return ''
      return String(time).replace('T', ' ').substring(11, 16)
    },
    /** 预约状态文案 */
    bookingStatusText(status) {
      const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }
      return map[status] || status
    },
    /** 预约状态对应时间轴颜色 */
    bookingStatusType(status) {
      const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
      return map[status] || 'primary'
    }
  }
}
</script>
