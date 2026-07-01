<template>
  <div class="h5-home">
    <div class="h5-header">
      <div class="h5-user">👋 {{ username }}</div>
      <el-button type="text" size="mini" style="color:#8899aa" @click="logout">退出</el-button>
    </div>
    <div class="h5-cards">
      <div class="h5-card" @click="$router.push('/h5/visitor')">
        <span class="icon">👤</span><span class="label">访客登记</span>
      </div>
      <div class="h5-card" @click="$router.push('/visitor/approval')">
        <span class="icon">✅</span><span class="label">访客审批</span>
      </div>
      <div class="h5-card" @click="$router.push('/h5/meeting')">
        <span class="icon">📅</span><span class="label">会议预约</span>
      </div>
      <div class="h5-card" @click="$router.push('/meeting/booking')">
        <span class="icon">👍</span><span class="label">会议审批</span>
      </div>
      <div class="h5-card" @click="$router.push('/h5/device')">
        <span class="icon">🖥</span><span class="label">设备状态</span>
      </div>
      <div class="h5-card" @click="$router.push('/h5/guard')">
        <span class="icon">🔒</span><span class="label">保安核验</span>
      </div>
      <div class="h5-card" @click="$router.push('/repair-complete')">
        <span class="icon">🔧</span><span class="label">报修确认</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'H5Index',
  data() {
    return { username: '' }
  },
  created() {
    this.username = this.$store.state.user.name || '用户'
  },
  methods: {
    logout() {
      this.$store.dispatch('LogOut').then(() => this.$router.push('/h5/login'))
    }
  }
}
</script>

<style scoped>
.h5-home { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:24px; }
.h5-user { font-size:16px; color:#fff; }
.h5-cards { display:grid; grid-template-columns:1fr 1fr 1fr; gap:10px; }
.h5-card .icon { font-size:28px; display:block; margin-bottom:6px; }
.h5-card { background:rgba(13,33,55,0.9); border:1px solid rgba(0,180,255,0.12); border-radius:10px;
  padding:28px 16px; text-align:center; cursor:pointer; transition:all .3s; }
.h5-card:active { background:rgba(0,180,255,0.12); }
.h5-card .icon { font-size:32px; display:block; margin-bottom:8px; }
.h5-card .label { font-size:13px; color:#8899aa; }
</style>
