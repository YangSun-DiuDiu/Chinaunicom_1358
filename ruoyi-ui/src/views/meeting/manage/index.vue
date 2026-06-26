<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" label-width="80px">
      <el-form-item label="会议室">
        <el-select v-model="query.roomId" placeholder="选择会议室" clearable style="width:180px">
          <el-option v-for="r in roomOptions" :key="r.roomId" :label="r.roomName" :value="r.roomId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="queryDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:160px"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="会议标题" prop="title" min-width="160" :show-overflow-tooltip="true"/>
      <el-table-column label="会议室" prop="roomName" min-width="120"/>
      <el-table-column label="主持人" prop="hostName" min-width="100"/>
      <el-table-column label="开始时间" prop="startTime" min-width="150" align="center"/>
      <el-table-column label="结束时间" prop="endTime" min-width="150" align="center"/>
      <el-table-column label="参会人员" prop="attendees" min-width="180" :show-overflow-tooltip="true"/>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import request from '@/utils/request'
const BASE = '/meeting/booking'

function todayStr() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return y + '-' + m + '-' + day
}

export default {
  data() {
    return {
      loading: false, total: 0, list: [], roomOptions: [],
      queryDate: todayStr(),
      query: { pageNum: 1, pageSize: 10, roomId: undefined, status: 'APPROVED', startTime: todayStr(), endTime: todayStr() }
    }
  },
  created() {
    this.getList(); this.getRoomOptions()
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.query }
      if (this.queryDate) {
        params.startTime = this.queryDate; params.endTime = this.queryDate
      }
      request({ url: BASE + '/list', method: 'get', params }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      })
    },
    getRoomOptions() {
      request({ url: '/meeting/room/list', method: 'get', params: { pageNum: 1, pageSize: 999 } }).then(r => {
        this.roomOptions = r.rows || []
      })
    },
    reset() {
      this.queryDate = todayStr()
      this.query = { pageNum: 1, pageSize: 10, roomId: undefined, status: 'APPROVED', startTime: todayStr(), endTime: todayStr() }
      this.getList()
    }
  }
}
</script>
