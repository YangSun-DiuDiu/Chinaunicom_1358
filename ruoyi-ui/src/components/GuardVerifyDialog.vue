<template>
  <el-dialog title="访客二维码核验" :visible.sync="visible" width="420px" :close-on-click-modal="false" @closed="onClosed">
    <div v-if="scanning" style="text-align:center;">
      <div id="scanner" ref="scanner" class="verify-scanner"></div>
      <p style="color:#999;font-size:13px;margin:8px 0;">将访客通行码对准框内扫码</p>
      <el-button size="small" type="info" plain @click="stopScan">关闭扫码</el-button>
    </div>
    <div v-else>
      <div style="text-align:center;color:#909399;padding:0 0 10px;font-size:12px;">进场扫码一次 → 已到访；离场再扫一次 → 自动完成</div>
      <el-button type="primary" size="medium" icon="el-icon-camera-solid" style="width:100%" @click="startScan">打开摄像头扫码</el-button>
      <div style="text-align:center;color:#999;padding:12px 0;font-size:13px;">或</div>
      <el-input v-model="code" placeholder="手动输入通行码（扫码枪扫描后自动核验）" size="medium" clearable @keyup.enter.native="verifyManual">
        <el-button slot="append" @click="verifyManual" :loading="loadingVerify">核验</el-button>
      </el-input>
    </div>
    <div v-if="result" class="verify-result" :class="result.valid?'valid':'invalid'">
      <div class="verify-title">{{ resultTitle }}</div>
      <div class="verify-grid" v-if="result.valid">
        <div class="verify-row"><span>访客</span><b>{{ result.visitorName }}</b></div>
        <div class="verify-row"><span>电话</span><b>{{ result.visitorPhone }}</b></div>
        <div class="verify-row"><span>被访人</span><b>{{ result.hostName }}</b></div>
        <div class="verify-row" v-if="result.hostDept"><span>部门</span><b>{{ result.hostDept }}</b></div>
        <div class="verify-row"><span>通行码</span><b>{{ result.passCode }}</b></div>
      </div>
      <div class="verify-time">{{ resultTime }}</div>
    </div>
  </el-dialog>
</template>

<script>
import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'
import { preflightCamera, startScanner, stopScanner, getCameraErrorMessage, releaseStream } from '@/utils/guardScan'

/**
 * 访客二维码核验弹窗（PC 端深组件，单一实现）
 *
 * 能力：摄像头扫码（html5-qrcode）+ 手动输入通行码 + 核验结果卡。
 * 核验流程：GET /pass/{code} 纯读判定 → POST /pass/{code}/checkin 状态机变更（APPROVED→VISITING）。
 * 事件：@refresh —— 核验成功或 checkin 失败后通知父组件刷新列表（状态可能已变化）。
 * 修复要点（与 H5 guard.vue 共用 guardScan）：
 *   - 扫码容器必须带 id="scanner"（Html5Qrcode 按 id 取元素，此前缺 id 导致"无法打开摄像头（未知错误）"）
 *   - 核验按钮 loading 绑定 loadingVerify（此前误绑列表 loading）
 *   - checkin 失败不再静默：错误提示 + 通知刷新，避免"显示已到访但实际状态未变"
 *   - 摄像头预检期间若关闭弹窗，立即释放媒体流，避免摄像头常亮
 */
