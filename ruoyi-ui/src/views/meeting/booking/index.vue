<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="会议室">
        <el-select v-model="query.roomId" placeholder="选择会议室" clearable style="width:180px">
          <el-option v-for="r in roomOptions" :key="r.roomId" :label="r.roomName" :value="r.roomId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="状态" clearable style="width:130px">
          <el-option label="待审批" value="PENDING"/>
          <el-option label="已通过" value="APPROVED"/>
          <el-option label="已拒绝" value="REJECTED"/>
          <el-option label="已取消" value="CANCELLED"/>
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
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['meeting:booking:add']">新增预约</el-button>
      </el-col>
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
          <el-tag v-if="row.status === 'PENDING'" type="warning" size="small">待审批</el-tag>
          <el-tag v-else-if="row.status === 'APPROVED'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="row.status === 'REJECTED'" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else-if="row.status === 'CANCELLED'" type="info" size="small">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button v-if="row.status === 'PENDING'" size="mini" type="text" icon="el-icon-check" style="color:#67c23a" @click="handleApprove(row)" v-hasPermi="['meeting:booking:approve']">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" size="mini" type="text" icon="el-icon-close" style="color:#f56c6c" @click="handleReject(row)" v-hasPermi="['meeting:booking:reject']">拒绝</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['meeting:booking:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['meeting:booking:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="会议室" prop="roomId">
          <el-select v-model="form.roomId" placeholder="选择会议室" style="width:100%">
            <el-option v-for="r in roomOptions" :key="r.roomId" :label="r.roomName" :value="r.roomId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="会议标题" prop="title">
          <el-input v-model="form.title" maxlength="200" placeholder="请输入会议标题"/>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择开始时间" style="width:100%"/>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择结束时间" style="width:100%"/>
        </el-form-item>
        <el-form-item label="主持人" prop="hostName">
          <el-input v-model="form.hostName" maxlength="50" placeholder="请输入主持人姓名"/>
        </el-form-item>
        <el-form-item label="主持人电话" prop="hostPhone">
          <el-input v-model="form.hostPhone" maxlength="20" placeholder="请输入主持人电话"/>
        </el-form-item>
        <el-form-item label="参会人员" prop="attendees">
          <el-input v-model="form.attendees" maxlength="500" placeholder="请输入参会人员" type="textarea" :rows="3"/>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit">确定</el-button>
        <el-button @click="open=false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
const BASE = '/meeting/booking'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      roomOptions: [], dateRange: [],
      query: { pageNum: 1, pageSize: 10, roomId: undefined, status: undefined, startTime: undefined, endTime: undefined },
      form: { bookingId: undefined, roomId: undefined, title: '', startTime: '', endTime: '', hostName: '', hostPhone: '', attendees: '' },
      rules: {
        roomId: [{ required: true, message: '请选择会议室', trigger: 'change' }],
        title: [{ required: true, message: '会议标题不能为空', trigger: 'blur' }],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
        hostName: [{ required: true, message: '主持人姓名不能为空', trigger: 'blur' }]
      }
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
      this.query = { pageNum: 1, pageSize: 10, roomId: undefined, status: undefined, startTime: undefined, endTime: undefined }
      this.getList()
    },
    handleAdd() {
      this.form = { bookingId: undefined, roomId: undefined, title: '', startTime: '', endTime: '', hostName: '', hostPhone: '', attendees: '' }
      this.title = '新增会议预约'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改会议预约'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除会议预约「' + row.title + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.bookingId, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
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
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.bookingId ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
