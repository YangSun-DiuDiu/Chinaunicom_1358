<template>
  <div class="cockpit-container">
    <!-- ========== TOP: 4 KPI 卡片 ========== -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:rgba(0,212,255,0.15)">
            <svg-icon icon-class="monitor" style="color:#00d4ff;font-size:28px" />
          </div>
          <div class="kpi-body">
            <count-to :start-val="0" :end-val="deviceData.totalCount" :duration="1500" class="kpi-value" />
            <div class="kpi-label">IoT设备</div>
            <div class="kpi-sub">
              <span class="sub-online">在线 {{ deviceData.onlineCount }}</span>
              <span class="sub-offline" v-if="deviceData.offlineCount>0">离线 {{ deviceData.offlineCount }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:rgba(0,230,118,0.15)">
            <svg-icon icon-class="peoples" style="color:#00e676;font-size:28px" />
          </div>
          <div class="kpi-body">
            <count-to :start-val="0" :end-val="visitorData.todayVisitors" :duration="1500" class="kpi-value" />
            <div class="kpi-label">今日访客</div>
            <div class="kpi-sub">
              <span class="sub-warn" v-if="visitorData.pendingApprovals>0">待审批 {{ visitorData.pendingApprovals }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:rgba(255,145,0,0.15)">
            <svg-icon icon-class="date" style="color:#ff9100;font-size:28px" />
          </div>
          <div class="kpi-body">
            <count-to :start-val="0" :end-val="meetingData.totalBookings" :duration="1500" class="kpi-value" />
            <div class="kpi-label">会议预约</div>
            <div class="kpi-sub">
              <span>今日 {{ meetingData.todayBookings }} 场</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="kpi-card">
          <div class="kpi-icon" style="background:rgba(255,82,82,0.15)">
            <svg-icon icon-class="home" style="color:#ff5252;font-size:28px" />
          </div>
          <div class="kpi-body">
            <count-to :start-val="0" :end-val="apartmentData.totalRooms" :duration="1500" class="kpi-value" />
            <div class="kpi-label">公寓房源</div>
            <div class="kpi-sub">
              <span class="sub-online">空置 {{ apartmentData.vacantCount }}</span>
              <span class="sub-warn">入住 {{ apartmentData.occupiedCount }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ========== MIDDLE: 2x2 图表网格 ========== -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">设备类型分布</span>
          </div>
          <div ref="devicePieChart" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">访客状态分布</span>
          </div>
          <div ref="visitorBarChart" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">公寓状态分布</span>
          </div>
          <div ref="apartmentPieChart" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">今日会议</span>
          </div>
          <div class="table-box">
            <el-table :data="meetingData.todayMeetings" size="mini" :show-header="true" max-height="260">
              <el-table-column label="会议名称" prop="title" :show-overflow-tooltip="true" />
              <el-table-column label="时间" width="140">
                <template slot-scope="scope">
                  <span>{{ parseTime(scope.row.startTime, '{h}:{i}') }} - {{ parseTime(scope.row.endTime, '{h}:{i}') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.status==='APPROVED'?'success':'warning'" size="mini">
                    {{ scope.row.status==='APPROVED'?'已审批':'待审批' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!meetingData.todayMeetings || meetingData.todayMeetings.length===0" class="empty-tip">暂无今日会议</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ========== BOTTOM: 动态列表 2列 ========== -->
    <el-row :gutter="16" class="list-row">
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">最新来访</span>
          </div>
          <div class="table-box">
            <el-table :data="visitorData.latestVisits" size="mini" max-height="240">
              <el-table-column label="访客" prop="visitorName" width="90" />
              <el-table-column label="被访人" prop="hostName" width="90" />
              <el-table-column label="事由" prop="visitReason" :show-overflow-tooltip="true" />
              <el-table-column label="时间" width="140">
                <template slot-scope="scope">{{ parseTime(scope.row.visitTime || scope.row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">最近事件</span>
          </div>
          <div class="table-box">
            <el-table :data="deviceData.recentEvents" size="mini" max-height="240">
              <el-table-column label="设备" prop="deviceName" width="100" :show-overflow-tooltip="true" />
              <el-table-column label="事件" prop="eventType" width="70">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.eventType==='离线'?'danger':'success'" size="mini">{{ scope.row.eventType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="详情" prop="eventDetail" :show-overflow-tooltip="true" />
              <el-table-column label="时间" width="140">
                <template slot-scope="scope">{{ parseTime(scope.row.eventTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import CountTo from 'vue-count-to'
import { getDeviceDashboard, getVisitorDashboard, getMeetingDashboard, getApartmentDashboard } from '@/api/dashboard/dashboard'

export default {
  name: 'CockpitIndex',
  components: { CountTo },
  data() {
    return {
      deviceData: { totalCount: 0, onlineCount: 0, offlineCount: 0, onlineRate: 0, deviceTypeDistribution: [], recentAlerts: [], recentEvents: [] },
      visitorData: { todayVisitors: 0, pendingApprovals: 0, totalAppointments: 0, latestVisits: [], visitorStatusDistribution: [] },
      meetingData: { totalBookings: 0, todayBookings: 0, todayMeetings: [], roomCount: 0 },
      apartmentData: { totalRooms: 0, vacantCount: 0, occupiedCount: 0, statusDistribution: [] },
      charts: {}
    }
  },
  mounted() {
    this.loadAll()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(c => c.dispose())
  },
  methods: {
    loadAll() {
      this.loadDevice()
      this.loadVisitor()
      this.loadMeeting()
      this.loadApartment()
    },
    loadDevice() {
      getDeviceDashboard().then(res => {
        const d = res.data || res
        this.deviceData.totalCount = d.totalCount || 0
        this.deviceData.onlineCount = d.onlineCount || 0
        this.deviceData.offlineCount = d.offlineCount || 0
        this.deviceData.onlineRate = d.onlineRate || 0
        this.deviceData.deviceTypeDistribution = (d.deviceTypeStats || []).map(t => ({ name: this.deviceTypeName(t.type), value: t.count }))
        this.deviceData.recentEvents = this.buildEvents(d.recentAlerts || [], d.recentHeartbeats || [])
        this.$nextTick(() => this.initDevicePie())
      }).catch(() => {})
    },
    loadVisitor() {
      getVisitorDashboard().then(res => {
        const d = res.data || res
        this.visitorData.todayVisitors = d.todayVisitors || 0
        this.visitorData.pendingApprovals = d.pendingApprovals || 0
        this.visitorData.totalAppointments = d.totalAppointments || 0
        this.visitorData.latestVisits = (d.recentVisits || []).slice(0, 5)
        this.visitorData.visitorStatusDistribution = (d.visitStats || []).map(s => ({ name: this.visitorStatusName(s.status), value: s.count }))
        this.$nextTick(() => this.initVisitorBar())
      }).catch(() => {})
    },
    loadMeeting() {
      getMeetingDashboard().then(res => {
        const d = res.data || res
        this.meetingData.totalBookings = d.totalBookings || 0
        this.meetingData.todayBookings = d.todayBookings || 0
        this.meetingData.todayMeetings = d.todayMeetings || []
        this.meetingData.roomCount = d.roomCount || 0
      }).catch(() => {})
    },
    loadApartment() {
      getApartmentDashboard().then(res => {
        const d = res.data || res
        this.apartmentData.totalRooms = d.totalRooms || 0
        this.apartmentData.statusDistribution = d.statusDistribution || []
        const dist = d.statusDistribution || []
        this.apartmentData.vacantCount = dist.find(s => s.status === 'GREEN')?.count || 0
        const cyan = dist.find(s => s.status === 'CYAN')?.count || 0
        const blue = dist.find(s => s.status === 'BLUE')?.count || 0
        this.apartmentData.occupiedCount = cyan + blue
        this.$nextTick(() => this.initApartmentPie())
      }).catch(() => {})
    },

    // ===== 图表 =====
    initDevicePie() {
      if (!this.$refs.devicePieChart) return
      if (this.charts.devicePie) this.charts.devicePie.dispose()
      this.charts.devicePie = echarts.init(this.$refs.devicePieChart, 'dark')
      const data = this.deviceData.deviceTypeDistribution
      this.charts.devicePie.setOption({
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie', radius: ['45%', '70%'], center: ['50%', '55%'],
          data: data.length > 0 ? data : [{ value: 0, name: '暂无' }],
          emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } },
          label: { color: '#8899aa', formatter: '{b}: {c}' }
        }]
      })
    },
    initVisitorBar() {
      if (!this.$refs.visitorBarChart) return
      if (this.charts.visitorBar) this.charts.visitorBar.dispose()
      this.charts.visitorBar = echarts.init(this.$refs.visitorBarChart, 'dark')
      const d = this.visitorData.visitorStatusDistribution
      this.charts.visitorBar.setOption({
        tooltip: { trigger: 'axis' },
        grid: { top: 10, left: '3%', right: '4%', bottom: 0, containLabel: true },
        xAxis: { type: 'category', data: d.map(i => i.name), axisLabel: { color: '#8899aa' } },
        yAxis: { type: 'value', axisLabel: { color: '#8899aa' } },
        series: [{ type: 'bar', barWidth: '45%', data: d.map(i => i.value),
          itemStyle: { color: '#00d4ff', borderRadius: [4, 4, 0, 0] } }]
      })
    },
    initApartmentPie() {
      if (!this.$refs.apartmentPieChart) return
      if (this.charts.apartmentPie) this.charts.apartmentPie.dispose()
      this.charts.apartmentPie = echarts.init(this.$refs.apartmentPieChart, 'dark')
      const colors = { GREEN: '#00e676', CYAN: '#00d4ff', BLUE: '#409EFF', GRAY: '#8899aa' }
      const data = this.apartmentData.statusDistribution.map(s => ({
        name: s.label, value: s.count, itemStyle: { color: colors[s.status] || '#8899aa' }
      }))
      this.charts.apartmentPie.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}间' },
        series: [{
          type: 'pie', radius: ['45%', '70%'], center: ['50%', '55%'],
          data: data.length > 0 ? data : [{ value: 0, name: '暂无', itemStyle: { color: '#8899aa' } }],
          label: { color: '#8899aa', formatter: '{b}: {c}' }
        }]
      })
    },

    // ===== 辅助 =====
    buildEvents(alerts, heartbeats) {
      const events = []
      ;(alerts || []).slice(0, 5).forEach(a => {
        events.push({ deviceName: a.deviceName, eventType: a.changeType === 'ONLINE' ? '恢复' : '离线',
          eventDetail: (a.oldStatus||'') + ' → ' + (a.newStatus||''), eventTime: a.changeTime || a.createTime })
      })
      return events.slice(0, 5)
    },
    deviceTypeName(t) {
      const m = { NETWORK: '网络设备', MONITOR: '监控设备', NVR: '硬盘录像机', OTHER: '其他' }
      return m[t] || t || '未知'
    },
    visitorStatusName(s) {
      const m = { PENDING: '待审批', APPROVED: '已通过', VISITING: '访客中', COMPLETED: '已完成', REJECTED: '已拒绝', CANCELLED: '已取消' }
      return m[s] || s || '未知'
    },
    handleResize() {
      Object.values(this.charts).forEach(c => c.resize && c.resize())
    }
  }
}
</script>

<style scoped>
.cockpit-container {
  min-height: 100vh;
  background: #0a1a2e;
  padding: 20px;
  margin: -20px;
}

/* ===== KPI 卡片 ===== */
.kpi-row { margin-bottom: 16px; }
.kpi-card {
  display: flex; align-items: center; height: 100px;
  background: rgba(13,33,55,0.85); border: 1px solid rgba(0,180,255,0.12);
  border-radius: 8px; padding: 16px; margin-bottom: 16px;
  transition: all 0.3s;
}
.kpi-card:hover { border-color: rgba(0,212,255,0.4); transform: translateY(-2px); }
.kpi-icon {
  width: 52px; height: 52px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  margin-right: 16px; flex-shrink: 0;
}
.kpi-body { flex: 1; min-width: 0; }
.kpi-value { font-size: 28px; font-weight: 700; color: #00d4ff; line-height: 1.1;
  text-shadow: 0 0 20px rgba(0,212,255,0.3); }
.kpi-label { font-size: 13px; color: #8899aa; margin-top: 4px; }
.kpi-sub { font-size: 12px; margin-top: 2px; }
.sub-online { color: #00e676; margin-right: 8px; }
.sub-offline { color: #ff5252; }
.sub-warn { color: #ff9100; }

/* ===== 图表面板 ===== */
.chart-row { margin-bottom: 16px; }
.panel-card {
  background: rgba(13,33,55,0.85); border: 1px solid rgba(0,180,255,0.12);
  border-radius: 8px; margin-bottom: 16px; overflow: hidden;
}
.panel-header { padding: 14px 20px; border-bottom: 1px solid rgba(0,180,255,0.08); }
.panel-title { font-size: 15px; font-weight: 600; color: #bcc9d4; }
.chart-box { width: 100%; height: 280px; }
.table-box { padding: 0; }
.empty-tip { text-align: center; padding: 40px 0; color: #556677; font-size: 13px; }
.list-row { margin-bottom: 0; }

/* ===== 表格暗色覆盖 ===== */
.cockpit-container >>> .el-table {
  background: transparent; color: #bcc9d4;
}
.cockpit-container >>> .el-table th.el-table__cell {
  background: rgba(0,180,255,0.06); color: #8899aa; border-bottom: 1px solid rgba(0,180,255,0.08);
  font-weight: 500; font-size: 12px;
}
.cockpit-container >>> .el-table tr { background: transparent !important; }
.cockpit-container >>> .el-table td.el-table__cell {
  border-bottom: 1px solid rgba(0,180,255,0.05); color: #bcc9d4; font-size: 13px;
}
.cockpit-container >>> .el-table--enable-row-hover .el-table__body tr:hover > td {
  background: rgba(0,212,255,0.06) !important;
}
.cockpit-container >>> .el-table__empty-text { color: #556677; }

@media (max-width: 768px) {
  .cockpit-container { padding: 10px; margin: -10px; }
  .kpi-card { height: 80px; padding: 12px; }
  .kpi-icon { width: 40px; height: 40px; }
  .kpi-value { font-size: 22px; }
  .chart-box { height: 220px; }
}
</style>
