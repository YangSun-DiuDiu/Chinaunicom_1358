<template>
  <div class="h5-page">
    <div class="h5-nav">
      <el-button type="text" icon="el-icon-arrow-left" style="color:#fff" @click="goBack">返回</el-button>
    </div>
    <h3 v-if="!result">🔍 通行码</h3>
    <template v-if="!result">
      <el-input v-model="code" placeholder="请输入通行码或从短信链接进入" size="large" @keyup.enter.native="search" clearable>
        <el-button slot="append" @click="search" :loading="loading">查询</el-button>
      </el-input>
      <p class="tip">门卫出示：确认二维码为有效通行码后，由保安扫码或输入通行码核验</p>
    </template>

    <!-- 通行码出示大屏 -->
    <div v-if="result" class="pass-screen" :class="result.status==='有效'?'success':'fail'">
      <div class="pass-title">{{ result.status === '有效' ? '✅ 通行有效' : '❌ ' + result.status }}</div>

      <div v-if="result.status==='有效'" class="qr-wrap">
        <div ref="qrBox" class="qr-box"></div>
        <div class="qr-hint">请保安扫描此二维码进行核验</div>
      </div>

      <div class="info-grid">
        <div><span class="info-label">访客</span> {{ result.visitorName }}</div>
        <div v-if="result.visitorPhone"><span class="info-label">电话</span> {{ result.visitorPhone }}</div>
        <div v-if="result.visitorCompany"><span class="info-label">单位</span> {{ result.visitorCompany }}</div>
        <div><span class="info-label">被访人</span> {{ result.hostName }}</div>
        <div v-if="result.hostDept"><span class="info-label">部门</span> {{ result.hostDept }}</div>
        <div v-if="result.visitTime"><span class="info-label">时间</span> {{ result.visitTime }}</div>
      </div>

      <div class="code-row">
        <span class="info-label">通行码</span>
        <span class="code-text">{{ code }}</span>
        <el-button type="text" size="mini" @click="copyCode">复制</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import QRCode from 'qrcodejs2'

export default {
  data() { return { code: '', loading: false, result: null } },
  created() {
    const q = this.$route.query.code
    if (q) {
      this.code = q
      this.search()
    }
  },
  methods: {
    goBack() {
      if (this.$route.query.from === 'appointments') {
        this.$router.push('/h5/appointments')
      } else {
        this.$router.back()
      }
    },
    search() {
      if (!this.code) return
      this.loading = true
      request({ url: '/pass/' + encodeURIComponent(this.code), method: 'get' }).then(res => {
        const d = res.data || res
        this.result = { ...d, status: d.msg && d.msg.includes('无效') ? '无效' : '有效' }
        if (this.result.status === '有效') {
          this.$nextTick(this.renderQr)
        }
      }).catch(() => { this.result = { status: '无效' } }).finally(() => { this.loading = false })
    },
    renderQr() {
      if (!this.$refs.qrBox) return
      this.$refs.qrBox.innerHTML = ''
      try {
        new QRCode(this.$refs.qrBox, {
          text: this.code,
          width: 220,
          height: 220,
          colorDark: '#0A1530',
          colorLight: '#ffffff',
          correctLevel: QRCode.CorrectLevel.M
        })
      } catch (e) {
        this.$refs.qrBox.innerHTML = '<span style="color:#888;font-size:13px">二维码生成失败，请使用下方通行码</span>'
      }
    },
    copyCode() {
      const input = document.createElement('textarea')
      input.value = this.code
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$message.success('通行码已复制')
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:linear-gradient(135deg, #0A1530 0%, #1A2A52 60%, #5645D4 100%); padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.tip { color:rgba(255,255,255,0.75); font-size:13px; margin-top:12px; }
.h5-page >>> .el-input__inner { background:#fff; border-color:#C8C4BE; color:#37352F; }
.h5-page >>> .el-input-group__append { background:#5645D4; border-color:#5645D4; color:#fff; }

/* 出示大屏 */
.pass-screen { margin-top:12px; border-radius:16px; padding:24px 20px; text-align:center; background:#fff; box-shadow:rgba(15,15,15,0.1) 0 6px 20px 0; }
.pass-screen.success { border:1px solid rgba(26,174,57,0.4); }
.pass-screen.fail { border:1px solid rgba(224,49,49,0.4); }
.pass-title { font-size:20px; font-weight:700; margin-bottom:16px; }
.pass-screen.success .pass-title { color:#1AAE39; }
.pass-screen.fail .pass-title { color:#E03131; }

.qr-wrap { display:flex; flex-direction:column; align-items:center; margin-bottom:16px; }
.qr-box { padding:12px; background:#fff; border:1px dashed #C8C4BE; border-radius:8px; display:inline-block; }
.qr-hint { margin-top:10px; font-size:13px; color:#787671; }

.info-grid { text-align:left; }
.info-grid div { margin:6px 0; font-size:14px; color:#37352F; }
.info-label { color:#787671; font-size:12px; margin-right:8px; }
.code-row { margin-top:14px; padding-top:12px; border-top:1px solid #EFEEEA; font-size:14px; color:#37352F; }
.code-text { font-family:Consolas,Menlo,monospace; font-weight:700; letter-spacing:2px; color:#0A1530; }
</style>
