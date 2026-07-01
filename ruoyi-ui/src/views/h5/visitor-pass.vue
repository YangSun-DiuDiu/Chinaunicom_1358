<template>
  <div class="h5-page">
    <div class="h5-nav"><el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button></div>
    <h3>🔍 查询通行码</h3>
    <el-input v-model="code" placeholder="请输入通行码" size="large" @keyup.enter.native="search" clearable>
      <el-button slot="append" @click="search" :loading="loading">查询</el-button>
    </el-input>
    <div v-if="result" class="result-card" :class="result.status==='有效'?'success':'fail'">
      <div class="result-title">{{ result.status === '有效' ? '✅ 通行有效' : '❌ ' + result.status }}</div>
      <div class="info-grid">
        <div><span class="info-label">访客</span> {{ result.visitorName }}</div>
        <div><span class="info-label">被访人</span> {{ result.hostName }}</div>
        <div v-if="result.hostDept"><span class="info-label">部门</span> {{ result.hostDept }}</div>
        <div v-if="result.visitTime"><span class="info-label">时间</span> {{ result.visitTime }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  data() { return { code: '', loading: false, result: null } },
  methods: {
    search() {
      if (!this.code) return
      this.loading = true
      request({ url:'/pass/'+encodeURIComponent(this.code), method:'get' }).then(res => {
        const d = res.data || res
        this.result = { ...d, status: d.msg&&d.msg.includes('无效')?'无效':'有效' }
      }).catch(() => { this.result = { status:'无效' } }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.result-card { margin-top:24px; border-radius:8px; padding:20px; text-align:center; }
.result-card.success { background:rgba(0,230,118,0.1); border:1px solid rgba(0,230,118,0.3); }
.result-card.fail { background:rgba(255,82,82,0.1); border:1px solid rgba(255,82,82,0.3); }
.result-title { font-size:18px; font-weight:700; margin-bottom:12px; }
.result-card.success .result-title { color:#00e676; }
.result-card.fail .result-title { color:#ff5252; }
.info-grid { text-align:left; }
.info-grid div { margin:6px 0; font-size:14px; color:#bcc9d4; }
.info-label { color:#8899aa; font-size:12px; margin-right:8px; }
.h5-page >>> .el-input__inner { background:rgba(0,0,0,0.3); border-color:rgba(0,180,255,0.2); color:#fff; }
</style>
