<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="会议室">
        <el-select v-model="query.roomId" placeholder="选择会议室" clearable style="width:180px">
          <el-option v-for="r in roomOptions" :key="r.roomId" :label="r.roomName" :value="r.roomId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="dateRange" style="width:240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="会议标题" prop="title" min-width="160" :show-overflow-tooltip="true"/>
      <el-table-column label="会议室" prop="roomName" min-width="120"/>
      <el-table-column label="主持人" prop="hostName" min-width="100"/>
      <el-table-column label="开始时间" prop="startTime" min-width="150" align="center"/>
      <el-table-column label="结束时间" prop="endTime" min-width="150" align="center"/>
      <el-table-column label="参会人员" prop="attendees" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="状态" min-width="90" align="center">
        <template slot-scope="{row}">
          <el-tag type="warning" size="small">待审批</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="180" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-check" style="color:#67c23a" @click="handleApprove(row)" v-hasPermi="['meeting:booking:approve']">通过</el-button>
          <el-button size="mini" type="text" icon="el-icon-close" style="color:#f56c6c" @click="handleReject(row)" v-hasPermi="['meeting:booking:reject']">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import request from '@/utils/request'
const BASE = '/meeting/booking'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], roomOptions: [], dateRange: [],
      query: { pageNum: 1, pageSize: 10, roomId: undefined, status: 'PENDING', startTime: undefined, endTime: undefined }
    }
  },
  created() {
    this.getList(); this.getRoomOptions()
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.query }
      if (this.dateRange && this.dateRange.length === 2) {
        params.startTime = this.dateRange[0]; params.endTime = this.dateRange[1]
      } else {
        params.startTime = undefined; params.endTime = undefined
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
      this.dateRange = []
      this.query = { pageNum: 1, pageSize: 10, roomId: undefined, status: 'PENDING', startTime: undefined, endTime: undefined }
      this.getList()
    },
    handleApprove(row) {
      this.$confirm('确认通过「' + row.title + '」的预约申请?', '审批通过', { type: 'success' }).then(() =>
        request({ url: BASE + '/approve/' + row.bookingId, method: 'put' }).then(() => { this.$message.success('已通过'); this.getList() })
      ).catch(() => {})
    },
    handleReject(row) {
      this.$confirm('确认拒绝「' + row.title + '」的预约申请?', '审批拒绝', { type: 'warning' }).then(() =>
        request({ url: BASE + '/reject/' + row.bookingId, method: 'put' }).then(() => { this.$message.success('已拒绝'); this.getList() })
      ).catch(() => {})
    }
  }
}
</script>
