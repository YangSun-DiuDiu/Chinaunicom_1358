<template>
  <div class="h5-register-page">
    <!-- 头部 -->
    <div class="header">
      <div class="header-icon">🏢</div>
      <h1>智慧园区访客登记</h1>
      <p>请填写以下信息完成来访登记</p>
    </div>

    <!-- 步骤条 -->
    <div class="steps">
      <div class="step active"><span class="dot">1</span>填写信息</div>
      <div class="line"></div>
      <div :class="['step', submitted ? 'active' : '']"><span class="dot">2</span>提交成功</div>
    </div>

    <!-- 表单 -->
    <div class="form-card" v-if="!submitted">
      <div class="section-title">访客信息</div>
      <div class="form-item">
        <label>姓名 <span class="required">*</span></label>
        <input v-model="form.visitorName" placeholder="请输入您的姓名" maxlength="30" />
      </div>
      <div class="form-item">
        <label>手机号码 <span class="required">*</span></label>
        <input v-model="form.visitorPhone" type="tel" placeholder="请输入手机号码" maxlength="11" />
      </div>
      <div class="form-item">
        <label>身份证号</label>
        <input v-model="form.visitorIdCard" placeholder="请输入身份证号（选填）" maxlength="18" />
      </div>
      <div class="form-item">
        <label>工作单位</label>
        <input v-model="form.visitorCompany" placeholder="请输入您的工作单位（选填）" maxlength="100" />
      </div>

      <div class="section-title">车辆与物资</div>
      <div class="radio-row">
        <label style="flex:1">是否开车</label>
        <span class="radio-group">
          <label class="radio-label"><input type="radio" v-model="form.hasCar" value="0" @change="form.carPlate=''" /> 否</label>
          <label class="radio-label"><input type="radio" v-model="form.hasCar" value="1" /> 是</label>
        </span>
      </div>
      <div class="form-item" v-if="form.hasCar === '1'">
        <label>车牌号</label>
        <input v-model="form.carPlate" placeholder="请输入车牌号" maxlength="20" />
      </div>
      <div class="radio-row">
        <label style="flex:1">是否携带物资</label>
        <span class="radio-group">
          <label class="radio-label"><input type="radio" v-model="form.hasGoods" value="0" @change="form.goodsDesc=''" /> 否</label>
          <label class="radio-label"><input type="radio" v-model="form.hasGoods" value="1" /> 是</label>
        </span>
      </div>
      <div class="form-item" v-if="form.hasGoods === '1'">
        <label>物资说明</label>
        <input v-model="form.goodsDesc" placeholder="请输入携带物资说明" maxlength="200" />
      </div>

      <div class="section-title">来访信息</div>
      <div class="form-item">
        <label>来访事由 <span class="required">*</span></label>
        <input v-model="form.visitReason" placeholder="请输入来访事由" maxlength="200" />
      </div>
      <div class="form-item">
        <label>被访人 <span class="required">*</span></label>
        <input v-model="form.hostName" placeholder="请输入被访人姓名" maxlength="30" list="host-list" @focus="loadHosts" />
        <datalist id="host-list">
          <option v-for="h in hostList" :key="h.hostName" :value="h.hostName">
            {{ h.hostName }} - {{ h.hostDept }}
          </option>
        </datalist>
      </div>
      <div class="form-item">
        <label>被访部门 <span class="required">*</span></label>
        <input v-model="form.hostDept" placeholder="请输入被访部门" maxlength="50" />
      </div>
      <div class="form-item">
        <label>被访人电话</label>
        <input v-model="form.hostPhone" type="tel" placeholder="请输入被访人电话（选填）" maxlength="11" />
      </div>

      <div class="agreement">
        <label>
          <input type="checkbox" v-model="agreed" />
          我承诺以上信息真实有效，遵守园区管理规定
        </label>
      </div>

      <button class="submit-btn" @click="handleSubmit" :disabled="submitting || !agreed">
        {{ submitting ? '提交中...' : '提交登记' }}
      </button>
    </div>

    <!-- 成功页 -->
    <div class="success-card" v-else>
      <div class="success-icon">✅</div>
      <h2>登记成功</h2>
      <div class="pass-info">
        <div class="pass-label">您的通行码</div>
        <div class="pass-code">{{ passCode }}</div>
        <div class="pass-tip">请向门卫出示此通行码</div>
      </div>
      <div class="detail-list">
        <div class="detail-item"><span>姓名</span><strong>{{ form.visitorName }}</strong></div>
        <div class="detail-item"><span>手机</span><strong>{{ form.visitorPhone }}</strong></div>
        <div class="detail-item"><span>被访人</span><strong>{{ form.hostName }}</strong></div>
        <div class="detail-item"><span>登记时间</span><strong>{{ entryTime }}</strong></div>
      </div>
      <button class="reset-btn" @click="resetForm">继续登记</button>
    </div>

    <!-- 底部 -->
    <div class="footer">
      <p>智慧园区管理系统</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

