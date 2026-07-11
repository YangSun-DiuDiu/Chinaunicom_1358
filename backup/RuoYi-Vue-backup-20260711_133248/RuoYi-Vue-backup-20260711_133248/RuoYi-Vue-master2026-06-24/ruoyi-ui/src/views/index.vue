<template>
  <div class="cockpit">
    <!-- ====== 背景层 ====== -->
    <div class="cockpit-bg"></div>

    <!-- ====== KPI 指标条 (1行8卡片) ====== -->
    <div class="kpi-strip">
      <!-- IoT设备 -->
      <div class="kpi-card">
        <div class="kpi-glow kpi-glow--cyan"></div>
        <span class="kpi-value"><count-to :start-val="0" :end-val="deviceData.totalCount" :duration="1200" /></span>
        <span class="kpi-label">IoT 设备</span>
        <span class="kpi-tag kpi-tag--cyan">在线 {{ deviceData.onlineCount }}</span>
        <span v-if="deviceData.offlineCount > 0" class="kpi-tag kpi-tag--red">离线 {{ deviceData.offlineCount }}</span>
      </div>
      <!-- 在线率 -->
      <div class="kpi-card">
        <div class="kpi-glow kpi-glow--green"></div>
        <span class="kpi-value"><count-to :start-val="0" :end-val="deviceData.onlineRate" :duration="1500" :decimals="1" />%</span>
        <span class="kpi-label">在线率</span>
        <span class="kpi-tag kpi-tag--green">{{ deviceData.onlineCount }} 台在线</span>
      </div>
      <!-- 今日访客 -->
      <div class="kpi-card">
        <div class="kpi-glow kpi-glow--cyan"></div>
        <span class="kpi-value kpi-value--green"><count-to :start-val="0" :end-val="visitorData.todayVisitors" :duration="1200" /></span>
        <span class="kpi-label">今日访客</span>
        <span class="kpi-tag kpi-tag--dim">累计 {{ visitorData.totalAppointments }}</span>
      </div>
      <!-- 待审批 -->
      <div class="kpi-card">
        <div class="kpi-glow" :class="visitorData.pendingApprovals > 0 ? 'kpi-glow--red' : 'kpi-glow--green'"></div>
        <span class="kpi-value" :class="visitorData.pendingApprovals > 0 ? 'kpi-value--amber' : 'kpi-value--green'">
          <count-to :start-val="0" :end-val="visitorData.pendingApprovals" :duration="1000" />
        </span>
        <span class="kpi-label">待审批</span>
        <span class="kpi-tag" :class="visitorData.pendingApprovals > 0 ? 'kpi-tag--amber' : 'kpi-tag--dim'">
          {{ visitorData.pendingApprovals > 0 ? '需处理' : '已清空' }}
        </span>
      </div>
      <!-- 会议预约 -->
      <div class="kpi-card">
        <div class="kpi-glow kpi-glow--amber"></div>
        <span class="kpi-value kpi-value--amber"><count-to :start-val="0" :end-val="meetingData.totalBookings" :duration="1200" /></span>
        <span class="kpi-label">会议预约</span>
        <span class="kpi-tag kpi-tag--dim">{{ meetingData.roomCount }} 间可用</span>
      </div>
      <!-- 今日会议 -->
      <div class="kpi-card">
        <div class="kpi-glow kpi-glow--amber"></div>
        <span class="kpi-value kpi-value--amber"><count-to :start-val="0" :end-val="meetingData.todayBookings" :duration="1000" /></span>
        <span class="kpi-label">今日会议</span>
        <span class="kpi-tag kpi-tag--dim">当日安排</span>
      </div>
      <!-- 公寓房源 -->
      <div class="kpi-card">
        <div class="kpi-glow kpi-glow--cyan"></div>
        <span class="kpi-value"><count-to :start-val="0" :end-val="apartmentData.totalRooms" :duration="1200" /></span>
        <span class="kpi-label">公寓房源</span>
        <span class="kpi-sub-row">
          <span class="kpi-tag kpi-tag--green">空置 {{ apartmentData.vacantCount }}</span>
          <span class="kpi-tag kpi-tag--cyan">入住 {{ apartmentData.occupiedCount }}</span>
        </span>
      </div>
      <!-- 入住率 -->
      <div class="kpi-card">
        <div class="kpi-glow kpi-glow--green"></div>
        <span class="kpi-value kpi-value--green"><count-to :start-val="0" :end-val="apartmentData.occupancyRate" :duration="1500" :decimals="1" />%</span>
        <span class="kpi-label">入住率</span>
        <span class="kpi-tag kpi-tag--dim">满房 {{ apartmentData.fullCount }} 间</span>
      </div>
    </div>

    <!-- ====== 图表区 (3列) ====== -->
    <div class="chart-row">
      <div class="panel">
        <div class="panel-head">
          <i class="panel-dot panel-dot--cyan"></i>
          <span class="panel-title">设备类型分布</span>
        </div>
        <div class="panel-body">
          <div ref="devicePieChart" class="chart-box"></div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-head">
          <i class="panel-dot panel-dot--amber"></i>
          <span class="panel-title">访客状态分布</span>
        </div>
        <div class="panel-body">
          <div ref="visitorBarChart" class="chart-box"></div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-head">
          <i class="panel-dot panel-dot--green"></i>
          <span class="panel-title">公寓状态分布</span>
        </div>
        <div class="panel-body">
          <div ref="apartmentPieChart" class="chart-box"></div>
        </div>
      </div>
    </div>

    <!-- ====== 表格区 (3列) ====== -->
    <div class="table-row">
      <div class="panel">
        <div class="panel-head panel-head--compact">
          <i class="panel-dot panel-dot--amber"></i>
          <span class="panel-title">今日会议</span>
          <span class="panel-badge">{{ meetingData.todayMeetings ? meetingData.todayMeetings.length : 0 }}</span>
        </div>
        <div class="panel-body panel-body--table">
          <el-table :data="meetingData.todayMeetings" size="mini" max-height="100%" class="dt">
            <el-table-column label="会议" prop="title" :show-overflow-tooltip="true" />
            <el-table-column label="时间" width="120">
              <template slot-scope="scope">{{ parseTime(scope.row.startTime, '{h}:{i}') }}-{{ parseTime(scope.row.endTime, '{h}:{i}') }}</template>
            </el-table-column>
            <el-table-column label="状态" width="64">
              <template slot-scope="scope">
                <span class="tb-status" :class="scope.row.status === 'APPROVED' ? 'tb-status--ok' : 'tb-status--warn'">
                  {{ scope.row.status === 'APPROVED' ? '已批' : '待批' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!meetingData.todayMeetings || meetingData.todayMeetings.length === 0" class="panel-empty">— 暂无今日会议 —</div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-head panel-head--compact">
          <i class="panel-dot panel-dot--cyan"></i>
          <span class="panel-title">最新来访</span>
        </div>
        <div class="panel-body panel-body--table">
          <el-table :data="visitorData.latestVisits" size="mini" max-height="100%" class="dt">
            <el-table-column label="访客" prop="visitorName" width="72" :show-overflow-tooltip="true" />
            <el-table-column label="被访" prop="hostName" width="72" :show-overflow-tooltip="true" />
            <el-table-column label="事由" prop="visitReason" :show-overflow-tooltip="true" />
            <el-table-column label="时间" width="130">
              <template slot-scope="scope">{{ parseTime(scope.row.visitTime || scope.row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <div v-if="!visitorData.latestVisits || visitorData.latestVisits.length === 0" class="panel-empty">— 暂无来访记录 —</div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-head panel-head--compact">
          <i class="panel-dot panel-dot--red"></i>
          <span class="panel-title">最近告警</span>
          <span v-if="deviceData.recentEvents && deviceData.recentEvents.length" class="panel-badge panel-badge--red">{{ deviceData.recentEvents.length }}</span>
        </div>
        <div class="panel-body panel-body--table">
          <el-table :data="deviceData.recentEvents" size="mini" max-height="100%" class="dt">
            <el-table-column label="设备" prop="deviceName" width="80" :show-overflow-tooltip="true" />
            <el-table-column label="事件" width="52">
              <template slot-scope="scope">
                <span class="tb-status" :class="scope.row.eventType === '离线' ? 'tb-status--err' : 'tb-status--ok'">
                  {{ scope.row.eventType }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="详情" prop="eventDetail" :show-overflow-tooltip="true" />
            <el-table-column label="时间" width="130">
              <template slot-scope="scope">{{ parseTime(scope.row.eventTime) }}</template>
            </el-table-column>
          </el-table>
          <div v-if="!deviceData.recentEvents || deviceData.recentEvents.length === 0" class="panel-empty">— 无告警 —</div>
        </div>
      </div>
    </div>

    <!-- ====== 状态栏 ====== -->
    <div class="status-bar">
      <span class="status-led status-led--green"></span>
      <span class="status-label">SYS NORMAL</span>
      <span class="status-sep">│</span>
      <span class="status-led status-led--green"></span>
      <span class="status-label">NET OK</span>
      <span class="status-sep">│</span>
      <span class="status-led status-led--green"></span>
      <span class="status-label">API OK</span>
      <span class="status-clock">{{ clock }}</span>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import CountTo from 'vue-count-to'
import { getDeviceDashboard, getVisitorDashboard, getMeetingDashboard, getApartmentDashboard } from '@/api/dashboard/dashboard'

/* ====== 工业控制室配色 ====== */
const C = {
  amber:    '#f0a000',
  cyan:     '#00a8cc',
  green:    '#0ecb7e',
  red:      '#e8453c',
  text:     '#8a9bb0',
  textDim:  '#556678',
  grid:     'rgba(138,155,176,0.10)',
  pieColors: ['#f0a000','#00a8cc','#0ecb7e','#e8453c','#8a9bb0','#556678']
}

/* ====== ECharts 共用基准 ====== */
function baseOpts() {
  return {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(20,28,38,0.95)',
      borderColor: 'rgba(240,160,0,0.25)',
      textStyle: { color: '#c8d6e0', fontSize: 12, fontFamily: 'Rajdhani, sans-serif' }
    }
  }
}

export default {
  name: 'CockpitIndex',
  components: { CountTo },
  data() {
    return {
      deviceData:    { totalCount: 0, onlineCount: 0, offlineCount: 0, onlineRate: 0, deviceTypeDistribution: [], recentEvents: [] },
      visitorData:   { todayVisitors: 0, pendingApprovals: 0, totalAppointments: 0, latestVisits: [], visitorStatusDistribution: [] },
      meetingData:   { totalBookings: 0, todayBookings: 0, todayMeetings: [], roomCount: 0 },
      apartmentData: { totalRooms: 0, vacantCount: 0, occupiedCount: 0, fullCount: 0, occupancyRate: 0, statusDistribution: [] },
      charts: {},
      clock: '',
      clockTimer: null,
      fontsReady: false
    }
  },
  mounted() {
    this.loadFonts()
    this.loadAll()
    this.updateClock()
    this.clockTimer = setInterval(this.updateClock, 1000)
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.clockTimer) clearInterval(this.clockTimer)
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(c => { try { c.dispose() } catch (e) { /* noop */ } })
  },
  methods: {
    /* ----- 字体加载 ----- */
    loadFonts() {
      if (document.getElementById('cockpit-fonts')) return
      const link = document.createElement('link')
      link.id = 'cockpit-fonts'
      link.href = 'https://fonts.googleapis.com/css2?family=Rajdhani:wght@400;500;600;700&family=Share+Tech+Mono&display=swap'
      link.rel = 'stylesheet'
      link.onload = () => { this.fontsReady = true }
      document.head.appendChild(link)
    },

    /* ----- 数据加载（串行：防并发竞态）----- */
    async loadAll() {
      await this.loadDevice()
      this.loadVisitor()
      this.loadMeeting()
      this.loadApartment()
    },

    loadDevice() {
      return getDeviceDashboard().then(res => {
        const d = res.data || res
        this.deviceData.totalCount = d.totalCount || 0
        this.deviceData.onlineCount = d.onlineCount || 0
        this.deviceData.offlineCount = d.offlineCount || 0
        this.deviceData.onlineRate = d.onlineRate || 0
        this.deviceData.deviceTypeDistribution = (d.deviceTypeStats || []).map(t => ({
          name: this.deviceTypeName(t.type), value: t.count
        }))
        this.deviceData.recentEvents = this.buildEvents(d.recentAlerts || [])
        this.$nextTick(() => { setTimeout(() => this.initDevicePie(), 300) })
      }).catch(() => {})
    },

    loadVisitor() {
      getVisitorDashboard().then(res => {
        const d = res.data || res
        this.visitorData.todayVisitors = d.todayVisitors || 0
        this.visitorData.pendingApprovals = d.pendingApprovals || 0
        this.visitorData.totalAppointments = d.totalAppointments || 0
        this.visitorData.latestVisits = (d.recentVisits || []).slice(0, 5)
        this.visitorData.visitorStatusDistribution = (d.visitStats || []).map(s => ({
          name: this.visitorStatusName(s.status), value: s.count
        }))
        this.$nextTick(() => { setTimeout(() => this.initVisitorBar(), 300) })
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
        this.apartmentData.fullCount = blue
        this.apartmentData.occupancyRate = this.apartmentData.totalRooms > 0
          ? Math.round(this.apartmentData.occupiedCount / this.apartmentData.totalRooms * 100) : 0
        this.$nextTick(() => { setTimeout(() => this.initApartmentPie(), 300) })
      }).catch(() => {})
    },

    /* ----- 环形饼图: 设备类型分布 ----- */
    initDevicePie() {
      const el = this.$refs.devicePieChart; if (!el) return
      if (this.charts.devicePie) this.charts.devicePie.dispose()
      this.charts.devicePie = echarts.init(el)
      const data = this.deviceData.deviceTypeDistribution
      this.charts.devicePie.setOption({
        ...baseOpts(),
        series: [{
          type: 'pie',
          radius: ['50%', '78%'],
          center: ['50%', '52%'],
          itemStyle: { borderColor: '#141c26', borderWidth: 2, borderRadius: 2 },
          data: data.length > 0
            ? data.map((d, i) => ({ ...d, itemStyle: { color: C.pieColors[i % C.pieColors.length] } }))
            : [{ value: 1, name: '暂无', itemStyle: { color: '#3a4a58' } }],
          label: { color: C.text, fontSize: 11, fontFamily: 'Rajdhani, sans-serif', formatter: '{b}\n{d}%' },
          labelLine: { lineStyle: { color: 'rgba(138,155,176,0.25)' } },
          emphasis: { scaleSize: 6, label: { fontSize: 14, fontWeight: 'bold' } }
        }]
      })
    },

    /* ----- 柱状图: 访客状态分布 ----- */
    initVisitorBar() {
      const el = this.$refs.visitorBarChart; if (!el) return
      if (this.charts.visitorBar) this.charts.visitorBar.dispose()
      this.charts.visitorBar = echarts.init(el)
      const d = this.visitorData.visitorStatusDistribution
      this.charts.visitorBar.setOption({
        ...baseOpts(),
        grid: { top: 8, left: 0, right: 8, bottom: 0, containLabel: true },
        xAxis: {
          type: 'category',
          data: d.map(i => i.name),
          axisLine: { lineStyle: { color: 'rgba(138,155,176,0.2)' } },
          axisTick: { show: false },
          axisLabel: { color: C.text, fontSize: 10, fontFamily: 'Rajdhani, sans-serif' }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: C.grid, type: 'dashed' } },
          axisLabel: { color: C.textDim, fontSize: 10, fontFamily: 'Rajdhani, sans-serif' }
        },
        series: [{
          type: 'bar',
          barWidth: '40%',
          data: d.map((i, idx) => ({
            value: i.value,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: idx % 2 === 0 ? C.amber : C.cyan },
                { offset: 1, color: idx % 2 === 0 ? 'rgba(240,160,0,0.2)' : 'rgba(0,168,204,0.2)' }
              ]),
              borderRadius: [3, 3, 0, 0]
            }
          }))
        }]
      })
    },

    /* ----- 环形饼图: 公寓状态分布 ----- */
    initApartmentPie() {
      const el = this.$refs.apartmentPieChart; if (!el) return
      if (this.charts.apartmentPie) this.charts.apartmentPie.dispose()
      this.charts.apartmentPie = echarts.init(el)
      const colorMap = { GREEN: C.green, CYAN: C.cyan, BLUE: '#409EFF', GRAY: C.textDim }
      const data = this.apartmentData.statusDistribution.map(s => ({
        name: s.label, value: s.count,
        itemStyle: { color: colorMap[s.status] || C.textDim, borderColor: '#141c26', borderWidth: 2 }
      }))
      this.charts.apartmentPie.setOption({
        ...baseOpts(),
        series: [{
          type: 'pie',
          radius: ['50%', '78%'],
          center: ['50%', '52%'],
          data: data.length > 0 ? data : [{ value: 1, name: '暂无', itemStyle: { color: '#3a4a58' } }],
          label: { color: C.text, fontSize: 11, fontFamily: 'Rajdhani, sans-serif', formatter: '{b}\n{c}间' },
          labelLine: { lineStyle: { color: 'rgba(138,155,176,0.25)' } },
          emphasis: { scaleSize: 6 }
        }]
      })
    },

    /* ----- 辅助 ----- */
    buildEvents(alerts) {
      return (alerts || []).slice(0, 5).map(a => ({
        deviceName: a.deviceName,
        eventType: a.changeType === 'ONLINE' ? '恢复' : '离线',
        eventDetail: (a.oldStatus || '') + ' → ' + (a.newStatus || ''),
        eventTime: a.changeTime || a.createTime
      }))
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
      Object.values(this.charts).forEach(c => { try { c.resize() } catch (e) { /* noop */ } })
    },

    updateClock() {
      const d = new Date()
      this.clock = d.getFullYear() + '-' +
        String(d.getMonth() + 1).padStart(2, '0') + '-' +
        String(d.getDate()).padStart(2, '0') + '  ' +
        String(d.getHours()).padStart(2, '0') + ':' +
        String(d.getMinutes()).padStart(2, '0') + ':' +
        String(d.getSeconds()).padStart(2, '0')
    }
  }
}
</script>

