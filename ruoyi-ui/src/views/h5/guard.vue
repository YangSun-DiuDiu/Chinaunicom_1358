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
        // ① 先用原生 getUserMedia 探测摄像头（绕过 html5-qrcode 的异常包装，拿真实错误名）
        const stream = await this.preflightCamera()
        // 预检成功：释放探测流，交给 html5-qrcode 重新获取（权限已记住，不会再弹框）
        stream.getTracks().forEach(t => t.stop())
        // ② 渲染容器后再构造扫码器
        this.scanning = true
        await this.$nextTick()
        this.scanner = new Html5Qrcode('scanner')
        // 优先后置摄像头（environment），失败自动降级前置摄像头
        const onSuccess = (decodedText) => {
          this.code = decodedText
          this.stopScan()
          this.verifyCode(decodedText)
        }
        try {
          await this.scanner.start(
            { facingMode: 'environment' },
            { fps: 10, qrbox: { width: 250, height: 250 } },
            onSuccess,
            () => {} // ignore scan errors
          )
        } catch (err1) {
          try {
            await this.scanner.start(
              { facingMode: 'user' },
              { fps: 10, qrbox: { width: 250, height: 250 } },
              onSuccess,
              () => {}
            )
          } catch (err2) {
            this.scanning = false
            this.showCameraError(err2)
          }
        }
      } catch (e) {
        this.scanning = false
        this.showCameraError(e)
      }
    },
    async preflightCamera() {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        const err = new Error('浏览器未提供摄像头接口（mediaDevices 不可用）')
        err.name = 'NoMediaDevices'
        throw err
      }
      try {
        return await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      } catch (err1) {
        // 后置摄像头失败 → 降级前置
        try {
          return await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
        } catch (err2) {
          this.reportCameraError(err2)
          throw err2
        }
      }
    },
    reportCameraError(e) {
      // 上报真实错误到服务器日志，用于远程诊断（应用内浏览器错误信息丢失时靠它定位）
      try {
        const d = {
          name: (e && e.name) || '',
          message: (e && e.message) || '',
          stack: ((e && e.stack) || '').slice(0, 500),
          ua: navigator.userAgent
        }
        fetch('/prod-api/visitor/h5/camlog', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(d)
        }).catch(() => {})
      } catch (ignore) {}
    },
    showCameraError(e) {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        this.$message.error('当前环境不支持摄像头调用（浏览器未提供摄像头接口）：请改用系统浏览器（Chrome/Safari）打开本站')
        return
      }
      const name = (e && e.name) || ''
      const msg = (e && e.message) || ''
      const msgMap = {
        NoMediaDevices: '当前环境不支持摄像头调用（浏览器未提供摄像头接口）：请改用系统浏览器（Chrome/Safari）打开本站',
        NotAllowedError: '浏览器拒绝了摄像头权限：请点击地址栏右侧摄像头图标，将本站权限设为「允许」后重试；或在手机设置中允许浏览器使用相机',
        NotFoundError: '未检测到可用摄像头：请确认设备已接入摄像头，或改用手动输入通行码',
        NotReadableError: '摄像头正被其他应用占用：请关闭其他占用相机的应用（如微信、钉钉）后重试',
        NotSupportedError: '当前环境不支持摄像头调用：请使用手机系统浏览器（Chrome/Safari）打开本站重试',
        SecurityError: '非安全环境无法调用摄像头：请通过 https:// 地址访问本站',
        TypeError: '浏览器拒绝摄像头调用（可能是应用内浏览器限制）：请改用系统浏览器（Chrome/Safari）打开'
      }
      if (msgMap[name]) {
        this.$message.error(msgMap[name])
      } else if (msg && /not found|element/i.test(msg)) {
        this.$message.error('页面组件未就绪：请刷新页面后重试打开摄像头')
      } else {
        this.$message.error('无法打开摄像头（' + (name || msg || '未知错误') + '）')
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
          // 到访确认（写操作走独立 POST；GET 为纯读）
          request({ url: '/pass/' + encodeURIComponent(code) + '/checkin', method: 'post' }).then(res2 => {
            const d2 = res2.data || res2
            if (d2.visitorName) {
              this.result = { valid: true, ...d2 }
            }
          }).catch(() => {})
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
.h5-page { min-height:100vh; background:linear-gradient(135deg, #0A1530 0%, #1A2A52 60%, #5645D4 100%); padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }

.scanner-box { text-align:center; margin-bottom:20px; }
.scanner-view { width:100%; max-width:320px; margin:0 auto 12px; border-radius:12px; overflow:hidden; background:#fff; }
.scanner-tip { font-size:13px; color:rgba(255,255,255,0.75); margin-bottom:12px; }

.action-area { margin-bottom:20px; }
.or-divider { text-align:center; padding:16px 0; color:rgba(255,255,255,0.6); font-size:13px; }

.verify-result { margin-top:16px; border-radius:12px; padding:24px; text-align:center; background:#fff; box-shadow: rgba(15,15,15,0.08) 0px 4px 12px 0px; }
.verify-result.valid { border:1px solid rgba(26,174,57,0.4); }
.verify-result.invalid { border:1px solid rgba(224,49,49,0.4); }
.verify-title { font-size:20px; font-weight:700; margin-bottom:16px; }
.valid .verify-title { color:#1AAE39; }
.invalid .verify-title { color:#E03131; }
.verify-grid { text-align:left; }
.verify-row { display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid #EDE9E4; font-size:14px; color:#37352F; }
.verify-row span { color:#787671; }
.verify-row b { color:#37352F; }
.verify-time { margin-top:16px; color:#A4A097; font-size:12px; }

.h5-page >>> .el-input__inner { background:#fff; border-color:#C8C4BE; color:#37352F; }
.h5-page >>> .el-input-group__append { background:#5645D4; border-color:#5645D4; color:#fff; }
.h5-page >>> .el-button--primary { background:#5645D4; border-color:#5645D4; }
</style>
