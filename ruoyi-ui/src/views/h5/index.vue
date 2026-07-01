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
      <div class="h5-card" @click="$router.push('/h5/meeting')">
        <span class="icon">📅</span><span class="label">会议预约</span>
      </div>
      <div class="h5-card" @click="$router.push('/h5/device')">
        <span class="icon">🖥</span><span class="label">设备状态</span>
      </div>
      <div class="h5-card" @click="$router.push('/h5/guard')">
        <span class="icon">🔒</span><span class="label">保安核验</span>
      </div>
    </div>
    <div class="h5-data">
      <div class="data-title">今日概览</div>
      <el-row :gutter="8">
        <el-col :span="12" v-for="item in kpiList" :key="item.label">
          <div class="data-card">
            <div class="data-icon">{{ item.icon }}</div>
            <div class="data-info">
              <div class="data-value">{{ item.value }}</div>
              <div class="data-label">{{ item.label }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    <div class="h5-news">
      <div class="data-title">近期动态</div>
      <div v-if="news.recentVisitors && news.recentVisitors.length" class="news-list">
        <div v-for="(v,i) in news.recentVisitors.slice(0,3)" :key="i" class="news-item">
          👤 {{ v.visitorName }} → 被访 {{ v.hostName }}
        </div>
      </div>
      <div v-if="news.recentMeetings && news.recentMeetings.length" class="news-list">
        <div v-for="(m,i) in news.recentMeetings.slice(0,3)" :key="'m'+i" class="news-item">
          📅 {{ m.title }} @ {{ m.roomName }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getToken, removeToken } from '@/utils/auth'
import { getDeviceDashboard, getVisitorDashboard, getMeetingDashboard, getApartmentDashboard } from '@/api/dashboard/dashboard'
import { getNews } from '@/api/h5'

export default {
  name: 'H5Index',
  data() {
    return {
      username: '',
      kpiList: [
        { icon:'🖥', label:'IoT设备', value:'-' },
        { icon:'👤', label:'今日访客', value:'-' },
        { icon:'📅', label:'会议预约', value:'-' },
        { icon:'🏠', label:'公寓房源', value:'-' }
      ],
      news: { recentVisitors:[], recentMeetings:[] }
    }
  },
  created() {
    this.username = this.$store.state.user.name || '用户'
    this.loadData()
  },
  methods: {
    loadData() {
      getDeviceDashboard().then(r => { const d=r.data||r; this.kpiList[0].value=d.totalCount||0 }).catch(()=>{})
      getVisitorDashboard().then(r => { const d=r.data||r; this.kpiList[1].value=d.todayVisitors||0 }).catch(()=>{})
      getMeetingDashboard().then(r => { const d=r.data||r; this.kpiList[2].value=d.totalBookings||0 }).catch(()=>{})
      getApartmentDashboard().then(r => { const d=r.data||r; this.kpiList[3].value=d.totalRooms||0 }).catch(()=>{})
      getNews().then(r => { this.news = r.data || r }).catch(()=>{})
    },
    logout() {
      this.$store.dispatch('LogOut').then(() => this.$router.push('/h5/login'))
    }
  }
}
</script>

<style scoped>
.h5-home { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.h5-user { font-size:16px; color:#fff; }
.h5-cards { display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:24px; }
.h5-card { background:rgba(13,33,55,0.9); border:1px solid rgba(0,180,255,0.12); border-radius:10px;
  padding:28px 16px; text-align:center; cursor:pointer; transition:all .3s; }
.h5-card:active { background:rgba(0,180,255,0.12); }
.h5-card .icon { font-size:32px; display:block; margin-bottom:8px; }
.h5-card .label { font-size:13px; color:#8899aa; }
.data-title { font-size:14px; color:#8899aa; margin-bottom:12px; }
.data-card { display:flex; align-items:center; background:rgba(13,33,55,0.7); border-radius:8px;
  padding:14px; margin-bottom:8px; }
.data-icon { font-size:24px; margin-right:12px; }
.data-value { font-size:20px; font-weight:700; color:#00d4ff; }
.data-label { font-size:11px; color:#8899aa; margin-top:2px; }
.news-item { padding:8px 0; border-bottom:1px solid rgba(0,180,255,0.06); font-size:13px; color:#8899aa; }
</style>