<style scoped>
/* ================================================================
   CSS Custom Properties — 工业控制室 HMI Design System
   ================================================================ */
.cockpit {
  --bg-shell:    #0b1016;
  --bg-panel:    #141c26;
  --border:      #1f2e3d;
  --amber:       #f0a000;
  --cyan:        #00a8cc;
  --green:       #0ecb7e;
  --red:         #e8453c;
  --text-bright: #c8d6e0;
  --text-dim:    #8a9bb0;
  --text-muted:  #556678;
  --font-display:'Rajdhani', 'Barlow Semi Condensed', 'Oswald', sans-serif;
  --font-mono:   'Share Tech Mono', 'Consolas', 'Courier New', monospace;
}

/* ====== 全屏容器：Grid 按比例分配 4 区 ====== */
.cockpit {
  position: relative;
  margin: -20px;
  height: calc(100vh - 84px);
  min-height: 620px;
  display: grid;
  grid-template-rows: auto 5fr 3fr auto;
  gap: 8px;
  overflow: hidden;
  padding: 8px 10px 6px;
  background: var(--bg-shell);
  font-family: var(--font-display);
  -webkit-font-smoothing: antialiased;
}

/* ----- 暗纹网格背景 ----- */
.cockpit-bg {
  position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background-image:
    linear-gradient(rgba(240,160,0,0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(240,160,0,0.025) 1px, transparent 1px);
  background-size: 24px 24px;
}

/* 保证内容在背景之上 */
.cockpit > *:not(.cockpit-bg) { position: relative; z-index: 1; }

/* ====== KPI 指标条 ====== */
.kpi-strip {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.kpi-card {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 88px;
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-radius: 2px;
  overflow: hidden;
  cursor: default;
  transition: border-color 0.3s, transform 0.2s;
}
.kpi-card:hover {
  border-color: rgba(240,160,0,0.3);
  transform: translateY(-1px);
}

/* 顶部琥珀装饰线 */
.kpi-card::after {
  content: '';
  position: absolute; top: 0; left: 12px; right: 12px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--amber), transparent);
  opacity: 0.5;
}

