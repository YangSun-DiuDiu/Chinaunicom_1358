<template>
  <div class="h5-page">
    <div class="h5-nav"><el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button></div>
    <h3>📅 会议室</h3>
    <div v-loading="loading">
      <div v-for="room in rooms" :key="room.roomId" class="room-card" @click="$router.push('/h5/meeting/book?roomId='+room.roomId)">
        <div class="room-header">
          <span class="room-name">{{ room.roomName }}</span>
          <el-tag :type="room.status==='0'?'success':'danger'" size="mini">{{ room.status==='0'?'可用':'停用' }}</el-tag>
        </div>
        <div class="room-info">容纳{{ room.capacity || '-' }}人 · {{ room.location || '' }}</div>
        <div v-if="room.todayBookings && room.todayBookings.length" class="room-schedule">
          <div v-for="b in room.todayBookings" :key="b.bookingId" class="schedule-item">
            {{ b.startTime?b.startTime.substring(11,16):'' }} - {{ b.endTime?b.endTime.substring(11,16):'' }}  {{ b.title }}
          </div>
        </div>
        <div v-else class="room-empty">今日暂无预约</div>
      </div>
    </div>
  </div>
</template>

<script>
import { getMeetingRooms } from '@/api/h5'

export default {
  data() { return { rooms: [], loading: false } },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      getMeetingRooms().then(res => {
        this.rooms = res.data || res || []
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.room-card { background:rgba(13,33,55,0.9); border:1px solid rgba(0,180,255,0.12); border-radius:10px;
  padding:16px; margin-bottom:12px; cursor:pointer; }
.room-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }
.room-name { font-size:15px; color:#fff; font-weight:600; }
.room-info { font-size:12px; color:#8899aa; margin-bottom:8px; }
.room-schedule { border-top:1px solid rgba(0,180,255,0.06); padding-top:8px; }
.schedule-item { font-size:12px; color:#bcc9d4; padding:4px 0; }
.room-empty { font-size:12px; color:#556677; padding-top:8px; border-top:1px solid rgba(0,180,255,0.06); }
</style>
