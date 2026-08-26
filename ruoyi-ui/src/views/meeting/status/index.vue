<template>
  <!-- 会议室状态墙：深色指挥中心风格，适配投影大屏（电视机/显示墙），无操作列纯展示 -->
  <div class="meeting-status-wall">
    <!-- 顶部：标题 + 实时大时钟 + 刷新 -->
    <header class="wall-header">
      <div class="wall-title">
        <span class="title-flag"></span>
        <div>
          <h1 class="title-text">会议室状态墙</h1>
          <p class="title-sub">MEETING&nbsp;ROOM&nbsp;STATUS</p>
        </div>
      </div>
      <div class="wall-meta">
        <div class="clock">
          <span class="clock-time">{{ clock }}</span>
          <span class="clock-date">{{ dateText }}</span>
        </div>
        <div class="meta-side">
          <span class="live-hint" :class="{ on: true }">
            <i></i>{{ lastUpdateText }}
          </span>
          <button class="refresh-btn" :disabled="loading" @click="getStatus">
            {{ loading ? '更新中…' : '刷新' }}
          </button>
        </div>
      </div>
    </header>

    <!-- 状态统计条 -->
    <section class="stat-bar" aria-label="会议室状态统计">
      <div v-for="s in stats" :key="s.key" class="stat-item" :style="{ '--c': s.color }">
        <i class="stat-dot"></i>
        <span class="stat-num">{{ s.count }}</span>
        <span class="stat-label">{{ s.label }}</span>
      </div>
    </section>

    <!-- 房间瓦片网格（≥4 铺满整屏，<4 固定舒适尺寸居中） -->
    <template v-if="list.length">
      <main
        class="room-grid"
        :class="fillMode ? 'fill' : 'few'"
        :style="{ '--cols': gridCols, '--rows': gridRows }">
        <article
          v-for="item in pagedList"
          :key="item.room.roomId"
        class="room-card"
        :class="stateClass(item)"
        tabindex="0"
        role="button"
        :aria-label="'查看' + item.room.roomName + '今日预约记录'"
        @click="handleViewBookings(item)"
        @keydown.enter.prevent="handleViewBookings(item)">
        <!-- 卡片头部：状态 LED + 房间信息 + 状态徽标 -->
        <div class="card-top">
          <span class="led" :class="stateClass(item)"></span>
          <div class="card-head">
            <h2 class="room-name">{{ item.room.roomName }}</h2>
            <p class="room-sub">
              {{ item.room.location || '—' }}
              <span v-if="item.room.deptName"> · {{ item.room.deptName }}</span>
            </p>
          </div>
          <span class="state-badge" :class="stateClass(item)">{{ stateText(item) }}</span>
        </div>

        <!-- 卡片主体：当前 / 下一场 -->
        <div class="card-body">
          <div v-if="item.currentBooking" class="meeting-block">
            <span class="m-label">当前会议</span>
            <span class="m-title">{{ item.currentBooking.title }}</span>
            <span class="m-time">{{ fmtRange(item.currentBooking) }}</span>
          </div>
          <div v-else-if="item.nextBooking" class="meeting-block">
            <span class="m-label">下一场</span>
            <span class="m-title">{{ item.nextBooking.title }}</span>
            <span class="m-time">{{ fmtRange(item.nextBooking) }}</span>
          </div>
          <div v-else class="meeting-block empty">
            <span class="m-label">今日空闲</span>
            <span class="m-title">—</span>
          </div>
        </div>

        <!-- 今日时间轴（签名元素）：预约色块 + 实时“现在”指针 -->
        <div class="timeline" :class="{ dim: stateKey(item) === 'disabled' }">
          <div class="axis-labels">
            <span v-for="(t, i) in axisLabels" :key="t" :style="labelStyle(i)">{{ t }}</span>
          </div>
          <div class="axis-track">
            <span
              v-for="(b, bi) in item.todayBookings"
              :key="bi"
              class="axis-block"
              :class="[ 'st-' + statusKey(b.status), { 'is-active': blockActive(b) } ]"
              :style="blockStyle(b)"></span>
            <span class="axis-now" :style="nowStyle()"></span>
          </div>
          <p class="axis-foot">今日预约 {{ (item.todayBookings || []).length }} 场</p>
        </div>
        </article>
      </main>

      <!-- 分页导航（>16 个会议室时显示，大屏自动轮播 + 手动切换） -->
      <footer v-if="totalPages > 1" class="page-nav" aria-label="页面切换">
        <button class="page-btn" title="上一页" @click="prevPage">‹</button>
        <span class="page-no">{{ page }} / {{ totalPages }}</span>
        <div class="page-dots">
          <span v-for="p in totalPages" :key="p" class="page-dot" :class="{ on: p === page }" @click="goPage(p)"></span>
        </div>
        <button class="page-btn" title="下一页" @click="nextPage">›</button>
      </footer>
    </template>

    <!-- 无数据兜底 -->
    <main v-else class="room-empty">
      <el-empty :image-size="160" description="暂无会议室数据"></el-empty>
    </main>

    <!-- 今日预约记录弹窗（点击卡片打开） -->
    <el-dialog
      :title="bookingDialog.title"
      :visible.sync="bookingDialog.open"
      width="620px"
      custom-class="wall-dialog"
      append-to-body>
      <el-empty v-if="bookingDialog.list.length === 0" description="今日暂无预约记录"/>
      <el-timeline v-else class="wall-timeline">
        <el-timeline-item
          v-for="(b, idx) in bookingDialog.list"
          :key="idx"
          :timestamp="fmtRange(b)"
          :type="bookingStatusType(b.status)">
          <div class="b-title">{{ b.title }}</div>
          <div class="b-sub">主持人：{{ b.hostName || '—' }}</div>
          <div class="b-sub">状态：{{ bookingStatusText(b.status) }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script>
import { statusList } from '@/api/meeting/room'

// 时间轴固定 0:00–24:00（全天），刻度固定 9 个整点平分长度
const AXIS_START = 0
const AXIS_END = 24 * 60

// 每页最多会议室数量（超出自动分页，大屏轮播）
const PAGE_SIZE = 16
// 会议室数量 → 网格形状（按 2:1 大屏内容区调优，让卡片铺满整屏，可微调）
const GRID_TABLE = {
  4: '2x2', 5: '3x2', 6: '3x2', 7: '4x2', 8: '4x2',
  9: '3x3', 10: '4x3', 11: '4x3', 12: '4x3',
  13: '5x3', 14: '5x3', 15: '5x3', 16: '4x4'
}
// 多页自动轮播间隔（毫秒）
const PAGE_INTERVAL = 20000

export default {
  name: 'MeetingStatus',
  data() {
    return {
      loading: false,
      list: [],
      nowMs: null,
      clock: '--:--:--',
      dateText: '',
      lastUpdateText: '等待刷新',
      clockTimer: null,
      dataTimer: null,
      page: 1,
      pageTimer: null,
      bookingDialog: { open: false, title: '', list: [] }
    }
  },
  computed: {
    /** 状态统计条（色点 + 计数） */
    stats() {
      const m = { in_use: 0, coming_soon: 0, free: 0, disabled: 0 }
      this.list.forEach(it => {
        const key = this.stateKey(it)
        if (m[key] !== undefined) m[key] += 1
      })
      return [
        { key: 'in_use', label: '使用中', count: m.in_use, color: '#FF5A5F' },
        { key: 'coming_soon', label: '即将开始', count: m.coming_soon, color: '#FFC53D' },
        { key: 'free', label: '空闲', count: m.free, color: '#34D399' },
        { key: 'disabled', label: '停用', count: m.disabled, color: '#6B7FA3' }
      ]
    },
    /** 时间轴刻度：固定 0/3/6/9/12/15/18/21/24 点共 9 个整点，平分整条长度 */
    axisLabels() {
      return ['0:00', '3:00', '6:00', '9:00', '12:00', '15:00', '18:00', '21:00', '24:00']
    },
    /** 今日时间轴固定 0:00–24:00（分钟），刻度与色块/“现在”指针共用同一全天比例 */
    axisStart() { return AXIS_START },
    axisEnd() { return AXIS_END },
    /** 当前页可见会议室（分页切片，每页最多 PAGE_SIZE 个） */
    pagedList() {
      const start = (this.page - 1) * PAGE_SIZE
      return this.list.slice(start, start + PAGE_SIZE)
    },
    /** 分页总数（至少 1 页） */
    totalPages() {
      return Math.max(1, Math.ceil(this.list.length / PAGE_SIZE))
    },
    /** 当前页网格形状 cols×rows：4–16 铺满整屏，<4 固定舒适尺寸 */
    gridShape() {
      const n = this.pagedList.length
      const hit = GRID_TABLE[n]
      if (hit) {
        const [c, r] = hit.split('x').map(Number)
        return { cols: c, rows: r }
      }
      return { cols: n === 1 ? 1 : 2, rows: Math.max(1, Math.ceil(n / 2)) }
    },
    gridCols() { return this.gridShape.cols },
    gridRows() { return this.gridShape.rows },
    /** 是否铺满模式（当前页 ≥4 个，充分使用整个页面空间） */
    fillMode() { return this.pagedList.length >= 4 }
  },
  created() {
    this.tickClock()
    this.clockTimer = setInterval(() => { this.tickClock() }, 1000)
    this.getStatus()
    // 大屏常驻：每 30 秒自动刷新状态
    this.dataTimer = setInterval(() => { this.getStatus() }, 30000)
    // 大屏常驻：多页时每 20 秒自动轮播（totalPages=1 时 nextPage 自回到 1，无副作用）
    this.pageTimer = setInterval(() => { this.nextPage() }, PAGE_INTERVAL)
  },
  beforeDestroy() {
    if (this.clockTimer) { clearInterval(this.clockTimer); this.clockTimer = null }
    if (this.dataTimer) { clearInterval(this.dataTimer); this.dataTimer = null }
    if (this.pageTimer) { clearInterval(this.pageTimer); this.pageTimer = null }
  },
  methods: {
    /** 拉取会议室状态总览 */
    getStatus() {
      this.loading = true
      statusList().then(response => {
        this.list = response.data || []
        // 会议室数量变化后钳制页码，避免停留在不存在的页
        const tp = Math.max(1, Math.ceil(this.list.length / PAGE_SIZE))
        if (this.page > tp) this.page = tp
        const d = new Date()
        this.lastUpdateText = '更新 ' + this.pad(d.getHours()) + ':' + this.pad(d.getMinutes()) + ':' + this.pad(d.getSeconds())
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    /** 每秒刷新实时时钟与“现在”指针时间戳 */
    tickClock() {
      const d = new Date()
      this.nowMs = d.getTime()
      this.clock = this.pad(d.getHours()) + ':' + this.pad(d.getMinutes()) + ':' + this.pad(d.getSeconds())
      const weeks = ['日', '一', '二', '三', '四', '五', '六']
      this.dateText = (d.getMonth() + 1) + '月' + d.getDate() + '日 · 星期' + weeks[d.getDay()]
    },
    /** 查看某会议室今日预约记录 */
    handleViewBookings(item) {
      this.bookingDialog.title = '「' + item.room.roomName + '」今日预约记录'
      this.bookingDialog.list = item.todayBookings || []
      this.bookingDialog.open = true
    },
    /* ---------- 分页（大屏自动轮播 + 手动切换） ---------- */
    /** 上一页（首页回卷到末页，大屏循环播放） */
    prevPage() {
      this.page = this.page <= 1 ? this.totalPages : this.page - 1
    },
    /** 下一页（末页回卷到首页，大屏循环播放） */
    nextPage() {
      this.page = this.page >= this.totalPages ? 1 : this.page + 1
    },
    /** 跳转指定页 */
    goPage(p) {
      this.page = Math.min(Math.max(1, p), this.totalPages)
    },
    /* ---------- 状态映射 ---------- */
    stateKey(item) {
      if (item.room.status === '1') return 'disabled'
      return (item.currentStatus || 'FREE').toLowerCase().replace('-', '_')
    },
    stateClass(item) {
      return 'is-' + this.stateKey(item)
    },
    stateText(item) {
      const map = { in_use: '使用中', coming_soon: '即将开始', free: '空闲', disabled: '停用' }
      return map[this.stateKey(item)] || '空闲'
    },
    statusKey(status) {
      return (status || '').toLowerCase()
    },
    /* ---------- 时间工具 ---------- */
    pad(n) { return n < 10 ? '0' + n : '' + n },
    toDate(t) {
      if (!t) return null
      return new Date(String(t).replace('T', ' ').replace(/-/g, '/'))
    },
    hm(d) {
      if (!d) return ''
      return this.pad(d.getHours()) + ':' + this.pad(d.getMinutes())
    },
    fmtRange(b) {
      if (!b) return ''
      const s = this.toDate(b.startTime)
      const e = this.toDate(b.endTime)
      if (!s || !e) return ''
      return this.hm(s) + ' – ' + this.hm(e)
    },
    /* ---------- 时间轴几何 ---------- */
    /** 刻度标签定位：i 为 0..8，按 3 小时一格精确落在 0/12.5/25/…/100% 处（中点对齐） */
    labelStyle(i) {
      return { left: (i / (this.axisLabels.length - 1) * 100) + '%' }
    },
    /** 预约色块位置（相对轴起止百分比） */
    blockStyle(b) {
      const s = this.toDate(b.startTime)
      const e = this.toDate(b.endTime)
      if (!s || !e) return { display: 'none' }
      const span = this.axisEnd - this.axisStart
      const sm = s.getHours() * 60 + s.getMinutes()
      const em = e.getHours() * 60 + e.getMinutes()
      if (em <= sm) return { display: 'none' }
      return {
        left: ((sm - this.axisStart) / span * 100) + '%',
        width: ((em - sm) / span * 100) + '%'
      }
    },
    /** 实时“现在”指针位置 */
    nowStyle() {
      if (!this.nowMs) return { display: 'none' }
      const d = new Date(this.nowMs)
      const m = d.getHours() * 60 + d.getMinutes()
      if (m < this.axisStart || m > this.axisEnd) return { display: 'none' }
      return { left: ((m - this.axisStart) / (this.axisEnd - this.axisStart) * 100) + '%' }
    },
    /** 块是否处于“正在进行的当前时段” */
    blockActive(b) {
      if (!this.nowMs) return false
      const s = this.toDate(b.startTime)
      const e = this.toDate(b.endTime)
      return !!(s && e && this.nowMs >= s.getTime() && this.nowMs < e.getTime())
    },
    /* ---------- 预约状态文案（弹窗） ---------- */
    bookingStatusText(status) {
      const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }
      return map[status] || status || '—'
    },
    bookingStatusType(status) {
      const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
      return map[status] || 'primary'
    }
  }
}
</script>

<style scoped>
/* ============ 大屏指挥中心墙：深蓝渐变 + 青色科技点缀 ============ */
.meeting-status-wall {
  --bg0: #0a1733;
  --bg1: #0e2a63;
  --panel: rgba(16, 42, 96, 0.72);
  --panel-line: #2b4a8c;
  --accent: #00d4ff;
  --txt-hi: #eef4ff;
  --txt-lo: #8fa9d8;
  --ok: #34d399;
  --warn: #ffc53d;
  --bad: #ff5a5f;
  --off: #6b7fa3;

  display: flex;
  flex-direction: column;
  gap: clamp(14px, 1.6vh, 26px);
  /* 铺满 app-main 可视区（navbar 高 50px，故高度 = 100vh - 50px）：
     给内部 flex:1 网格确定高度，1fr 行才能按剩余空间均分，N 个卡片始终完整铺满不溢出；
     横向 margin:-20px 抵消水平边距，内容与侧边栏/右缘齐平 */
  height: calc(100vh - 50px);
  margin: 0 -20px;
  padding: clamp(18px, 2.4vh, 34px) clamp(20px, 3vw, 48px);
  box-sizing: border-box;
  color: var(--txt-hi);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  background-color: var(--bg0);
  background-image:
    linear-gradient(160deg, var(--bg0) 0%, var(--bg1) 100%),
    linear-gradient(rgba(0, 212, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.04) 1px, transparent 1px);
  background-size: 100% 100%, 40px 40px, 40px 40px;
  overflow: hidden;
}

/* ============ 顶部：标题 + 时钟 ============ */
.wall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}
.wall-title { display: flex; align-items: center; gap: 14px; }
.title-flag {
  width: clamp(10px, 1vw, 16px);
  height: clamp(10px, 1vw, 16px);
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 14px var(--accent);
}
.title-text {
  margin: 0;
  font-size: clamp(26px, 3.2vw, 46px);
  font-weight: 700;
  letter-spacing: 0.04em;
  line-height: 1.1;
}
.title-sub {
  margin: 4px 0 0;
  font-size: clamp(10px, 1.1vw, 14px);
  letter-spacing: 0.34em;
  color: var(--txt-lo);
}
.wall-meta { display: flex; align-items: center; gap: clamp(14px, 2vw, 28px); }
.clock { text-align: right; line-height: 1.1; }
.clock-time {
  font-family: Consolas, "JetBrains Mono", "Roboto Mono", monospace;
  font-size: clamp(30px, 4vw, 60px);
  font-weight: 700;
  color: var(--accent);
  text-shadow: 0 0 22px rgba(0, 212, 255, 0.4);
  letter-spacing: 0.02em;
}
.clock-date { display: block; margin-top: 4px; font-size: clamp(12px, 1.3vw, 16px); color: var(--txt-lo); }
.meta-side { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.live-hint {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: clamp(11px, 1.1vw, 14px); color: var(--txt-lo); white-space: nowrap;
}
.live-hint i {
  width: 8px; height: 8px; border-radius: 50%; background: var(--ok);
  animation: hintBlink 2s ease-in-out infinite;
}
.refresh-btn {
  padding: 6px 16px;
  font-size: clamp(12px, 1.2vw, 15px);
  color: var(--accent);
  background: rgba(0, 212, 255, 0.1);
  border: 1px solid rgba(0, 212, 255, 0.45);
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}
.refresh-btn:hover { background: rgba(0, 212, 255, 0.22); }
.refresh-btn:disabled { opacity: 0.55; cursor: not-allowed; }

/* ============ 状态统计条 ============ */
.stat-bar {
  display: flex;
  flex-wrap: wrap;
  gap: clamp(10px, 1.4vw, 20px);
}
.stat-item {
  display: flex; align-items: center; gap: 10px;
  padding: clamp(8px, 1vh, 14px) clamp(14px, 1.6vw, 24px);
  background: var(--panel);
  border: 1px solid var(--panel-line);
  border-radius: 8px;
}
.stat-dot {
  width: clamp(8px, 0.9vw, 12px); height: clamp(8px, 0.9vw, 12px);
  border-radius: 50%; background: var(--c);
  box-shadow: 0 0 10px var(--c);
}
.stat-num {
  font-family: Consolas, "JetBrains Mono", monospace;
  font-size: clamp(22px, 2.6vw, 38px); font-weight: 700; line-height: 1;
}
.stat-label { font-size: clamp(12px, 1.3vw, 16px); color: var(--txt-lo); }

/* ============ 房间瓦片网格（大屏动态铺满：≥4 整屏铺满 / <4 居中舒适） ============ */
.room-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  gap: clamp(14px, 1.8vw, 26px);
}
/* 铺满模式（≥4 个）：按 cols×rows 双向均分，充分使用整个页面空间 */
/* 注意 minmax(0, 1fr)：纯 1fr 会尊重卡片内容最小宽度导致轨道不均等/溢出，0 下限强制等分铺满 */
.room-grid.fill {
  grid-template-columns: repeat(var(--cols, 2), minmax(0, 1fr));
  grid-template-rows: repeat(var(--rows, 2), minmax(0, 1fr));
}
/* 少量模式（<4 个）：固定舒适尺寸居中，不拉伸铺满 */
.room-grid.few {
  grid-template-columns: repeat(var(--cols, 2), minmax(300px, 540px));
  grid-auto-rows: auto;
  justify-content: center;
  align-content: center;
}
.room-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: clamp(14px, 1.8vw, 24px);
  box-sizing: border-box;
  background: var(--panel);
  border: 1px solid var(--panel-line);
  border-radius: 12px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}
.room-grid.few .room-card { min-height: clamp(240px, 30vh, 400px); }
.room-card:hover,
.room-card:focus-visible {
  border-color: var(--accent);
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(0, 212, 255, 0.4);
}

/* 状态边框着色：空闲绿 / 即将黄 / 使用中红（呼吸光晕）/ 停用灰 */
.room-card.is-free { border-color: rgba(52, 211, 153, 0.5); }
.room-card.is-coming_soon { border-color: rgba(255, 197, 61, 0.55); }
.room-card.is-in_use {
  border-color: rgba(255, 90, 95, 0.7);
  animation: cardPulse 2.4s ease-in-out infinite;
}
.room-card.is-disabled { opacity: 0.55; }

/* ============ 卡片头部 ============ */
.card-top { display: flex; align-items: center; gap: 12px; }
.led {
  width: clamp(12px, 1.4vw, 18px); height: clamp(12px, 1.4vw, 18px);
  border-radius: 50%; background: var(--off);
  flex-shrink: 0;
}
.led.is-free { background: var(--ok); box-shadow: 0 0 12px var(--ok); }
.led.is-coming_soon { background: var(--warn); box-shadow: 0 0 12px var(--warn); animation: ledPulse 1.6s ease-in-out infinite; }
.led.is-in_use { background: var(--bad); box-shadow: 0 0 14px var(--bad); animation: ledPulse 1.2s ease-in-out infinite; }
.led.is-disabled { background: var(--off); }
.card-head { flex: 1; min-width: 0; }
.room-name {
  margin: 0;
  font-size: clamp(20px, 2.4vw, 34px);
  font-weight: 700;
  line-height: 1.15;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.room-sub { margin: 4px 0 0; font-size: clamp(11px, 1.2vw, 15px); color: var(--txt-lo); }
.state-badge {
  flex-shrink: 0;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: clamp(12px, 1.3vw, 16px);
  border: 1px solid;
}
.state-badge.is-free { color: var(--ok); border-color: rgba(52, 211, 153, 0.55); background: rgba(52, 211, 153, 0.12); }
.state-badge.is-coming_soon { color: var(--warn); border-color: rgba(255, 197, 61, 0.55); background: rgba(255, 197, 61, 0.12); }
.state-badge.is-in_use { color: var(--bad); border-color: rgba(255, 90, 95, 0.6); background: rgba(255, 90, 95, 0.14); }
.state-badge.is-disabled { color: var(--txt-lo); border-color: rgba(111, 127, 163, 0.5); background: rgba(111, 127, 163, 0.12); }

/* ============ 卡片主体：当前 / 下一场 ============ */
.card-body { flex: 1; display: flex; flex-direction: column; justify-content: center; padding: clamp(10px, 1.4vh, 18px) 0; }
.meeting-block { display: flex; flex-direction: column; gap: 4px; }
.m-label { font-size: clamp(10px, 1.1vw, 13px); color: var(--txt-lo); letter-spacing: 0.08em; }
.m-title {
  font-size: clamp(16px, 1.9vw, 26px); font-weight: 600;
  color: var(--txt-hi);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.m-time {
  font-family: Consolas, "JetBrains Mono", monospace;
  font-size: clamp(13px, 1.4vw, 18px); color: var(--accent);
}
.meeting-block.empty .m-title { color: var(--txt-lo); }

/* ============ 今日时间轴（签名元素） ============ */
.timeline { padding-top: clamp(8px, 1vh, 14px); border-top: 1px solid rgba(43, 74, 140, 0.6); }
.timeline.dim { opacity: 0.6; }
.axis-labels {
  position: relative;
  height: clamp(16px, 2vh, 24px);
  font-family: Consolas, "JetBrains Mono", monospace;
  font-size: clamp(10px, 1vw, 13px); color: var(--txt-lo);
  margin-bottom: 6px;
}
.axis-labels span {
  position: absolute; top: 0;
  transform: translateX(-50%);
  white-space: nowrap;
}
.axis-labels span:first-child { transform: translateX(0); }
.axis-labels span:last-child { transform: translateX(-100%); }
.axis-track {
  position: relative;
  height: clamp(14px, 2vh, 22px);
  background: rgba(4, 14, 38, 0.7);
  border: 1px solid rgba(43, 74, 140, 0.7);
  border-radius: 4px;
  overflow: hidden;
}
.axis-block {
  position: absolute; top: 0; bottom: 0;
  border-radius: 2px;
}
/* 已通过：青色实心；待审批：黄色斜纹；拒绝/取消：灰淡 */
.axis-block.st-approved { background: rgba(0, 212, 255, 0.55); }
.axis-block.st-approved.is-active { background: var(--bad); box-shadow: 0 0 12px rgba(255, 90, 95, 0.6); }
.axis-block.st-pending { background: repeating-linear-gradient(45deg, rgba(255, 197, 61, 0.55) 0 6px, rgba(255, 197, 61, 0.3) 6px 12px); }
.axis-block.st-rejected,
.axis-block.st-cancelled { background: rgba(143, 169, 216, 0.22); }
/* 实时“现在”指针 */
.axis-now {
  position: absolute; top: -3px; bottom: -3px; width: 2px;
  background: #ffffff;
  box-shadow: 0 0 8px #ffffff;
  pointer-events: none;
}
.axis-foot { margin: 6px 0 0; font-size: clamp(10px, 1vw, 13px); color: var(--txt-lo); }

/* ============ 空态 ============ */
.room-empty { flex: 1; display: flex; align-items: center; justify-content: center; }

/* ============ 分页导航（多页自动轮播 + 手动切换） ============ */
.page-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding-top: clamp(6px, 0.8vh, 12px);
}
.page-btn {
  width: 34px; height: 34px;
  border-radius: 50%;
  color: var(--accent);
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.4);
  font-size: 18px; line-height: 1;
  cursor: pointer;
  transition: background 0.2s;
}
.page-btn:hover { background: rgba(0, 212, 255, 0.22); }
.page-no {
  font-family: Consolas, "JetBrains Mono", monospace;
  font-size: 14px; color: var(--txt-lo);
  min-width: 34px; text-align: center;
}
.page-dots { display: flex; align-items: center; gap: 8px; }
.page-dot {
  width: 9px; height: 9px; border-radius: 50%;
  background: rgba(143, 169, 216, 0.35);
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}
.page-dot.on { background: var(--accent); box-shadow: 0 0 8px var(--accent); transform: scale(1.3); }

/* ============ 动画（尊重 reduced-motion） ============ */
@keyframes ledPulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.45; } }
@keyframes hintBlink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
@keyframes cardPulse {
  0%, 100% { box-shadow: 0 0 0 rgba(255, 90, 95, 0); }
  50% { box-shadow: 0 0 24px rgba(255, 90, 95, 0.25); }
}
@media (prefers-reduced-motion: reduce) {
  .room-card.is-in_use, .led.is-coming_soon, .led.is-in_use, .live-hint i,
  .axis-block.st-approved.is-active { animation: none; }
}
</style>

<!-- 预约记录弹窗为 append-to-body，DOM 挂在 body 下，scoped 样式不生效，故用全局样式（写死色值） -->
<style>
.wall-dialog .el-dialog__header,
.wall-dialog .el-dialog__body { background: #0e2a63; }
.wall-dialog .el-dialog__title { color: #eef4ff; font-weight: 600; }
.wall-dialog .el-dialog__headerbtn .el-dialog__close { color: #8fa9d8; }
.wall-dialog .el-dialog__headerbtn:hover .el-dialog__close { color: #00d4ff; }
.wall-dialog .el-timeline-item__timestamp { color: #8fa9d8; }
.wall-dialog .el-empty__description { color: #8fa9d8; }
.wall-dialog .b-title { font-size: 15px; font-weight: 600; color: #eef4ff; }
.wall-dialog .b-sub { font-size: 12px; color: #8fa9d8; margin-top: 2px; }
</style>
