<template>
  <div class="h5-login">
    <div class="h5-login-card">
      <h2>🏢 智慧园区</h2>
      <p class="subtitle">园区服务门户</p>
      <el-form ref="form" :model="form" :rules="rules" @submit.native.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" prefix-icon="el-icon-user" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="密码" prefix-icon="el-icon-lock" type="password" size="large" show-password @keyup.enter.native="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" long :loading="loading" @click="handleLogin">登 录</el-button>
      </el-form>
      <div class="service-cards">
        <div class="svc-item">👤 访客登记</div>
        <div class="svc-item">📅 会议预约</div>
        <div class="svc-item">🖥 设备状态</div>
        <div class="svc-item">🔒 保安核验</div>
      </div>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/login'
import { setToken } from '@/utils/auth'

export default {
  name: 'H5Login',
  data() {
    return {
      form: { username: '', password: '' },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.loading = true
        login(this.form.username, this.form.password, 'h5').then(res => {
          setToken(res.token)
          this.$router.push(this.$route.query.redirect || '/h5')
        }).catch(() => {
          this.loading = false
        })
      })
    }
  }
}
</script>

<style scoped>
.h5-login {
  min-height: 100vh; background: linear-gradient(135deg, #0A1530 0%, #1A2A52 60%, #5645D4 100%); display: flex; align-items: center; justify-content: center; padding: 20px;
}
.h5-login-card {
  background: rgba(255,255,255,0.98); border: 1px solid #E5E3DF; border-radius: 12px; padding: 36px 24px;
  width: 100%; max-width: 380px; text-align: center;
  box-shadow: rgba(15,15,15,0.08) 0px 4px 12px 0px;
}
.h5-login-card h2 { color: #37352F; margin: 0 0 4px; font-size: 22px; }
.subtitle { color: #787671; margin: 0 0 24px; font-size: 13px; }
.service-cards { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 24px; justify-content: center; }
.svc-item { background: rgba(86,69,212,0.08); color: #5D5B54; font-size: 12px; padding: 6px 12px; border-radius: 9999px; }
.h5-login >>> .el-input__inner { background: #fff; border-color: #C8C4BE; color: #37352F; }
.h5-login >>> .el-button--primary { background: #5645D4; border-color: #5645D4; }
</style>