/* 底部光晕条 */
.kpi-glow {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 2px;
  opacity: 0.6;
  transition: opacity 0.4s;
}
.kpi-card:hover .kpi-glow { opacity: 1; }
.kpi-glow--amber { background: linear-gradient(90deg, transparent, var(--amber), transparent); }
.kpi-glow--cyan  { background: linear-gradient(90deg, transparent, var(--cyan), transparent); }
.kpi-glow--green { background: linear-gradient(90deg, transparent, var(--green), transparent); }
.kpi-glow--red   { background: linear-gradient(90deg, transparent, var(--red), transparent); }

.kpi-value {
  font-family: var(--font-mono);
  font-size: 26px;
  font-weight: 700;
  color: var(--cyan);
  line-height: 1;
  letter-spacing: 0.5px;
}
.kpi-value--amber { color: var(--amber); }
.kpi-value--green { color: var(--green); }

.kpi-label {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 500;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-top: 4px;
}

.kpi-tag {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 500;
  padding: 1px 6px;
  border-radius: 2px;
  line-height: 1.3;
  margin-top: 3px;
}
.kpi-tag--cyan  { color: var(--cyan);  background: rgba(0,168,204,0.1); }
.kpi-tag--green { color: var(--green); background: rgba(14,203,126,0.1); }
.kpi-tag--amber { color: var(--amber); background: rgba(240,160,0,0.1); }
.kpi-tag--red   { color: var(--red);   background: rgba(232,69,60,0.1); }
.kpi-tag--dim   { color: var(--text-muted); }

