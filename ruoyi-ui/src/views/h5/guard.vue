<template>
  <div class="h5-page">
    <div class="h5-nav"><el-button type="text" icon="el-icon-arrow-left" @click="stopScan">返回</el-button></div>
    <h3>🔒 保安通行核验</h3>

    <!-- 扫码区域 -->
    <div v-if="scanning" class="scanner-box">
      <div ref="scanner" class="scanner-view"></div>
      <div class="scanner-tip">将访客通行码对准框内扫码</div>
      <el-button size="small" type="info" plain @click="stopScan">关闭扫码</el-button>
    </div>

    <!-- 按钮区域 -->
    <div v-if="!scanning" class="action-area">
      <el-button type="primary" size="large" icon="el-icon-camera-solid" long @click="startScan">
        打开摄像头扫码
      </el-button>
      <div class="or-divider">或</div>
      <el-input v-model="code" placeholder="手动输入通行码" size="large" clearable @keyup.enter.native="verifyManual">
        <el-button slot="append" @click="verifyManual" :loading="loading">核验</el-button>
      </el-input>
    </div>

    <!-- 核验结果 -->
    <div v-if="result" class="verify-result" :class="result.valid?'valid':'invalid'">
      <div class="verify-title">{{ result.valid ? '✅ 通行有效 · 已到访' : '❌ 无效通行码' }}</div>
      <div class="verify-grid" v-if="result.valid">
        <div class="verify-row"><span>访客</span><b>{{ result.visitorName }}</b></div>
        <div class="verify-row"><span>电话</span><b>{{ result.visitorPhone }}</b></div>
        <div class="verify-row"><span>被访人</span><b>{{ result.hostName }}</b></div>
        <div class="verify-row" v-if="result.hostDept"><span>部门</span><b>{{ result.hostDept }}</b></div>
      </div>
      <div class="verify-time">{{ resultTime }}</div>
    </div>
  </div>
</template>

<script>
import { Html5Qrcode } from 'html5-qrcode'
import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'

export default {
  data() {
    return { code: '', loading: false, scanning: false, scanner: null, result: null, resultTime: '' }
  },
  beforeDestroy() { this.stopScan() },
  methods: {
    async startScan() {
      this.result = null
      try {
        this.scanner = new Html5Qrcode('scanner')
        this.scanning = true
        await this.$nextTick()
        await this.scanner.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 250 } },
          (decodedText) => {
            this.code = decodedText
            this.stopScan()
            this.verifyCode(decodedText)
          },
          () => {} // ignore scan errors
        )
      } catch (e) {
        this.scanning = false
        this.$message.error('无法打开摄像头，请检查权限或手动输入')
      }
    },
    async stopScan() {
      if (this.scanner) {
        try { await this.scanner.stop() } catch (e) {}
        try { this.scanner.clear() } catch (e) {}
        this.scanner = null
      }
      this.scanning = false
    },
    verifyManual() {
      if (!this.code) return
      this.verifyCode(this.code.trim())
    },
    verifyCode(code) {
      this.loading = true
      this.code = code
      request({ url: '/pass/' + encodeURIComponent(code), method: 'get' }).then(res => {
        const d = res.data || res
        if (d.visitorName) {
          this.result = { valid: true, ...d }
        } else {
          this.result = { valid: false }
        }
        this.resultTime = parseTime(new Date())
      }).catch(() => {
        this.result = { valid: false }
        this.resultTime = parseTime(new Date())
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }

.scanner-box { text-align:center; margin-bottom:20px; }
.scanner-view { width:100%; max-width:320px; margin:0 auto 12px; border-radius:10px; overflow:hidden; }
.scanner-tip { font-size:13px; color:#8899aa; margin-bottom:12px; }

.action-area { margin-bottom:20px; }
.or-divider { text-align:center; padding:16px 0; color:#556677; font-size:13px; }

.verify-result { margin-top:16px; border-radius:10px; padding:24px; text-align:center; }
.verify-result.valid { background:rgba(0,230,118,0.1); border:1px solid rgba(0,230,118,0.3); }
.verify-result.invalid { background:rgba(255,82,82,0.1); border:1px solid rgba(255,82,82,0.3); }
.verify-title { font-size:20px; font-weight:700; margin-bottom:16px; }
.valid .verify-title { color:#00e676; }
.invalid .verify-title { color:#ff5252; }
.verify-grid { text-align:left; }
.verify-row { display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid rgba(0,180,255,0.06); font-size:14px; }
.verify-row span { color:#8899aa; }
.verify-row b { color:#fff; }
.verify-time { margin-top:16px; color:#556677; font-size:12px; }

.h5-page >>> .el-input__inner { background:rgba(0,0,0,0.3); border-color:rgba(0,180,255,0.2); color:#fff; }
</style>
