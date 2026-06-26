<template>
  <div class="login-wrapper">
    <!-- 左侧：科技渐变背景 + CSS 粒子动画 -->
    <div class="login-left">
      <div class="particles-container">
        <div class="particle" v-for="n in 30" :key="n"
          :style="{
            left: randomPos(n, 0) + '%',
            top: randomPos(n, 1) + '%',
            width: randomSize(n, 0) + 'px',
            height: randomSize(n, 0) + 'px',
            animationDelay: randomDelay(n) + 's',
            animationDuration: randomDuration(n) + 's'
          }"
        ></div>
      </div>
      <div class="left-content">
        <h1 class="system-title">智慧园区管理系统</h1>
        <p class="system-subtitle">Smart Park Management System</p>
        <div class="tech-line"></div>
        <p class="tech-desc">智能化 · 数字化 · 可视化的园区综合管理平台</p>
      </div>
    </div>
    <!-- 右侧：白色卡片登录表单 -->
    <div class="login-right">
      <div class="login-card">
        <h3 class="login-card-title">欢迎登录</h3>
        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              auto-complete="off"
              placeholder="账号"
            >
              <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              auto-complete="off"
              placeholder="密码"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <el-input
              v-model="loginForm.code"
              auto-complete="off"
              placeholder="验证码"
              style="width: 63%"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img"/>
            </div>
          </el-form-item>
          <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
          <el-form-item style="width:100%;">
            <el-button
              :loading="loading"
              size="medium"
              type="primary"
              style="width:100%;"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
            </el-button>
            <div style="float: right;" v-if="register">
              <router-link class="link-type" :to="'/register'">立即注册</router-link>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <!--  底部  -->
      <div class="login-footer">
        <span>Copyright &copy; 2026 智慧园区管理系统 All Rights Reserved.</span>
      </div>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import defaultSettings from '@/settings'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    },
    /** 粒子随机位置 */
    randomPos(index, axis) {
      const seed = index * 7 + axis * 13
      return (seed * 17 + 3) % 100
    },
    /** 粒子随机大小 */
    randomSize(index, axis) {
      return ((index * 11 + axis * 5) % 4) + 2
    },
    /** 粒子随机延迟 */
    randomDelay(index) {
      return ((index * 23) % 10)
    },
    /** 粒子随机时长 */
    randomDuration(index) {
      return ((index * 13) % 8) + 4
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login-wrapper {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

/* ========== 左侧：科技渐变背景 + CSS 粒子 ========== */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #0A1628 0%, #0D2B4E 50%, #00D4FF 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.particles-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.particle {
  position: absolute;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 0.6;
  }
  90% {
    opacity: 0.6;
  }
  100% {
    transform: translateY(-100vh) scale(0.3);
    opacity: 0;
  }
}

.left-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.system-title {
  font-size: 42px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 16px 0;
  letter-spacing: 4px;
  text-shadow: 0 2px 12px rgba(0, 212, 255, 0.5);
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}

.system-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 24px 0;
  letter-spacing: 2px;
  font-weight: 300;
}

.tech-line {
  width: 80px;
  height: 3px;
  background: linear-gradient(90deg, #00D4FF, #00A8CC);
  margin: 0 auto 24px;
  border-radius: 2px;
}

.tech-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
  letter-spacing: 2px;
}

/* ========== 右侧：白色卡片登录表单 ========== */
.login-right {
  width: 480px;
  min-width: 480px;
  background: #F0F2F5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.login-card {
  width: 400px;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 40px 36px 20px 36px;
  margin-bottom: 20px;
}

.login-card-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  text-align: center;
  margin: 0 0 32px 0;
  letter-spacing: 2px;
}

.login-form {
  .el-input {
    height: 42px;
    input {
      height: 42px;
      border-radius: 6px;
    }
  }
  .input-icon {
    height: 42px;
    width: 14px;
    margin-left: 2px;
  }
}

.login-code {
  width: 33%;
  height: 42px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.login-code-img {
  height: 42px;
  border-radius: 4px;
}

.login-footer {
  text-align: center;
  color: #999;
  font-size: 12px;
  letter-spacing: 1px;
}

.link-type {
  color: #409EFF;
}

/* ========== 响应式：小屏幕时只显示右侧表单 ========== */
@media (max-width: 768px) {
  .login-left {
    display: none;
  }
  .login-right {
    width: 100%;
    min-width: 100%;
    padding: 20px;
  }
  .login-card {
    width: 100%;
    padding: 30px 24px 20px 24px;
  }
}
</style>