.kpi-sub-row {
  display: flex; gap: 6px; margin-top: 1px;
}

/* ====== 面板通用 ====== */
.panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-radius: 2px;
  overflow: hidden;
  min-width: 0;
  transition: border-color 0.3s;
}
.panel:hover { border-color: rgba(240,160,0,0.2); }

/* 面板标题栏 */
.panel-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-bottom: 1px solid rgba(138,155,176,0.08);
  flex-shrink: 0;
  background: rgba(138,155,176,0.03);
}
.panel-head--compact { padding: 4px 10px; }

.panel-dot {
  display: inline-block;
  width: 6px; height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 5px currentColor;
}
.panel-dot--cyan  { color: var(--cyan);  background: var(--cyan); }
.panel-dot--amber { color: var(--amber); background: var(--amber); }
.panel-dot--green { color: var(--green); background: var(--green); }
.panel-dot--red   { color: var(--red);   background: var(--red); }

.panel-title {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-bright);
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.panel-badge {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--amber);
  background: rgba(240,160,0,0.12);
  padding: 1px 6px;
  border-radius: 2px;
  line-height: 1.3;
}
.panel-badge--red { color: var(--red); background: rgba(232,69,60,0.12); }

/* 面板内容区 */
.panel-body {
  flex: 1;
  position: relative;
  overflow: hidden;
}
.panel-body--table { padding: 0; }

