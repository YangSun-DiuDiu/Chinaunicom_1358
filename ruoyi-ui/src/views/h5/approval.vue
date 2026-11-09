<template>
  <div
    class="h5-page"
    @touchstart.passive="onTouchStart"
    @touchmove.passive="onTouchMove"
    @touchend="onTouchEnd"
  >
    <div class="pull-tip" :style="{ height: pullDistance + 'px' }">
      <span v-if="pullDistance > 0">{{ pullText }}</span>
    </div>

    <div class="topbar">
      <div class="back" @click="goHome">‹</div>
      <div class="title">来访审批</div>
      <div class="user">
        <span class="user-name">{{ username }}</span>
        <span class="logout" @click="logout">退出</span>
      </div>
    </div>

    <div class="tabs">
      <div class="tab" :class="{ active: activeTab === 'pending' }" @click="switchTab('pending')">
        待审批
        <span v-if="pendingList.length" class="badge">{{ pendingList.length }}</span>
      </div>
      <div class="tab" :class="{ active: activeTab === 'done' }" @click="switchTab('done')">
        已办
        <span v-if="doneList.length" class="badge done-badge">{{ doneList.length }}</span>
      </div>
    </div>

    <div v-if="loading" class="state">
      <i class="el-icon-loading"></i>
      <span>加载中…</span>
    </div>

    <div v-else-if="loadError" class="state error">
      <div>{{ loadError }}</div>
      <div class="retry" @click="refresh">点击重试</div>
    </div>

    <template v-else-if="activeTab === 'pending'">
      <div v-if="pendingList.length === 0" class="empty">
        <div class="empty-icon">✅</div>
        <div class="empty-text">暂无待审批的来访申请</div>
        <div class="empty-sub">下拉可刷新列表</div>
      </div>

      <div v-else class="card-list">
        <div v-for="item in pendingList" :key="item.appointmentId" class="appt-card">
          <div class="card-head">
            <div class="visitor">
              <span class="visitor-name">{{ item.visitorName }}</span>
              <span v-if="item.visitorCompany" class="visitor-company">{{ item.visitorCompany }}</span>
            </div>
            <span class="tag tag-pending">待审批</span>
          </div>

          <div class="info-row"><span class="k">来访事由</span><span class="v">{{ item.visitReason || '-' }}</span></div>
          <div class="info-row"><span class="k">来访时间</span><span class="v">{{ formatTime(item.visitTime) }}</span></div>
          <div class="info-row">
            <span class="k">被访人</span>
            <span class="v">{{ item.hostName || '-' }}<span v-if="item.hostDept" class="dept"> · {{ item.hostDept }}</span></span>
          </div>
          <div class="info-row"><span class="k">访客电话</span><span class="v">{{ item.visitorPhone || '-' }}</span></div>
          <div class="info-row"><span class="k">提交时间</span><span class="v">{{ formatTime(item.createTime) }}</span></div>

          <div class="card-actions">
            <button class="btn btn-reject" @click="openReject(item)">拒 绝</button>
            <button class="btn btn-approve" @click="doApprove(item)">通 过</button>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div v-if="doneList.length === 0" class="empty">
        <div class="empty-icon">📭</div>
        <div class="empty-text">暂无已办记录</div>
        <div class="empty-sub">你审批过的申请会出现在这里</div>
      </div>

      <div v-else class="card-list">
        <div v-for="item in doneList" :key="'done-' + item.appointmentId" class="appt-card">
          <div class="card-head">
            <div class="visitor">
              <span class="visitor-name">{{ item.visitorName }}</span>
              <span v-if="item.visitorCompany" class="visitor-company">{{ item.visitorCompany }}</span>
            </div>
            <span class="tag" :class="item.status === 'APPROVED' ? 'tag-ok' : 'tag-reject'">
              {{ item.status === 'APPROVED' ? '已通过' : '已拒绝' }}
            </span>
          </div>

          <div class="info-row">
            <span class="k">通行码</span>
            <span class="v">
              <span v-if="item.passCode" class="pass-code">{{ item.passCode }}</span>
              <span v-else class="muted">-</span>
            </span>
          </div>
          <div class="info-row"><span class="k">来访时间</span><span class="v">{{ formatTime(item.visitTime) }}</span></div>
          <div class="info-row">
            <span class="k">审批意见</span>
            <span class="v">{{ item.approveRemark || '-' }}</span>
          </div>
          <div class="info-row"><span class="k">审批时间</span><span class="v">{{ formatTime(item.approveTime) }}</span></div>
        </div>
      </div>
    </template>

    <div class="footer">
      <p>智慧园区管理系统</p>
    </div>

    <div v-if="rejectVisible" class="mask" @click.self="closeReject">
      <div class="sheet">
        <div class="sheet-title">拒绝来访申请</div>
        <div class="sheet-sub">{{ rejectTarget.visitorName }} · {{ rejectTarget.visitReason || '来访申请' }}</div>
        <textarea
          v-model="rejectRemark"
          class="sheet-input"
          maxlength="200"
          placeholder="请填写拒绝理由（必填，200 字以内）"
        ></textarea>
        <div class="sheet-count">{{ rejectRemark.length }}/200</div>
        <div class="sheet-actions">
          <button class="btn btn-ghost" @click="closeReject">取 消</button>
          <button class="btn btn-reject" :disabled="submitting" @click="submitReject">
            {{ submitting ? '提交中…' : '确认拒绝' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { pendingAppointment, doneAppointment, approveAppointment } from '@/api/visitor/visitor'
import { parseTime } from '@/utils/ruoyi'

export default {
  name: 'H5Approval',
  data() {
    return {
      activeTab: 'pending',
      pendingList: [],
      doneList: [],
      loading: false,
      loadError: '',
      username: '',
      // 下拉刷新
      pullDistance: 0,
      pullText: '',
      startY: 0,
      pulling: false,
      // 拒绝弹层
      rejectVisible: false,
      rejectTarget: {},
      rejectRemark: '',
      submitting: false
    }
  },
  created() {
    this.username = this.$store.state.user.name || ''
    this.loadPending()
    this.loadDone()
  },
  methods: {
    formatTime(time) {
      return time ? parseTime(time, '{y}-{m}-{d} {h}:{i}') : '-'
    },
    goHome() {
      this.$router.push('/h5')
    },
    logout() {
      this.$store.dispatch('LogOut').then(() => this.$router.push('/h5/login'))
    },
    switchTab(tab) {
      if (this.activeTab === tab) return
      this.activeTab = tab
      this.refresh()
    },
    refresh() {
      if (this.activeTab === 'pending') {
        this.loadPending()
      } else {
        this.loadDone()
      }
    },
    loadPending() {
      this.loading = true
      this.loadError = ''
      pendingAppointment()
        .then(res => {
          this.pendingList = (res && res.data) || []
        })
        .catch(err => {
          this.loadError = (err && err.message) || '待审批列表加载失败'
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadDone() {
      doneAppointment()
        .then(res => {
          this.doneList = (res && res.data) || []
        })
        .catch(() => {
          // 已办列表失败不阻塞待审批，切换页签时再重试
          this.doneList = []
        })
    },
    doApprove(item) {
      if (this.submitting) return
      this.submitting = true
      approveAppointment({ appointmentId: item.appointmentId, status: 'APPROVED' })
        .then(() => {
          this.$message.success('已通过，通行码已发送给访客')
          this.pendingList = this.pendingList.filter(row => row.appointmentId !== item.appointmentId)
          this.loadDone()
        })
        .catch(() => {
          // 失败提示由请求拦截器统一弹出，这里只刷新列表以对齐真实状态
          this.refresh()
        })
        .finally(() => {
          this.submitting = false
        })
    },
    openReject(item) {
      this.rejectTarget = item
      this.rejectRemark = ''
      this.rejectVisible = true
    },
    closeReject() {
      this.rejectVisible = false
      this.rejectRemark = ''
    },
    submitReject() {
      const remark = this.rejectRemark.trim()
      if (!remark) {
        this.$message.warning('请填写拒绝理由')
        return
      }
      if (this.submitting) return
      this.submitting = true
      approveAppointment({
        appointmentId: this.rejectTarget.appointmentId,
        status: 'REJECTED',
        approveRemark: remark
      })
        .then(() => {
          this.$message.success('已拒绝该来访申请')
          this.pendingList = this.pendingList.filter(
            row => row.appointmentId !== this.rejectTarget.appointmentId
          )
          this.closeReject()
          this.loadDone()
        })
        .catch(() => {
          this.refresh()
        })
        .finally(() => {
          this.submitting = false
        })
    },
    // 下拉刷新：仅在页面顶部、非加载中时生效
    onTouchStart(e) {
      if (this.loading || window.scrollY > 0) return
      this.startY = e.touches[0].clientY
      this.pulling = true
    },
    onTouchMove(e) {
      if (!this.pulling) return
      const delta = e.touches[0].clientY - this.startY
      if (delta <= 0) {
        this.pullDistance = 0
        return
      }
      this.pullDistance = Math.min(delta * 0.4, 56)
      this.pullText = this.pullDistance >= 40 ? '松开刷新' : '下拉刷新'
    },
    onTouchEnd() {
      if (!this.pulling) return
      const shouldRefresh = this.pullDistance >= 40
      this.pulling = false
      this.pullDistance = 0
      this.pullText = ''
      if (shouldRefresh) {
        this.refresh()
      }
    }
  }
}
</script>

<style scoped>
.h5-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a1530 0%, #1a2a52 60%, #5645d4 100%);
  padding: 0 16px 20px;
  box-sizing: border-box;
}
.pull-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  transition: height 0.15s ease;
}
.topbar {
  display: flex;
  align-items: center;
  padding: 18px 0 14px;
}
.back {
  width: 32px;
  height: 32px;
  line-height: 30px;
  text-align: center;
  font-size: 24px;
  color: #fff;
  cursor: pointer;
}
.title {
  flex: 1;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-right: -72px;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 72px;
  justify-content: flex-end;
}
.user-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.logout {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  padding: 2px 6px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 10px;
  cursor: pointer;
}
.tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 14px;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.2s;
}
.tab.active {
  background: #fff;
  color: #37352f;
  font-weight: 700;
}
.badge {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 5px;
  margin-left: 4px;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 400;
  color: #fff;
  background: #5645d4;
  vertical-align: 1px;
}
.tab.active .badge {
  background: #5645d4;
}
.done-badge {
  background: rgba(255, 255, 255, 0.35);
}
.tab.active .done-badge {
  background: #8a8a8a;
}
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 420px;
  margin: 0 auto;
}
.appt-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: rgba(15, 15, 15, 0.15) 0 4px 14px 0;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #f0eeea;
}
.visitor {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}
.visitor-name {
  font-size: 17px;
  font-weight: 700;
  color: #37352f;
}
.visitor-company {
  font-size: 12px;
  color: #787671;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}
.tag {
  flex-shrink: 0;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
}
.tag-pending {
  color: #b25e09;
  background: rgba(255, 159, 10, 0.12);
}
.tag-ok {
  color: #1f7a4d;
  background: rgba(52, 199, 89, 0.12);
}
.tag-reject {
  color: #b02a2a;
  background: rgba(255, 59, 48, 0.1);
}
.info-row {
  display: flex;
  align-items: flex-start;
  padding: 3px 0;
  font-size: 13px;
  line-height: 1.6;
}
.info-row .k {
  flex-shrink: 0;
  width: 62px;
  color: #9a978f;
}
.info-row .v {
  flex: 1;
  color: #37352f;
  word-break: break-all;
}
.dept {
  color: #787671;
}
.muted {
  color: #9a978f;
}
.pass-code {
  font-family: Menlo, Consolas, monospace;
  letter-spacing: 1px;
  color: #5645d4;
  font-weight: 700;
}
.card-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}
.btn {
  flex: 1;
  height: 40px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.btn:active {
  transform: scale(0.98);
}
.btn:disabled {
  opacity: 0.6;
}
.btn-approve {
  background: #5645d4;
  color: #fff;
}
.btn-reject {
  background: rgba(255, 59, 48, 0.1);
  color: #b02a2a;
}
.btn-ghost {
  background: #f3f2ef;
  color: #5d5b54;
}
.state {
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  padding: 60px 0;
}
.state.error .retry {
  margin-top: 12px;
  color: #fff;
  text-decoration: underline;
  cursor: pointer;
}
.empty {
  text-align: center;
  padding: 70px 0;
  color: rgba(255, 255, 255, 0.85);
}
.empty-icon {
  font-size: 42px;
  margin-bottom: 12px;
}
.empty-text {
  font-size: 15px;
}
.empty-sub {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}
.footer {
  text-align: center;
  padding: 24px 0 8px;
}
.footer p {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
  margin: 0;
}
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}
.sheet {
  width: 100%;
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 20px 18px 28px;
  box-sizing: border-box;
  animation: sheet-up 0.2s ease;
}
@keyframes sheet-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}
.sheet-title {
  font-size: 17px;
  font-weight: 700;
  color: #37352f;
}
.sheet-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #787671;
}
.sheet-input {
  width: 100%;
  height: 96px;
  margin-top: 14px;
  padding: 10px;
  box-sizing: border-box;
  border: 1px solid #dcdad5;
  border-radius: 10px;
  font-size: 14px;
  color: #37352f;
  resize: none;
  outline: none;
  font-family: inherit;
}
.sheet-input:focus {
  border-color: #5645d4;
}
.sheet-count {
  text-align: right;
  font-size: 11px;
  color: #9a978f;
  margin-top: 4px;
}
.sheet-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}
</style>