const api = axios.create({
  baseURL: process.env.NODE_ENV === 'production' ? '/prod-api' : '/dev-api',
  timeout: 15000
})

export default {
  name: 'H5Register',
  data() {
    return {
      submitted: false,
      submitting: false,
      agreed: false,
      passCode: '',
      entryTime: '',
      hostList: [],
      form: {
        visitorName: '',
        visitorPhone: '',
        visitorIdCard: '',
        visitorCompany: '',
        visitReason: '',
        hasCar: '0',
        carPlate: '',
        hasGoods: '0',
        goodsDesc: '',
        hostName: '',
        hostDept: '',
        hostPhone: ''
      }
    }
  },
  methods: {
    loadHosts() {
      if (this.hostList.length > 0) return
      api.get('/visitor/h5/hosts').then(res => {
        if (res.data && res.data.data) {
          this.hostList = res.data.data
        }
      }).catch(() => {})
    },
    validate() {
      if (!this.form.visitorName.trim()) { this.toast('请输入姓名'); return false }
      if (!/^1[3-9]\d{9}$/.test(this.form.visitorPhone)) { this.toast('请输入正确的手机号码'); return false }
      if (this.form.visitorIdCard && !/(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(this.form.visitorIdCard)) { this.toast('请输入正确的身份证号'); return false }
      if (!this.form.visitReason.trim()) { this.toast('请输入来访事由'); return false }
      if (!this.form.hostName.trim()) { this.toast('请输入被访人姓名'); return false }
      if (!this.form.hostDept.trim()) { this.toast('请输入被访部门'); return false }
      if (this.form.hostPhone && !/^1[3-9]\d{9}$/.test(this.form.hostPhone)) { this.toast('请输入正确的被访人电话'); return false }
      return true
    },
    handleSubmit() {
      if (!this.validate()) return
      this.submitting = true
      api.post('/visitor/h5/submit', this.form).then(res => {
        const data = res.data
        if (data.code === 200) {
          this.passCode = data.passCode || ''
          this.entryTime = new Date().toLocaleString('zh-CN')
          this.submitted = true
        } else {
          this.toast(data.msg || '提交失败')
        }
      }).catch(() => {
        this.toast('网络错误，请稍后重试')
      }).finally(() => {
        this.submitting = false
      })
    },
    resetForm() {
      this.submitted = false
      this.form = {
        visitorName: '',
        visitorPhone: '',
        visitorIdCard: '',
        visitorCompany: '',
        visitReason: '',
        hasCar: '0',
        carPlate: '',
        hasGoods: '0',
        goodsDesc: '',
        hostName: '',
        hostDept: '',
        hostPhone: ''
      }
      this.agreed = false
    },
    toast(msg) {
      const el = document.createElement('div')
      el.className = 'toast-msg'
      el.textContent = msg
      document.body.appendChild(el)
      setTimeout(() => { el.classList.add('show') }, 10)
      setTimeout(() => { el.classList.remove('show'); setTimeout(() => document.body.removeChild(el), 300) }, 2500)
    }
  }
}
</script>

<style scoped>
* { box-sizing: border-box; margin: 0; padding: 0; }
.h5-register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  padding-bottom: 40px;
}
.header { text-align: center; padding: 30px 20px 15px; color: #fff; }
.header-icon { font-size: 48px; margin-bottom: 8px; }
.header h1 { font-size: 22px; font-weight: 600; }
.header p { font-size: 13px; opacity: 0.85; margin-top: 4px; }
.steps { display: flex; align-items: center; justify-content: center; padding: 0 40px 15px; }
.step { display: flex; align-items: center; gap: 6px; font-size: 13px; color: rgba(255,255,255,0.6); }
.step.active { color: #fff; font-weight: 600; }
.step .dot { width: 22px; height: 22px; border-radius: 50%; background: rgba(255,255,255,0.3); display: flex; align-items: center; justify-content: center; font-size: 12px; }
.step.active .dot { background: #fff; color: #1890ff; }
.line { flex: 1; height: 2px; background: rgba(255,255,255,0.3); margin: 0 12px; max-width: 60px; }
.form-card, .success-card { background: #fff; margin: 0 16px; border-radius: 12px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
.section-title { font-size: 15px; font-weight: 600; color: #333; margin: 0 0 12px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.section-title + .section-title { margin-top: 20px; }
.form-item { margin-bottom: 14px; }
.form-item label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.form-item .required { color: #ff4d4f; }
.form-item input {
  width: 100%; height: 42px; border: 1px solid #e0e0e0; border-radius: 8px; padding: 0 12px;
  font-size: 15px; color: #333; outline: none; transition: border-color .2s;
}
.form-item input:focus { border-color: #1890ff; }
.agreement { font-size: 12px; color: #999; margin: 16px 0; display: flex; align-items: center; }
.agreement input[type=checkbox] { margin-right: 8px; width: 16px; height: 16px; }
.submit-btn, .reset-btn {
  width: 100%; height: 46px; border: none; border-radius: 23px; font-size: 17px; font-weight: 600;
  cursor: pointer; transition: all .2s;
}
.submit-btn { background: linear-gradient(135deg, #1890ff, #36cfc9); color: #fff; margin-top: 8px; }
.submit-btn:disabled { background: #ccc; cursor: not-allowed; }
.reset-btn { background: #f0f0f0; color: #333; margin-top: 16px; }
.success-card { text-align: center; }
.success-icon { font-size: 56px; margin: 10px 0; }
.success-card h2 { font-size: 20px; color: #333; margin: 8px 0; }
.pass-info { background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 10px; padding: 16px; margin: 16px 0; }
.pass-label { font-size: 12px; color: #52c41a; }
.pass-code { font-size: 32px; font-weight: 700; color: #333; letter-spacing: 6px; margin: 8px 0; }
.pass-tip { font-size: 12px; color: #999; }
.detail-list { text-align: left; padding: 0 8px; }
.detail-item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f5f5f5; font-size: 14px; }
.detail-item span { color: #999; }
.detail-item strong { color: #333; }
.radio-row { display: flex; align-items: center; margin-bottom: 10px; font-size: 13px; color: #666; }
.radio-group { display: flex; gap: 16px; }
.radio-label { display: flex; align-items: center; gap: 4px; cursor: pointer; color: #333; }
.radio-label input[type=radio] { width: 16px; height: 16px; accent-color: #1890ff; }
.footer { text-align: center; padding: 20px; color: rgba(255,255,255,0.6); font-size: 12px; }
</style>
<style>
.toast-msg {
  position: fixed; top: 50%; left: 50%; transform: translate(-50%,-50%) scale(0.8);
  background: rgba(0,0,0,0.78); color: #fff; padding: 12px 24px; border-radius: 8px;
  font-size: 15px; z-index: 9999; opacity: 0; transition: all .25s; pointer-events: none;
}
.toast-msg.show { opacity: 1; transform: translate(-50%,-50%) scale(1); }
</style>
