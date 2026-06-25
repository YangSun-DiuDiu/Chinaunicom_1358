<template>
  <div class="app-container">
    <!-- 统计卡片 - 均分宽度 -->
    <el-row :gutter="15" class="mb8">
      <el-col :span="4.8" v-for="(card, i) in statCards" :key="i">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{color: card.color}">{{ stats[card.key] || 0 }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-form :model="query" ref="queryForm" size="small" :inline="true" label-width="60px">
      <el-form-item label="用户名" prop="userName">
        <el-input v-model="query.userName" placeholder="请输入" clearable style="width:150px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="日期" prop="attendanceDate">
        <el-date-picker v-model="query.attendanceDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:150px"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd">新增记录</el-button>
      <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
      <el-button icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
    </el-row>

    <!-- 数据表格 - 列宽平均分配 -->
    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column label="用户名" prop="user_name" align="center" show-overflow-tooltip/>
      <el-table-column label="部门" prop="dept_name" align="center" show-overflow-tooltip/>
      <el-table-column label="考勤日期" prop="attendance_date" align="center"/>
      <el-table-column label="签到时间" align="center">
        <template slot-scope="{row}">
          <span v-if="row.check_in_time">{{ parseTime(row.check_in_time) }}</span>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="签退时间" align="center">
        <template slot-scope="{row}">
          <span v-if="row.check_out_time">{{ parseTime(row.check_out_time) }}</span>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template slot-scope="{row}">
          <el-tag :type="tagType(row.status)" size="small" effect="dark">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" prop="source" align="center"/>
      <el-table-column label="备注" prop="remark" align="center" show-overflow-tooltip/>
      <el-table-column label="操作" align="center" width="100">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="openEdit(row)"/>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c" @click="handleDel(row)"/>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>

    <!-- 新增/修改弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px" size="small">
        <el-form-item label="用户名" prop="user_name">
          <el-input v-model="form.user_name" placeholder="请输入用户名" maxlength="100"/>
        </el-form-item>
        <el-form-item label="部门" prop="dept_name">
          <el-input v-model="form.dept_name" placeholder="请输入部门名称" maxlength="100"/>
        </el-form-item>
        <el-form-item label="考勤日期" prop="attendance_date">
          <el-date-picker v-model="form.attendance_date" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%"/>
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="签到时间">
              <el-time-picker v-model="form.check_in_time" value-format="yyyy-MM-dd HH:mm:ss" placeholder="签到" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="签退时间">
              <el-time-picker v-model="form.check_out_time" value-format="yyyy-MM-dd HH:mm:ss" placeholder="签退" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width:100%">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-select v-model="form.source" style="width:100%">
            <el-option label="手动录入" value="MANUAL"/>
            <el-option label="钉钉同步" value="DINGTALK"/>
            <el-option label="微信同步" value="WECHAT"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="选填" maxlength="200" :rows="2"/>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit" :loading="submitting">确 定</el-button>
        <el-button @click="open=false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'

export default {
  name: 'AttendanceData',
  data() {
    return {
      loading: false, total: 0, list: [], stats: {}, open: false, submitting: false, title: '',
      statCards: [
        { key: 'total', label: '总人数', color: '#1890ff' },
        { key: 'normal', label: '正常', color: '#52c41a' },
        { key: 'late', label: '迟到', color: '#faad14' },
        { key: 'absent', label: '缺勤', color: '#f5222d' },
        { key: 'overtime', label: '加班', color: '#722ed1' }
      ],
      statusOptions: [
        { label: '正常', value: 'NORMAL' }, { label: '迟到', value: 'LATE' },
        { label: '早退', value: 'EARLY' }, { label: '缺勤', value: 'ABSENT' },
        { label: '加班', value: 'OVERTIME' }
      ],
      query: { pageNum: 1, pageSize: 10, userName: undefined, attendanceDate: undefined, status: undefined },
      form: { record_id: undefined, user_name: '', dept_name: '', attendance_date: '', check_in_time: '', check_out_time: '', status: 'NORMAL', source: 'MANUAL', remark: '' },
      rules: {
        user_name: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
        dept_name: [{ required: true, message: '部门不能为空', trigger: 'blur' }],
        attendance_date: [{ required: true, message: '日期不能为空', trigger: 'change' }],
        status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
        source: [{ required: true, message: '来源不能为空', trigger: 'change' }]
      }
    }
  },
  created() { this.getList(); this.loadStats() },
  methods: {
    parseTime,
    getList() {
      this.loading = true
      const params = { pageNum: this.query.pageNum, pageSize: this.query.pageSize }
      if (this.query.userName) params.userName = this.query.userName
      if (this.query.attendanceDate) params.attendanceDate = this.query.attendanceDate
      if (this.query.status) params.status = this.query.status
      request({ url: '/attendance/list', method: 'get', params }).then(r => {
        this.list = r.rows || []; this.total = r.total || 0; this.loading = false
      }).catch(() => { this.loading = false })
    },
    reset() { this.query = { pageNum: 1, pageSize: 10, userName: undefined, attendanceDate: undefined, status: undefined }; this.getList(); this.loadStats() },
    loadStats() { request({ url: '/attendance/stats', method: 'get' }).then(r => { this.stats = r.data || {} }) },
    tagType(s) { const m = { NORMAL: 'success', LATE: 'warning', EARLY: 'warning', ABSENT: 'danger', OVERTIME: 'info' }; return m[s] || '' },
    statusLabel(s) { const m = { NORMAL: '正常', LATE: '迟到', EARLY: '早退', ABSENT: '缺勤', OVERTIME: '加班' }; return m[s] || s },
    openAdd() {
      this.title = '新增考勤记录'
      this.form = { record_id: undefined, user_name: '', dept_name: '', attendance_date: '', check_in_time: '', check_out_time: '', status: 'NORMAL', source: 'MANUAL', remark: '' }
      this.open = true
      this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() })
    },
    openEdit(row) {
      this.title = '修改考勤记录'
      this.form = { ...row }
      this.open = true
      this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() })
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        this.submitting = true
        const data = { user_name: this.form.user_name, dept_name: this.form.dept_name, attendance_date: this.form.attendance_date, check_in_time: this.form.check_in_time || null, check_out_time: this.form.check_out_time || null, status: this.form.status, source: this.form.source, remark: this.form.remark }
        if (this.form.record_id) data.record_id = this.form.record_id
        request({ url: '/attendance/add', method: 'post', data }).then(() => {
          this.$message.success(this.form.record_id ? '修改成功' : '新增成功')
          this.open = false; this.getList(); this.loadStats()
        }).finally(() => { this.submitting = false })
      })
    },
    handleDel(row) {
      this.$confirm('确认删除【' + row.user_name + '】的考勤记录吗？', '提示', { type: 'warning' }).then(() => {
        request({ url: '/attendance/delete/' + row.record_id, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList(); this.loadStats() })
      }).catch(() => {})
    },
    handleExport() {
      this.$message.info('导出功能开发中')
    }
  }
}
</script>
<style scoped>
.stat-card { text-align: center; cursor: default; }
.stat-value { font-size: 32px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
</style>