.panel-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* ====== 图表行 / 表格行 ====== */
.chart-row,
.table-row {
  display: flex;
  gap: 8px;
  overflow: hidden;
  min-height: 0;
}
.chart-box { width: 100%; height: 100%; }

/* ====== 暗色表格 ====== */
.cockpit >>> .dt { background: transparent !important; color: var(--text-bright); }
.cockpit >>> .dt .el-table__header-wrapper th {
  background: rgba(138,155,176,0.06) !important;
  color: var(--text-dim);
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  border-bottom: 1px solid rgba(138,155,176,0.12) !important;
  padding: 5px 0 !important;
}
.cockpit >>> .dt .el-table__body-wrapper td {
  background: transparent !important;
  color: var(--text-bright);
  font-size: 11px;
  border-bottom: 1px solid rgba(138,155,176,0.05) !important;
  padding: 4px 0 !important;
}
.cockpit >>> .dt .el-table__body tr:hover > td {
  background: rgba(240,160,0,0.05) !important;
}
.cockpit >>> .dt .el-table__empty-text {
  color: var(--text-muted);
  font-family: var(--font-display);
  letter-spacing: 1px;
}

/* 表格内状态标签 */
.tb-status {
  display: inline-block;
  font-family: var(--font-display);
  font-size: 9px;
  font-weight: 600;
  padding: 0 4px;
  border-radius: 2px;
  letter-spacing: 0.5px;
}
.tb-status--ok  { color: var(--green); background: rgba(14,203,126,0.12); }
.tb-status--warn{ color: var(--amber); background: rgba(240,160,0,0.12); }
.tb-status--err { color: var(--red);   background: rgba(232,69,60,0.12); }

