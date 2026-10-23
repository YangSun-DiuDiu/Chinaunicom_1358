<template>
  <div class="h5-page">
    <div class="h5-nav">
      <el-button type="text" icon="el-icon-arrow-left" style="color:#fff" @click="goBack">返回</el-button>
    </div>
    <h3>🔎 我的预约</h3>
    <p class="tip">输入预约时填写的访客手机号，查询预约记录并在门卫处出示通行码</p>

    <div class="search-bar">
      <el-input v-model="phone" placeholder="请输入访客手机号" size="large" maxlength="11"
                @keyup.enter.native="search" clearable>
        <el-button slot="append" @click="search" :loading="loading">查询</el-button>
      </el-input>
    </div>

    <div v-if="list.length > 0" class="list-wrap">
      <div v-for="item in list" :key="item.appointmentId" class="appt-card">
        <div class="appt-head">
          <span class="appt-name">{{ item.visitorName }}</span>
          <span class="status-tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
        </div>
        <div class="info-grid">
          <div><span class="info-label">访客电话</span>{{ item.visitorPhone }}</div>
          <div v-if="item.visitorCompany"><span class="info-label">访客单位</span>{{ item.visitorCompany }}</div>
          <div><span class="info-label">被访人</span>{{ item.hostName }}</div>
          <div v-if="item.hostDept"><span class="info-label">被访部门</span>{{ item.hostDept }}</div>
          <div v-if="item.visitTime"><span class="info-label">来访时间</span>{{ item.visitTime }}</div>
        </div>
        <div v-if="item.passCode" class="appt-foot">
          <el-button type="primary" size="small" @click="showPass(item)">出示通行码</el-button>
        </div>
      </div>
    </div>

    <div v-else-if="searched && !loading" class="empty">
      未查询到该手机号的预约记录
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

const STATUS_MAP = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
  VISITING: '已到访',
  COMPLETED: '已完成'
}

export default {
  name: 'H5Appointments',
  data() {
    return { phone: '', loading: false, list: [], searched: false }
  },
  methods: {
    goBack() {
      if (this.$route.query.from && this.$route.query.from === 'login') {
        this.$router.push('/h5/login')
      } else {
        this.$router.back()
      }
    },
    search() {
      const p = (this.phone || '').trim()
      if (!p) {
        this.$message.warning('请输入访客手机号')
        return
      }
      this.loading = true
      this.searched = true
      request({ url: '/pass/list', method: 'get', params: { phone: p } }).then(res => {
        this.list = res.data || []
      }).catch(() => {
        this.list = []
        this.$message.error('查询失败，请稍后重试')
      }).finally(() => { this.loading = false })
    },
    showPass(item) {
      this.$router.push('/h5/visitor/pass?code=' + encodeURIComponent(item.passCode))
    },
    statusText(s) {
      return STATUS_MAP[s] || s
    },
    statusClass(s) {
      return ({ APPROVED: 'ok', VISITING: 'ok', PENDING: 'warn' })[s] || 'bad'
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:linear-gradient(135deg, #0A1530 0%, #1A2A52 60%, #5645D4 100%); padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 6px; }
.tip { color:rgba(255,255,255,0.75); font-size:13px; margin:0 0 16px; }
.search-bar >>> .el-input__inner { background:#fff; border-color:#C8C4BE; color:#37352F; }
.search-bar >>> .el-input-group__append { background:#5645D4; border-color:#5645D4; color:#fff; }
.list-wrap { margin-top:20px; }
.appt-card { background:#fff; border-radius:12px; padding:16px; margin-bottom:12px; box-shadow:rgba(15,15,15,0.08) 0 4px 12px 0; }
.appt-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; }
.appt-name { font-size:16px; font-weight:700; color:#37352F; }
.status-tag { font-size:12px; padding:2px 10px; border-radius:10px; }
.status-tag.ok { color:#1AAE39; background:rgba(26,174,57,0.1); }
.status-tag.warn { color:#E6A23C; background:rgba(230,162,60,0.1); }
.status-tag.bad { color:#E03131; background:rgba(224,49,49,0.1); }
.info-grid { font-size:13px; color:#37352F; }
.info-grid div { margin:4px 0; }
.info-label { color:#787671; font-size:12px; margin-right:8px; }
.appt-foot { margin-top:12px; text-align:right; }
.empty { margin-top:40px; text-align:center; color:rgba(255,255,255,0.8); font-size:14px; }
</style>
