<template>
  <div class="repair-complete-page">
    <!-- Token 输入模式 -->
    <div class="card" v-if="!token">
      <div class="header-icon">🔑</div>
      <h2>设备维修确认</h2>
      <p style="color:#666;text-align:center;margin-bottom:16px">请输入短信中的设备登录码</p>
      <el-input v-model="tokenInput" placeholder="请输入设备登录码" clearable size="medium" style="margin-bottom:12px" />
      <el-button type="primary" size="medium" :disabled="!tokenInput" @click="submitToken" style="width:100%">提 交</el-button>
    </div>

    <!-- 原有逻辑: 有 token 参数时加载工单 -->
    <div v-else>
      <div class="header">
        <div class="header-icon">🔧</div>
        <h1>设备维修确认</h1>
        <p>请确认维修结果并上传凭证</p>
      </div>

      <!-- 加载中 -->
      <div class="card" v-if="loading">
        <div class="loading-spin">⏳</div>
        <p style="text-align:center;color:#666">加载工单信息...</p>
      </div>

      <!-- 错误 -->
      <div class="card" v-else-if="error">
        <div class="error-icon">❌</div>
        <h3>{{ error }}</h3>
      </div>

      <!-- 已完成 -->
      <div class="card" v-else-if="completed">
        <div class="success-icon">✅</div>
        <h2>维修确认成功</h2>
        <p style="color:#666;text-align:center">您的维修结果已上报，感谢！</p>
      </div>

      <!-- 工单详情 + 提交 -->
      <div class="card" v-else>
        <div class="info-row"><span>设备名称</span><strong>{{ repair.deviceName }}</strong></div>
        <div class="info-row"><span>IP地址</span><strong>{{ repair.deviceIp }}</strong></div>
        <div class="info-row"><span>责任人</span><strong>{{ repair.currentResponsible }}</strong></div>
        <div class="info-row"><span>状态</span><strong class="status-tag">{{ statusLabel }}</strong></div>

        <div v-if="canComplete" style="margin-top:16px">

          <!-- 维修前照片 -->
          <div class="section">
            <div class="form-label">📷 维修前照片</div>
            <div class="photo-area" @click="$refs.beforeInput.click()">
              <img v-if="photoBefore" :src="photoBefore" class="photo-preview" @error="onImgError" />
              <div v-else class="photo-placeholder">
                <i style="font-size:32px;color:#ccc;display:block;margin-bottom:4px">+</i>
                <span style="font-size:11px;color:#999">点击上传维修前照片</span>
              </div>
            </div>
            <input ref="beforeInput" type="file" accept="image/*" style="display:none" @change="uploadPhoto('before', $event)" />
            <span style="font-size:11px;color:#4caf50" v-if="photoBefore">✅ 已上传</span>
          </div>

          <!-- 维修后照片 -->
          <div class="section">
            <div class="form-label">📸 维修完成照片</div>
            <div class="photo-area" @click="$refs.afterInput.click()">
              <img v-if="photoAfter" :src="photoAfter" class="photo-preview" @error="onImgError" />
              <div v-else class="photo-placeholder">
                <i style="font-size:32px;color:#ccc;display:block;margin-bottom:4px">+</i>
                <span style="font-size:11px;color:#999">点击上传维修后照片</span>
              </div>
            </div>
            <input ref="afterInput" type="file" accept="image/*" style="display:none" @change="uploadPhoto('after', $event)" />
            <span style="font-size:11px;color:#4caf50" v-if="photoAfter">✅ 已上传</span>
          </div>

          <!-- 是否使用配件 -->
          <div class="section">
            <div class="form-label">🔩 是否使用了配件？</div>
            <div class="radio-group">
              <label class="radio-item" :class="{active: useParts}">
                <input type="radio" v-model="useParts" :value="true" /> 是，使用了配件
              </label>
              <label class="radio-item" :class="{active: !useParts}">
                <input type="radio" v-model="useParts" :value="false" /> 否，无需配件
              </label>
            </div>
            <input v-if="useParts" v-model="partsDesc" class="result-input" placeholder="请输入使用的配件名称，多个用逗号分隔" style="margin-top:8px" />
          </div>

          <!-- 维修结果说明 -->
          <div class="section">
            <div class="form-label">📝 维修结果说明</div>
            <textarea v-model="result" placeholder="请描述维修过程和结果" rows="3" class="result-input" maxlength="500"></textarea>
          </div>

          <button class="btn" :disabled="submitting" @click="submitComplete">
            {{ submitting ? '提交中...' : '✅ 确认维修完成' }}
          </button>
        </div>
      </div>

      <div class="footer">智慧园区管理系统</div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

const api = axios.create({ baseURL: '/prod-api', timeout: 30000 })

