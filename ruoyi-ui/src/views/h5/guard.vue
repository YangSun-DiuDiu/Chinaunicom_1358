<template>
  <div class="h5-page">
    <div class="h5-nav"><el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button></div>
    <h3>🔒 保安通行核验</h3>
    <el-input v-model="code" placeholder="请输入或扫描通行码" size="large" clearable @keyup.enter.native="verify">
      <el-button slot="append" @click="verify" :loading="loading">核验</el-button>
    </el-input>
    <div v-if="result" class="verify-result" :class="result.valid?'valid':'invalid'">
      <div class="verify-title">{{ result.valid ? '✅ 通行有效' : '❌ 无效' }}</div>
      <div class="verify-grid" v-if="result.valid">
        <div class="verify-row"><span>访客</span><b>{{ result.visitorName }}</b></div>
        <div class="verify-row"><span>电话</span><b>{{ result.visitorPhone }}</b></div>
        <div class="verify-row"><span>被访人</span><b>{{ result.hostName }}</b></div>
        <div class="verify-row" v-if="result.hostDept"><span>部门</span><b>{{ result.hostDept }}</b></div>
        <div class="verify-row" v-if="result.visitTime"><span>时间</span><b>{{ result.visitTime }}</b></div>
      </div>
      <div class="verify-time">核验时间：{{ now }}</div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'

export default {
  data() {
    return { code: '', loading: false, result: null, now: '' }
  },
  methods: {
    verify() {
      if (!this.code) return
      this.loading = true
      request({ url: '/visitor/verify/' + encodeURIComponent(this.code), method: 'get' }).then(res => {
        const d = res.data || res
        this.result = { valid: d.status && d.status !== '无效', ...d }
        this.now = parseTime(new Date())
      }).catch(() => {
        this.result = { valid: false }
        this.now = parseTime(new Date())
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.verify-result { margin-top:24px; border-radius:10px; padding:24px; text-align:center; }
.verify-result.valid { background:rgba(0,230,118,0.1); border:1px solid rgba(0,230,118,0.3); }
.verify-result.invalid { background:rgba(255,82,82,0.1); border:1px solid rgba(255,82,82,0.3); }
.verify-title { font-size:20px; font-weight:700; margin-bottom:16px; }
.valid .verify-title { color:#00e676; }
.invalid .verify-title { color:#ff5252; }
.verify-grid { text-align:left; }
.verify-row { display:flex; justify-content:space-between; padding:6px 0; border-bottom:1px solid rgba(0,180,255,0.06); font-size:14px; }
.verify-row span { color:#8899aa; }
.verify-row b { color:#fff; }
.verify-time { margin-top:16px; color:#556677; font-size:12px; }
.h5-page >>> .el-input__inner { background:rgba(0,0,0,0.3); border-color:rgba(0,180,255,0.2); color:#fff; }
</style>