export default {
  name: 'GuardVerifyDialog',
  props: {
    visible: { type: Boolean, default: false }
  },
  data() {
    return { code: '', loadingVerify: false, scanning: false, scanner: null, result: null, resultTime: '' }
  },
  watch: {
    visible(val) {
      if (val) {
        this.result = null
        this.code = ''
      }
    }
  },
  computed: {
    // 核验结果卡标题：按返回状态区分入场/离场/无效
    resultTitle() {
      if (!this.result) return ''
      if (!this.result.valid) return '❌ 无效通行码'
      return this.result.status === 'COMPLETED' ? '✅ 核验完成 · 访客已离开' : '✅ 通行有效 · 已到访'
    }
  },
  beforeDestroy() { this.stopScan() },
  methods: {
    async startScan() {
      this.result = null
      let stream = null
      try {
        // ① 预检摄像头（绕过 html5-qrcode 异常包装，拿真实错误名）
        stream = await preflightCamera()
        // 预检期间用户若已关闭弹窗，立即释放媒体流，避免摄像头常亮且无法停止
        if (!this.visible) {
          releaseStream(stream)
          return
        }
        // 预检成功：释放探测流，交给 html5-qrcode 重新获取（权限已记住，不会再弹框）
        releaseStream(stream)
        // ② 渲染容器后再启动扫码器
        this.scanning = true
        await this.$nextTick()
        this.scanner = await startScanner('scanner', {
          onDecoded: (decodedText) => {
            this.code = decodedText
            this.stopScan()
            this.verifyCode(decodedText)
          },
          onFail: (err) => {
            this.scanning = false
            this.$message.error(getCameraErrorMessage(err))
          }
        })
        if (!this.scanner) {
          this.scanning = false
        }
      } catch (e) {
        releaseStream(stream)
        this.scanning = false
        this.$message.error(getCameraErrorMessage(e))
      }
    },
    async stopScan() {
      this.scanner = await stopScanner(this.scanner)
      this.scanning = false
    },
    verifyManual() {
      if (!this.code) return
      this.verifyCode(this.code.trim())
    },
    verifyCode(code) {
      this.loadingVerify = true
      this.code = code
      request({ url: '/pass/' + encodeURIComponent(code), method: 'get' }).then(res => {
        const d = res.data || res
        if (d.visitorName) {
          // 已完成码：幂等提示，不再触发写操作
          if (d.status === 'COMPLETED') {
            this.result = { valid: true, ...d }
            this.$modal.msgWarning('该访客已完成拜访，无需重复核验')
            return
          }
          this.result = { valid: true, ...d }
          // 到访确认 / 离场核验（写操作走独立 POST；GET 为纯读），按当前状态自动分派：
          // APPROVED → VISITING（已到访）；VISITING → COMPLETED（已离开）
          request({ url: '/pass/' + encodeURIComponent(code) + '/checkin', method: 'post' }).then(res2 => {
            const d2 = res2.data || res2
            if (d2.visitorName) {
              this.result = { valid: true, ...d2 }
              if (d2.status === 'COMPLETED') {
                this.$modal.msgSuccess('核验完成，访客已离开，拜访流程已闭环')
              } else {
                this.$modal.msgSuccess('核验通过，已自动变更状态为「已到访」')
              }
              this.$emit('refresh')
            }
          }).catch(() => {
            // 状态机拒绝（重复核验/已完成等）或网络异常：状态未变，必须提示并让列表回到真实状态
            this.$modal.msgError('到访状态变更失败，请以列表中的实际状态为准')
            this.$emit('refresh')
          })
        } else {
          this.result = { valid: false }
        }
        this.resultTime = parseTime(new Date())
      }).catch(() => {
        this.result = { valid: false }
        this.resultTime = parseTime(new Date())
      }).finally(() => { this.loadingVerify = false })
    },
    onClosed() {
      this.stopScan()
      this.result = null
      this.code = ''
    }
  }
}
</script>

<style scoped>
.verify-scanner { width:100%; max-width:320px; margin:0 auto 12px; border-radius:12px; overflow:hidden; background:#fff; }
.verify-result { margin-top:16px; border-radius:12px; padding:20px; text-align:center; background:#fff; border:1px solid #E4E7ED; }
.verify-result.valid { border:1px solid rgba(26,174,57,0.4); }
.verify-result.invalid { border:1px solid rgba(224,49,49,0.4); }
.verify-title { font-size:18px; font-weight:700; margin-bottom:14px; }
.valid .verify-title { color:#1AAE39; }
.invalid .verify-title { color:#E03131; }
.verify-grid { text-align:left; }
.verify-row { display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid #F0F2F5; font-size:14px; color:#303133; }
.verify-row span { color:#909399; }
.verify-time { margin-top:14px; color:#C0C4CC; font-size:12px; }
</style>