export default {
  name: 'RepairComplete',
  data() {
    return {
      token: this.$route.query.token || '',
      tokenInput: '',
      loading: true, error: '', completed: false, submitting: false,
      result: '', repair: {},
      photoBefore: '', photoAfter: '',
      useParts: false, partsDesc: ''
    }
  },
  computed: {
    baseUrl() { return '' },  // 图片直接Nginx服务，无需/prod-api前缀
    canComplete() { return this.repair.status === 'PENDING' || this.repair.status === 'ASSIGNED' || this.repair.status === 'ACCEPTED' },
    statusLabel() {
      const m = { PENDING: '待处理', ASSIGNED: '已转派', ACCEPTED: '已接收', REJECTED: '已拒绝', COMPLETED: '已完成' }
      return m[this.repair.status] || this.repair.status
    }
  },
  watch: {
    '$route.query.token'(val) { if (val) { this.token = val; this.loadRepair() } }
  },
  mounted() { if (this.token) { this.loadRepair() } },
  methods: {
    submitToken() {
      if (this.tokenInput) {
        this.$router.replace({ path: '/repair-complete', query: { token: this.tokenInput } })
      }
    },
    loadRepair() {
      if (!this.token) { this.error = '无效链接：缺少token参数'; this.loading = false; return }
      api.get('/device/repair/public/detail?token=' + this.token).then(res => {
        const d = res.data
        if (d.code === 200) {
          this.repair = d.data
          if (d.data.status === 'COMPLETED') { this.completed = true }
        } else { this.error = d.msg || '工单不存在' }
      }).catch(() => { this.error = '网络错误，请稍后重试' }).finally(() => { this.loading = false })
    },
    uploadPhoto(type, e) {
      const file = e.target.files[0]
      if (!file) return
      if (file.size > 5 * 1024 * 1024) { alert('图片不能超过5MB'); e.target.value = ''; return }
      const fd = new FormData(); fd.append('file', file)
      // 强制清除全局Content-Type，让浏览器自动设置multipart boundary
      const config = {
        timeout: 30000,
        transformRequest: [(data, headers) => { delete headers['Content-Type']; return data; }]
      }
      api.post('/device/repair/public/upload', fd, config).then(res => {
        const d = res.data
        if (d.code === 200) {
          const url = d.url || d.data || ''
          if (!url) { alert('上传成功但未返回图片地址'); return }
          const fullUrl = url.startsWith('http') ? url : (this.baseUrl + url)
          if (type === 'before') { this.photoBefore = fullUrl }
          else { this.photoAfter = fullUrl }
        } else {
          alert('上传失败: ' + (d.msg || '未知错误'))
        }
      }).catch(err => {
        const msg = err.response ? (err.response.status + ' ' + err.response.statusText) : (err.message || '网络错误')
        alert('上传失败: ' + msg)
      })
      e.target.value = ''
    },
    onImgError(e) {
      console.error('Image load failed:', e.target.src)
    },
    submitComplete() {
      this.submitting = true
      api.post('/device/repair/public/complete', {
        token: this.token,
        result: this.result,
        photoBefore: this.photoBefore,
        photoAfter: this.photoAfter,
        hasParts: this.useParts ? '1' : '0',
        partsDesc: this.partsDesc
      }).then(res => {
        if (res.data.code === 200) { this.completed = true }
        else { alert(res.data.msg || '提交失败') }
      }).catch(() => { alert('网络错误') }).finally(() => { this.submitting = false })
    }
  }
}
</script>

<style scoped>
* { box-sizing: border-box; margin: 0; padding: 0; }
.repair-complete-page { min-height: 100vh; background: linear-gradient(135deg,#fa8c16,#ffc069); font-family: -apple-system,sans-serif; padding-bottom: 40px; }
.header { text-align: center; padding: 40px 20px 20px; color: #fff; }
.header-icon { font-size: 52px; }
.header h1 { font-size: 22px; margin: 8px 0; }
.header p { font-size: 13px; opacity: 0.85; }
.card { background: #fff; margin: 0 16px; border-radius: 12px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,.1); }
.info-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f5f5f5; font-size: 15px; }
.info-row span { color: #999; }
.info-row strong { color: #333; }
.status-tag { color: #fa8c16 !important; }
.section { margin-bottom: 14px; }
.form-label { font-size: 13px; color: #666; margin-bottom: 6px; font-weight: 500; }
.photo-area { width: 100%; height: 150px; border: 1px dashed #ddd; border-radius: 8px; overflow: hidden; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.photo-placeholder { text-align: center; color: #ccc; }
.photo-preview { width: 100%; height: 100%; object-fit: cover; }
.radio-group { display: flex; gap: 12px; }
.radio-item { flex: 1; padding: 8px 12px; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 13px; cursor: pointer; text-align: center; color: #666; }
.radio-item.active { border-color: #fa8c16; background: #fff7e6; color: #fa8c16; font-weight: 500; }
.radio-item input { display: none; }
.result-input { width: 100%; border: 1px solid #e0e0e0; border-radius: 8px; padding: 10px; font-size: 14px; resize: vertical; outline: none; }
.result-input:focus { border-color: #fa8c16; }
.btn { width: 100%; height: 46px; border: none; border-radius: 23px; font-size: 17px; font-weight: 600; color: #fff; background: linear-gradient(135deg,#fa8c16,#ffc069); cursor: pointer; margin-top: 8px; }
.btn:disabled { background: #ccc; cursor: not-allowed; }
.loading-spin { font-size: 48px; text-align: center; animation: spin 1s infinite linear; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.error-icon, .success-icon { font-size: 56px; text-align: center; margin: 10px 0; }
h3 { text-align: center; color: #333; margin: 12px 0; }
h2 { text-align: center; color: #333; margin: 8px 0; }
.footer { text-align: center; padding: 20px; color: rgba(255,255,255,.6); font-size: 12px; }
</style>