/* ====== 底部状态栏 ====== */
.status-bar {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  height: 20px;
  padding: 0 8px;
  background: rgba(138,155,176,0.04);
  border: 1px solid rgba(138,155,176,0.06);
  border-radius: 2px;
}

.status-led {
  display: inline-block;
  width: 5px; height: 5px;
  border-radius: 50%;
  box-shadow: 0 0 4px currentColor;
}
.status-led--green { color: var(--green); background: var(--green); }

.status-label {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 600;
  color: var(--text-dim);
  letter-spacing: 1px;
}
.status-sep {
  color: rgba(138,155,176,0.15);
  font-size: 10px;
}
.status-clock {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--amber);
  letter-spacing: 0.5px;
}

/* ====== 响应式 ====== */
@media (max-width: 1400px) {
  .kpi-value { font-size: 18px; }
  .kpi-label { font-size: 9px; letter-spacing: 0.5px; }
  .kpi-tag   { font-size: 8px; }
  .panel-title { font-size: 10px; letter-spacing: 0.5px; }
}
@media (max-width: 1100px) {
  .kpi-strip { flex-wrap: wrap; }
  .kpi-card { flex: 0 0 calc(25% - 5px); height: 56px; }
  .chart-row { flex-direction: column; min-height: 360px; }
  .table-row { flex-direction: column; height: auto; max-height: none; }
  .cockpit { overflow-y: auto; }
}
</style>
