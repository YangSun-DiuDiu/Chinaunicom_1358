<template>
  <div class="h5-page">
    <div class="h5-nav"><el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button></div>
    <h3>🖥 设备状态</h3>
    <div v-loading="loading">
      <div v-for="d in dedupedDevices" :key="d.deviceId" class="device-card" :class="d.status==='ONLINE'?'online':'offline'">
        <div class="device-name">{{ d.deviceName }}</div>
        <el-tag :type="d.status==='ONLINE'?'success':'danger'" size="small">{{ d.status==='ONLINE'?'在线':'离线' }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script>
import { getDeviceList } from '@/api/h5'

export default {
  data() { return { devices: [], loading: false } },
  computed: {
    dedupedDevices() {
      const seen = {}
      return this.devices.filter(d => {
        if (seen[d.deviceId]) return false
        seen[d.deviceId] = true
        return true
      })
    }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      getDeviceList().then(res => {
        this.devices = res.data || res || []
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.device-card { background:rgba(13,33,55,0.9); border-radius:10px; padding:16px 20px;
  margin-bottom:10px; display:flex; justify-content:space-between; align-items:center; }
.device-card.online { border-left:4px solid #00e676; }
.device-card.offline { border-left:4px solid #ff5252; }
.device-name { font-size:15px; color:#fff; font-weight:600; }
</style>
